package com.borrowercopilot.domain.engine

import com.borrowercopilot.domain.model.*
import com.borrowercopilot.domain.rules.FinancialRules
import com.borrowercopilot.domain.rules.FinancialRules.CreditScoreBands
import com.borrowercopilot.domain.rules.FinancialRules.CreditScoreRateAdj
import com.borrowercopilot.domain.rules.FinancialRules.FoirThresholds
import com.borrowercopilot.domain.rules.FinancialRules.HighCostDebtRules
import com.borrowercopilot.domain.rules.FinancialRules.LenderSanctionMultipliers
import com.borrowercopilot.domain.rules.FinancialRules.ProductRouting
import com.borrowercopilot.domain.rules.FinancialRules.SafeAmountDiscount
import com.borrowercopilot.domain.rules.FinancialRules.StressTest
import kotlin.math.min
import kotlin.math.max

/**
 * BorrowerAssessmentEngine
 *
 * Takes a BorrowerProfile + LoanRequest and returns a complete AssessmentResult.
 * All financial logic delegates to FinancialRules — nothing is hard-coded here.
 *
 * Public API:
 *   calculateAssessment(profile, request) -> AssessmentResult
 *   buildNegotiationCard(result, request)  -> NegotiationCard
 */
object BorrowerAssessmentEngine {

    fun calculateAssessment(
        profile: BorrowerProfile,
        request: LoanRequest
    ): AssessmentResult {
        val warnings = mutableListOf<String>()

        // ── Step 1: Affordability ─────────────────────────────────────────
        val affordability = calculateAffordability(profile, request, warnings)

        // ── Step 2: Sanction Estimate ─────────────────────────────────────
        val sanction = calculateSanction(profile, request, affordability, warnings)

        // ── Step 3: Rate Band ─────────────────────────────────────────────
        val (fairRate, rateExplanation) = calculateRateBand(profile, sanction.routeRecommendation, request.loanType)

        // ── Step 4: APR ───────────────────────────────────────────────────
        val aprResult = calculateApr(request, sanction, fairRate, warnings)

        // ── Step 5: EMI ceiling ───────────────────────────────────────────
        val recommendedEmi = affordability.maxSafeMonthlyEmi
        val midRate = (fairRate.lowPercent + fairRate.highPercent) / 2.0
        val tenureOptions = FinancialRules.tenureOptionsFor(request.loanType)
            .filter { it <= 60 || request.loanType == LoanType.HOME_LOAN || request.loanType == LoanType.LOAN_AGAINST_PROPERTY }
        val tenureRows = buildTenureTable(sanction.borrowerSafeRange.midpoint(), midRate, tenureOptions)

        // ── Step 6: Stress test ───────────────────────────────────────────
        val stressResult = calculateStress(profile, request, sanction, fairRate, affordability)

        // ── Step 7: Confidence ────────────────────────────────────────────
        val (confidence, confidenceExplanation) = calculateConfidence(profile)

        // ── Step 8: Verdict ───────────────────────────────────────────────
        val (verdict, verdictExplanation, keyReasons) =
            calculateVerdict(profile, request, affordability, sanction, stressResult, warnings)

        val emiExplanation = buildEmiExplanation(profile, affordability, sanction)

        return AssessmentResult(
            verdict = verdict,
            verdictExplanation = verdictExplanation,
            keyReasons = keyReasons,
            confidence = confidence,
            confidenceExplanation = confidenceExplanation,
            sanctionEstimate = sanction,
            fairRateRange = fairRate,
            aprResult = aprResult,
            rateExplanation = rateExplanation,
            recommendedMaxEmi = recommendedEmi,
            emiExplanation = emiExplanation,
            tenureRows = tenureRows,
            stressResult = stressResult,
            affordabilityResult = affordability,
            missingDataWarnings = warnings
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFORDABILITY
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateAffordability(
        profile: BorrowerProfile,
        request: LoanRequest,
        warnings: MutableList<String>
    ): AffordabilityResult {
        val income = profile.netMonthlyIncome

        // Determine FOIR ceiling based on borrower type
        val foirCeiling = when (profile.incomeType) {
            IncomeType.INFORMAL_GIG -> FoirThresholds.INFORMAL_MAX
            IncomeType.SELF_EMPLOYED -> {
                if ((profile.businessVintageYears ?: 0.0) >= 5) FoirThresholds.MODERATE_MAX
                else FoirThresholds.HIGH_RISK_MAX
            }
            IncomeType.SALARIED -> {
                val stable = profile.employmentStability in listOf(
                    EmploymentStability.THREE_TO_FIVE_YEARS, EmploymentStability.MORE_THAN_5_YEARS
                )
                val hasDependents = profile.numberOfDependents > 0
                when {
                    stable && !hasDependents -> FoirThresholds.CONSERVATIVE_MAX
                    stable                   -> FoirThresholds.MODERATE_MAX
                    else                     -> FoirThresholds.MODERATE_MAX
                }
            }
        }

        // Essential living expenses (use stated or estimate)
        val essentialExpenses = profile.estimatedHouseholdExpenses
            ?: estimateHouseholdExpenses(profile, warnings)

        // Available for EMI = income - existing EMI - essential expenses
        val existingEmi = profile.existingMonthlyEmi
        val netAfterExpenses = income - existingEmi - essentialExpenses
        val foirBasedMax = income * foirCeiling - existingEmi

        // The safe EMI ceiling is the LOWER of the two approaches
        val maxSafeEmi = max(0.0, min(netAfterExpenses * 0.80, foirBasedMax))
        // Keep 20% of net-after-expenses as buffer even if FOIR allows more

        val currentFoir = if (income > 0) existingEmi / income else 0.0
        val midRate = 14.0 // conservative estimate for affordability check
        val proposedEmi = FinancialRules.calculateEmi(
            request.requestedAmountRs, midRate, request.preferredTenureMonths
        )
        val proposedFoir = if (income > 0) (existingEmi + proposedEmi) / income else 1.0
        val availableCapacity = maxSafeEmi
        val safeCapacityUsed = if (availableCapacity > 0) (proposedEmi / availableCapacity).coerceIn(0.0, 2.0) else 1.0

        val explanation = buildAffordabilityExplanation(
            income, existingEmi, essentialExpenses, maxSafeEmi, foirCeiling, profile
        )

        return AffordabilityResult(
            maxSafeMonthlyEmi = maxSafeEmi,
            currentFoir = currentFoir,
            proposedFoir = proposedFoir,
            availableMonthlyCapacity = availableCapacity,
            safeCapacityUsed = safeCapacityUsed,
            explanation = explanation
        )
    }

    private fun estimateHouseholdExpenses(
        profile: BorrowerProfile,
        warnings: MutableList<String>
    ): Double {
        warnings.add("Household expenses not stated — estimated from dependents and income level.")
        val perPerson = FinancialRules.HouseholdExpenseEstimates.URBAN_PER_PERSON_MIN
        val dependentExtra = FinancialRules.HouseholdExpenseEstimates.DEPENDENT_EXTRA
        val rent = profile.monthlyRent ?: 0.0
        val base = perPerson + (profile.numberOfDependents * dependentExtra)
        return base + rent
    }

    private fun buildAffordabilityExplanation(
        income: Double,
        existingEmi: Double,
        expenses: Double,
        maxEmi: Double,
        foirCeiling: Double,
        profile: BorrowerProfile
    ): String {
        val foirPct = (foirCeiling * 100).toInt()
        val parts = mutableListOf<String>()
        parts.add("Net income: ${formatLakhCrore(income)}/month.")
        if (existingEmi > 0) parts.add("Existing EMIs: ${formatLakhCrore(existingEmi)}/month already committed.")
        parts.add("Estimated living expenses: ${formatLakhCrore(expenses)}/month.")
        parts.add("Safe EMI ceiling: ${formatLakhCrore(maxEmi)}/month (${foirPct}% FOIR threshold).")
        if (profile.numberOfDependents > 0) parts.add("${profile.numberOfDependents} dependent(s) reduce available capacity.")
        return parts.joinToString(" ")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LENDER SANCTION ESTIMATE
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateSanction(
        profile: BorrowerProfile,
        request: LoanRequest,
        affordability: AffordabilityResult,
        warnings: MutableList<String>
    ): SanctionEstimate {
        val income = profile.netMonthlyIncome

        // Determine product route
        val (route, routeExplanation) = determineProductRoute(profile, request, warnings)

        // Lender-likely: income multiplier approach (capped by FOIR)
        val multiplier = when (profile.incomeType) {
            IncomeType.SALARIED -> when (profile.creditScoreStatus) {
                CreditScoreStatus.EXCELLENT -> LenderSanctionMultipliers.SALARIED_EXCELLENT_CREDIT
                CreditScoreStatus.VERY_GOOD -> LenderSanctionMultipliers.SALARIED_GOOD_CREDIT
                CreditScoreStatus.GOOD      -> LenderSanctionMultipliers.SALARIED_GOOD_CREDIT
                CreditScoreStatus.FAIR      -> LenderSanctionMultipliers.SALARIED_FAIR_CREDIT
                CreditScoreStatus.POOR      -> LenderSanctionMultipliers.SALARIED_POOR_CREDIT
                CreditScoreStatus.UNKNOWN   -> LenderSanctionMultipliers.UNKNOWN_CREDIT
            }
            IncomeType.SELF_EMPLOYED -> {
                // For self-employed use lower of cash or ITR-derived income
                val effectiveIncome = if (profile.itrAnnualIncome != null) {
                    min(income, profile.itrAnnualIncome / 12.0)
                } else income
                return calculateSanctionForSelfEmployed(profile, request, affordability, route, routeExplanation, effectiveIncome, warnings)
            }
            IncomeType.INFORMAL_GIG -> LenderSanctionMultipliers.INFORMAL
        }

        // FOIR-based max EMI, then calculate principal
        val lenderFoirEmi = income * FoirThresholds.MODERATE_MAX - profile.existingMonthlyEmi
        val midRate = FinancialRules.BASE_RATE_BANDS[request.loanType]?.let {
            (it.lowPct + it.highPct) / 2.0
        } ?: 14.0
        val foirPrincipal = FinancialRules.maxPrincipalFromEmi(
            max(0.0, lenderFoirEmi), midRate, request.preferredTenureMonths
        )
        val multiplierPrincipal = income * multiplier

        val lenderLikelyMax = min(foirPrincipal, multiplierPrincipal)
            .coerceAtLeast(0.0)

        // Unknown credit score — widen the range
        val rangeFactor = if (profile.creditScoreStatus == CreditScoreStatus.UNKNOWN) {
            warnings.add("Credit score unknown — lender sanction range is wider.")
            0.25
        } else 0.10
        val lenderLow  = lenderLikelyMax * (1 - rangeFactor)
        val lenderHigh = lenderLikelyMax * (1 + rangeFactor * 0.5)

        // Borrower-safe: apply discount
        val discount = when (profile.incomeType) {
            IncomeType.INFORMAL_GIG  -> SafeAmountDiscount.INFORMAL_DISCOUNT_PCT
            IncomeType.SELF_EMPLOYED -> SafeAmountDiscount.HIGH_RISK_DISCOUNT_PCT
            IncomeType.SALARIED      -> SafeAmountDiscount.STANDARD_DISCOUNT_PCT
        }
        val safePrincipal = FinancialRules.maxPrincipalFromEmi(
            affordability.maxSafeMonthlyEmi, midRate, request.preferredTenureMonths
        ).coerceAtMost(lenderLikelyMax * (1 - discount))
        val safeRangeFactor = if (profile.creditScoreStatus == CreditScoreStatus.UNKNOWN) 0.20 else 0.10
        val safeLow  = safePrincipal * (1 - safeRangeFactor)
        val safeHigh = safePrincipal * (1 + safeRangeFactor * 0.5)

        val explanation = buildSanctionExplanation(profile, lenderLikelyMax, safePrincipal, request)

        return SanctionEstimate(
            lenderLikelyRange = MoneyRange(lenderLow.coerceAtLeast(0.0), lenderHigh),
            borrowerSafeRange = MoneyRange(safeLow.coerceAtLeast(0.0), safeHigh),
            explanation = explanation,
            recommendedAmount = safePrincipal.coerceAtLeast(0.0),
            routeRecommendation = route,
            routeExplanation = routeExplanation
        )
    }

    private fun calculateSanctionForSelfEmployed(
        profile: BorrowerProfile,
        request: LoanRequest,
        affordability: AffordabilityResult,
        route: ProductRoute,
        routeExplanation: String?,
        effectiveIncome: Double,
        warnings: MutableList<String>
    ): SanctionEstimate {
        val midRate = FinancialRules.BASE_RATE_BANDS[request.loanType]?.let {
            (it.lowPct + it.highPct) / 2.0
        } ?: 15.0

        // If collateral available and LAP route, use LTV-based sanction
        val collateralSanction = if (profile.collateralValueLakh != null && profile.isCollateralUnencumbered) {
            profile.collateralValueLakh * FinancialRules.CollateralAdj.LAP_MAX_LTV
        } else null

        val incomeSanction = effectiveIncome * LenderSanctionMultipliers.SELF_EMPLOYED
        val foirEmi = effectiveIncome * FoirThresholds.MODERATE_MAX - profile.existingMonthlyEmi
        val foirPrincipal = FinancialRules.maxPrincipalFromEmi(max(0.0, foirEmi), midRate, request.preferredTenureMonths)
        val lenderMax = if (collateralSanction != null) {
            max(min(incomeSanction, foirPrincipal), collateralSanction)
        } else min(incomeSanction, foirPrincipal)

        val widthFactor = if (profile.creditScoreStatus == CreditScoreStatus.UNKNOWN) {
            warnings.add("Credit score unknown — range is wider.")
            0.30
        } else 0.15

        if (profile.itrAnnualIncome != null && profile.itrAnnualIncome / 12.0 < effectiveIncome * FinancialRules.ProductRouting.ITR_CASH_INCOME_RATIO_WARN / FinancialRules.ProductRouting.ITR_CASH_INCOME_RATIO_WARN) {
            warnings.add("ITR income appears significantly lower than stated cash income. Lenders may underwrite on ITR figures.")
        }

        val safePrincipal = FinancialRules.maxPrincipalFromEmi(
            affordability.maxSafeMonthlyEmi, midRate, request.preferredTenureMonths
        ).coerceAtMost(lenderMax * (1 - SafeAmountDiscount.HIGH_RISK_DISCOUNT_PCT))

        val explanation = if (collateralSanction != null) {
            "Collateral of ₹${formatLakhCrore(profile.collateralValueLakh!! * 100_000)} supports secured route. LAP LTV ≤ 60% gives ~₹${formatLakhCrore(collateralSanction * 100_000)} sanction capacity."
        } else {
            buildSanctionExplanation(profile, lenderMax, safePrincipal, request)
        }

        return SanctionEstimate(
            lenderLikelyRange = MoneyRange(lenderMax * (1 - widthFactor), lenderMax * (1 + widthFactor * 0.5)),
            borrowerSafeRange = MoneyRange(safePrincipal * 0.85, safePrincipal * 1.10),
            explanation = explanation,
            recommendedAmount = safePrincipal.coerceAtLeast(0.0),
            routeRecommendation = route,
            routeExplanation = routeExplanation
        )
    }

    private fun determineProductRoute(
        profile: BorrowerProfile,
        request: LoanRequest,
        warnings: MutableList<String>
    ): Pair<ProductRoute, String?> {
        // Explicit loan type overrides routing
        if (request.loanType == LoanType.HOME_LOAN) return Pair(ProductRoute.HOME_LOAN, null)
        if (request.loanType == LoanType.GOLD_LOAN) return Pair(ProductRoute.GOLD_LOAN, null)
        if (request.loanType == LoanType.TWO_WHEELER_LOAN) return Pair(ProductRoute.TWO_WHEELER_LOAN, null)

        // Self-employed with significant collateral → LAP route
        val collateralLakh = profile.collateralValueLakh ?: 0.0
        val requestedLakh  = request.requestedAmountRs / 100_000.0
        if (profile.incomeType == IncomeType.SELF_EMPLOYED &&
            collateralLakh >= requestedLakh * FinancialRules.ProductRouting.LAP_COLLATERAL_MULTIPLE &&
            profile.isCollateralUnencumbered) {
            val explanation = "Unencumbered collateral (₹${collateralLakh}L) is ${String.format("%.1f", collateralLakh/requestedLakh)}× the requested amount. " +
                "A Loan Against Property (LAP) or secured business loan is likely more suitable than an unsecured personal loan — " +
                "it typically offers a lower interest rate and higher sanction. Your lender will verify and value the property."
            return Pair(ProductRoute.SECURED_LAP, explanation)
        }

        // Self-employed, business purpose, reasonable vintage → business loan
        if (profile.incomeType == IncomeType.SELF_EMPLOYED &&
            (profile.businessVintageYears ?: 0.0) >= ProductRouting.BUSINESS_VINTAGE_MIN_YEARS &&
            (request.purpose == LoanPurpose.BUSINESS_EXPANSION || request.purpose == LoanPurpose.WORKING_CAPITAL)) {
            return Pair(ProductRoute.BUSINESS_LOAN, "Business vintage of ${profile.businessVintageYears?.toInt()} years with business purpose supports a formal business loan route.")
        }

        return when (request.loanType) {
            LoanType.BUSINESS_LOAN -> Pair(ProductRoute.BUSINESS_LOAN, null)
            else                   -> Pair(ProductRoute.PERSONAL_LOAN, null)
        }
    }

    private fun buildSanctionExplanation(
        profile: BorrowerProfile,
        lenderMax: Double,
        safeAmount: Double,
        request: LoanRequest
    ): String {
        val parts = mutableListOf<String>()
        parts.add("Lender-likely estimate based on income of ${formatLakhCrore(profile.netMonthlyIncome)}/month.")
        if (profile.existingMonthlyEmi > 0)
            parts.add("Existing EMI of ${formatLakhCrore(profile.existingMonthlyEmi)} reduces available capacity.")
        if (profile.creditScoreStatus == CreditScoreStatus.UNKNOWN)
            parts.add("Unknown credit score widens the estimate range.")
        if (safeAmount < lenderMax * 0.85)
            parts.add("Borrower-safe amount is lower to maintain financial headroom.")
        return parts.joinToString(" ")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RATE BAND
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateRateBand(
        profile: BorrowerProfile,
        route: ProductRoute,
        requestedLoanType: LoanType
    ): Pair<RateRange, String> {
        val effectiveLoanType = when (route) {
            ProductRoute.SECURED_LAP    -> LoanType.LOAN_AGAINST_PROPERTY
            ProductRoute.BUSINESS_LOAN  -> LoanType.BUSINESS_LOAN
            ProductRoute.GOLD_LOAN      -> LoanType.GOLD_LOAN
            ProductRoute.HOME_LOAN      -> LoanType.HOME_LOAN
            ProductRoute.TWO_WHEELER_LOAN -> LoanType.TWO_WHEELER_LOAN
            ProductRoute.PERSONAL_LOAN  -> requestedLoanType
        }

        val base = FinancialRules.BASE_RATE_BANDS[effectiveLoanType]
            ?: FinancialRules.BASE_RATE_BANDS[LoanType.PERSONAL_LOAN]!!

        val reasons = mutableListOf<String>()
        var adj = 0.0

        // Credit score adjustment
        val creditAdj = when (profile.creditScoreStatus) {
            CreditScoreStatus.EXCELLENT -> { reasons.add("Excellent credit score (800+) reduces rate."); CreditScoreRateAdj.EXCELLENT_ADJ }
            CreditScoreStatus.VERY_GOOD -> { reasons.add("Very good credit score (750–799) supports lower rate."); CreditScoreRateAdj.VERY_GOOD_ADJ }
            CreditScoreStatus.GOOD      -> { reasons.add("Good credit score (700–749)."); CreditScoreRateAdj.GOOD_ADJ }
            CreditScoreStatus.FAIR      -> { reasons.add("Fair credit score (600–699) pushes rate higher."); CreditScoreRateAdj.FAIR_ADJ }
            CreditScoreStatus.POOR      -> { reasons.add("Poor credit score (<600) significantly raises rate."); CreditScoreRateAdj.POOR_ADJ }
            CreditScoreStatus.UNKNOWN   -> { reasons.add("Credit score unknown — range is wider."); 0.0 }
        }
        adj += creditAdj

        // Income stability adjustment
        val stabilityAdj = when (profile.incomeType) {
            IncomeType.SALARIED -> {
                val stab = profile.employmentStability
                if (stab == EmploymentStability.MORE_THAN_5_YEARS || stab == EmploymentStability.THREE_TO_FIVE_YEARS) {
                    reasons.add("Stable salaried employment supports lower rate.")
                    FinancialRules.IncomeStabilityRateAdj.SALARIED_STABLE
                } else {
                    reasons.add("Shorter employment tenure adds slight risk premium.")
                    FinancialRules.IncomeStabilityRateAdj.SALARIED_NEW
                }
            }
            IncomeType.SELF_EMPLOYED -> {
                val vintage = profile.businessVintageYears ?: 0.0
                if (vintage >= 5) {
                    reasons.add("${vintage.toInt()}-year business vintage reduces lender risk.")
                    FinancialRules.IncomeStabilityRateAdj.SELF_EMPLOYED_OLD
                } else {
                    reasons.add("Shorter business vintage increases rate premium.")
                    FinancialRules.IncomeStabilityRateAdj.SELF_EMPLOYED_YOUNG
                }
            }
            IncomeType.INFORMAL_GIG -> {
                reasons.add("Informal/gig income carries higher lender risk premium.")
                FinancialRules.IncomeStabilityRateAdj.INFORMAL
            }
        }
        adj += stabilityAdj

        // Collateral benefit
        if (route == ProductRoute.SECURED_LAP && (profile.collateralValueLakh ?: 0.0) > 0) {
            reasons.add("Collateral backing reduces lender risk, improving rate.")
        }

        var finalLow  = base.lowPct  + adj
        var finalHigh = base.highPct + adj

        // Unknown credit score: widen both ends
        if (profile.creditScoreStatus == CreditScoreStatus.UNKNOWN) {
            finalLow  -= CreditScoreRateAdj.UNKNOWN_WIDEN
            finalHigh += CreditScoreRateAdj.UNKNOWN_WIDEN
        }

        finalLow  = finalLow.coerceAtLeast(6.0)
        finalHigh = finalHigh.coerceAtMost(40.0)
        if (finalLow > finalHigh) finalLow = finalHigh - 1.0

        val explanation = reasons.joinToString(" ")
        return Pair(RateRange(finalLow, finalHigh), explanation)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APR
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateApr(
        request: LoanRequest,
        sanction: SanctionEstimate,
        fairRate: RateRange,
        warnings: MutableList<String>
    ): APRResult {
        val effectiveLoanType = when (sanction.routeRecommendation) {
            ProductRoute.SECURED_LAP    -> LoanType.LOAN_AGAINST_PROPERTY
            ProductRoute.BUSINESS_LOAN  -> LoanType.BUSINESS_LOAN
            ProductRoute.GOLD_LOAN      -> LoanType.GOLD_LOAN
            ProductRoute.HOME_LOAN      -> LoanType.HOME_LOAN
            ProductRoute.TWO_WHEELER_LOAN -> LoanType.TWO_WHEELER_LOAN
            ProductRoute.PERSONAL_LOAN  -> request.loanType
        }
        val processingFeePct = FinancialRules.PROCESSING_FEE_BANDS[effectiveLoanType] ?: 0.02
        val principal = sanction.borrowerSafeRange.midpoint()
        val feeRs = principal * processingFeePct
        val tenure = request.preferredTenureMonths

        val aprLow  = FinancialRules.estimateApr(principal, fairRate.lowPercent,  tenure, processingFeePct)
        val aprHigh = FinancialRules.estimateApr(principal, fairRate.highPercent, tenure, processingFeePct)

        val explanation = "Estimated APR includes ${String.format("%.1f", processingFeePct * 100)}% processing fee " +
            "(~${formatLakhCrore(feeRs)}). " +
            "A lender quoting ${String.format("%.1f", fairRate.lowPercent)}% with a high processing fee " +
            "can be more expensive than one quoting ${String.format("%.1f", fairRate.highPercent)}% with a lower fee. " +
            "Always compare total cost, not headline rate. This is an estimate."

        return APRResult(
            nominalRate = (fairRate.lowPercent + fairRate.highPercent) / 2.0,
            processingFeePercent = processingFeePct * 100,
            processingFeeRs = feeRs,
            estimatedAprLow = aprLow,
            estimatedAprHigh = aprHigh,
            explanation = explanation
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMI TABLE
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildTenureTable(
        principal: Double,
        annualRate: Double,
        tenureOptions: List<Int>
    ): List<TenureRow> {
        return tenureOptions.map { months ->
            val emi = FinancialRules.calculateEmi(principal, annualRate, months)
            val total = emi * months
            TenureRow(
                tenureMonths   = months,
                monthlyEmi     = emi,
                totalInterest  = total - principal,
                totalPayment   = total
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STRESS TEST
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateStress(
        profile: BorrowerProfile,
        request: LoanRequest,
        sanction: SanctionEstimate,
        fairRate: RateRange,
        affordability: AffordabilityResult
    ): StressResult {
        val midRate = (fairRate.lowPercent + fairRate.highPercent) / 2.0
        val proposedEmi = FinancialRules.calculateEmi(
            sanction.borrowerSafeRange.midpoint(), midRate, request.preferredTenureMonths
        )
        val totalNormalEmi = profile.existingMonthlyEmi + proposedEmi
        val normalRemaining = profile.netMonthlyIncome - totalNormalEmi -
            (profile.estimatedHouseholdExpenses ?: affordability.availableMonthlyCapacity * 0.3)

        val (scenario, stressIncome) = when (profile.incomeType) {
            IncomeType.SALARIED     -> Pair("Income falls by 20%", profile.netMonthlyIncome * (1 - StressTest.SALARIED_INCOME_DROP_PCT))
            IncomeType.SELF_EMPLOYED -> Pair("Monthly income falls to lower end of stated range",
                profile.netMonthlyIncome * StressTest.SELF_EMPLOYED_INCOME_FLOOR)
            IncomeType.INFORMAL_GIG -> Pair("Income falls 20% and existing obligations remain unchanged",
                profile.netMonthlyIncome * (1 - StressTest.INFORMAL_INCOME_DROP_PCT))
        }

        // For rate-sensitive loans, also consider rate rise
        val stressRate = midRate + StressTest.RATE_RISE_POINTS
        val stressEmi = if (request.loanType == LoanType.HOME_LOAN || request.loanType == LoanType.LOAN_AGAINST_PROPERTY) {
            FinancialRules.calculateEmi(sanction.borrowerSafeRange.midpoint(), stressRate, request.preferredTenureMonths)
        } else proposedEmi

        val stressTotalEmi = profile.existingMonthlyEmi + stressEmi
        val stressFoir = if (stressIncome > 0) stressTotalEmi / stressIncome else 1.0
        val stressRemaining = stressIncome - stressTotalEmi -
            (profile.estimatedHouseholdExpenses ?: affordability.availableMonthlyCapacity * 0.3)

        val outcome = when {
            stressFoir <= StressTest.MANAGEABLE_FOIR_THRESHOLD -> StressOutcome.MANAGEABLE
            stressFoir <= StressTest.RISKY_FOIR_THRESHOLD     -> StressOutcome.RISKY
            else                                               -> StressOutcome.NOT_RECOMMENDED
        }

        val stressExplanation = when (outcome) {
            StressOutcome.MANAGEABLE -> "Even under stress, monthly outflow stays within a manageable range. " +
                "Remaining income: ${formatLakhCrore(stressRemaining.coerceAtLeast(0.0))}/month."
            StressOutcome.RISKY      -> "Under stress, the debt load becomes difficult. " +
                "Consider a lower EMI or shorter loan amount as a buffer."
            StressOutcome.NOT_RECOMMENDED -> "Under stress, this loan becomes very hard to service. " +
                "The recommended amount should be reduced significantly."
        }

        return StressResult(
            scenario              = scenario,
            normalEmi             = proposedEmi,
            stressEmi             = stressEmi,
            normalRemainingIncome = normalRemaining.coerceAtLeast(0.0),
            stressRemainingIncome = stressRemaining.coerceAtLeast(0.0),
            outcome               = outcome,
            explanation           = stressExplanation
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIDENCE
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateConfidence(profile: BorrowerProfile): Pair<Confidence, String> {
        var score = 0
        val known = mutableListOf<String>()
        val missing = mutableListOf<String>()

        if (profile.creditScoreStatus != CreditScoreStatus.UNKNOWN) {
            score += FinancialRules.ConfidenceScoring.CREDIT_SCORE_KNOWN; known.add("credit score")
        } else missing.add("credit score")

        if (profile.estimatedHouseholdExpenses != null) {
            score += FinancialRules.ConfidenceScoring.HOUSEHOLD_EXPENSES_KNOWN; known.add("household expenses")
        } else missing.add("exact household expenses")

        if (profile.employmentStability != null) {
            score += FinancialRules.ConfidenceScoring.EMPLOYMENT_STABILITY_KNOWN; known.add("employment stability")
        }

        if (profile.incomeVariabilityPercent != null) {
            score += FinancialRules.ConfidenceScoring.INCOME_VARIABILITY_KNOWN; known.add("income variability")
        }

        if (profile.emergencySavingsMonths != null) {
            score += FinancialRules.ConfidenceScoring.EMERGENCY_SAVINGS_KNOWN; known.add("emergency savings")
        } else missing.add("emergency savings")

        if (profile.creditUtilisationPercent != null) {
            score += FinancialRules.ConfidenceScoring.CREDIT_UTILISATION_KNOWN; known.add("credit utilisation")
        }

        if (profile.businessVintageYears != null) {
            score += FinancialRules.ConfidenceScoring.BUSINESS_VINTAGE_KNOWN; known.add("business vintage")
        }

        if (profile.itrAnnualIncome != null) {
            score += FinancialRules.ConfidenceScoring.ITR_INCOME_KNOWN; known.add("ITR income")
        }

        if (profile.collateralValueLakh != null) {
            score += FinancialRules.ConfidenceScoring.COLLATERAL_KNOWN; known.add("collateral value")
        }

        val confidence = when {
            score >= FinancialRules.ConfidenceScoring.HIGH_THRESHOLD   -> Confidence.HIGH
            score >= FinancialRules.ConfidenceScoring.MEDIUM_THRESHOLD -> Confidence.MEDIUM
            else                                                        -> Confidence.LOW
        }

        val explanation = buildString {
            if (known.isNotEmpty()) append("Known: ${known.joinToString(", ")}. ")
            if (missing.isNotEmpty()) append("Unknown: ${missing.joinToString(", ")} — ranges are wider.")
        }

        return Pair(confidence, explanation)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERDICT
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateVerdict(
        profile: BorrowerProfile,
        request: LoanRequest,
        affordability: AffordabilityResult,
        sanction: SanctionEstimate,
        stress: StressResult,
        warnings: MutableList<String>
    ): Triple<Verdict, String, List<String>> {
        val reasons = mutableListOf<String>()
        var dontBorrowScore = 0
        var borrowLessScore = 0

        // HIGH-COST DEBT CHECK (Anita-type)
        if (profile.existingHighCostDebt != null && profile.existingHighCostDebt > 0) {
            val existingFoir = profile.existingMonthlyEmi / profile.netMonthlyIncome
            if (existingFoir > HighCostDebtRules.HIGH_DEBT_FOIR_OVERRIDE) {
                dontBorrowScore += 3
                reasons.add("Existing debt obligations already exceed ${(HighCostDebtRules.HIGH_DEBT_FOIR_OVERRIDE * 100).toInt()}% of income.")
            }
            val existingRate = profile.existingHighCostRate ?: 0.0
            if (existingRate > HighCostDebtRules.HIGH_COST_RATE_THRESHOLD * 100) {
                dontBorrowScore += 2
                reasons.add("Existing high-cost debt at ${existingRate.toInt()}%+ should be cleared first.")
            }
        }

        // RECENT EMI BOUNCE
        if (profile.recentEmiBounce) {
            dontBorrowScore += 3
            reasons.add("Recent EMI bounce signals repayment stress — adding more debt is risky now.")
        }

        // MULTIPLE APP LOANS
        if (profile.appLoanCount >= HighCostDebtRules.APP_LOAN_WARNING_COUNT) {
            dontBorrowScore += 2
            reasons.add("${profile.appLoanCount} active app loans indicate over-leverage.")
        }

        // FOIR CHECK
        val foirResult = affordability.proposedFoir
        when {
            foirResult > FoirThresholds.ABSOLUTE_MAX -> {
                dontBorrowScore += 3
                reasons.add("Proposed FOIR of ${(foirResult * 100).toInt()}% exceeds maximum safe limit of ${(FoirThresholds.ABSOLUTE_MAX * 100).toInt()}%.")
            }
            foirResult > FoirThresholds.MODERATE_MAX -> {
                borrowLessScore += 2
                reasons.add("Proposed FOIR of ${(foirResult * 100).toInt()}% is above the comfortable range.")
            }
        }

        // REQUESTED AMOUNT vs SAFE AMOUNT
        val requested = request.requestedAmountRs
        val safeMax = sanction.borrowerSafeRange.high
        val lenderMax = sanction.lenderLikelyRange.high
        when {
            requested > lenderMax * 1.1 -> {
                borrowLessScore += 2
                reasons.add("Requested amount significantly exceeds estimated lender sanction range.")
            }
            requested > safeMax -> {
                borrowLessScore += 2
                reasons.add("Requested ${formatLakhCrore(requested)} exceeds borrower-safe range of ${sanction.borrowerSafeRange.formatted()}.")
            }
        }

        // SAFE EMI vs PROPOSED EMI
        if (affordability.maxSafeMonthlyEmi <= 0) {
            dontBorrowScore += 3
            reasons.add("No remaining monthly capacity after existing obligations and essential expenses.")
        } else if (affordability.safeCapacityUsed > 1.2) {
            borrowLessScore += 2
            reasons.add("Proposed EMI would exceed your safe repayment capacity.")
        }

        // STRESS TEST OUTCOME
        when (stress.outcome) {
            StressOutcome.NOT_RECOMMENDED -> { dontBorrowScore += 2; reasons.add("Stress scenario shows this loan becomes unserviceable under income pressure.") }
            StressOutcome.RISKY           -> { borrowLessScore += 1; reasons.add("Stress scenario shows moderate repayment risk.") }
            else -> {}
        }

        // PRODUCTIVE PURPOSE (reduces concern for self-employed)
        if (request.isProductivePurpose && profile.incomeType == IncomeType.SELF_EMPLOYED) {
            borrowLessScore = (borrowLessScore - 1).coerceAtLeast(0)
            reasons.add("Productive purpose (income-generating) moderately improves the case.")
        }

        // STRONG PROFILE
        val strongProfile = profile.creditScoreStatus in listOf(CreditScoreStatus.EXCELLENT, CreditScoreStatus.VERY_GOOD) &&
            profile.incomeType == IncomeType.SALARIED &&
            (profile.employmentStability == EmploymentStability.MORE_THAN_5_YEARS || profile.employmentStability == EmploymentStability.THREE_TO_FIVE_YEARS) &&
            !profile.recentEmiBounce
        if (strongProfile) {
            reasons.add("Strong credit profile and stable employment support this loan.")
        }

        // FINAL VERDICT
        val verdict = when {
            dontBorrowScore >= 5 -> Verdict.DONT_BORROW
            dontBorrowScore >= 3 -> Verdict.DONT_BORROW
            borrowLessScore >= 3 || dontBorrowScore >= 1 -> Verdict.BORROW_LESS
            else -> if (requested <= safeMax) Verdict.BORROW else Verdict.BORROW_LESS
        }

        val explanation = when (verdict) {
            Verdict.BORROW      -> "Your profile supports this loan at the requested amount. Proceed, but stay within the recommended EMI ceiling."
            Verdict.BORROW_LESS -> "Your requested EMI or amount exceeds your safer affordability range. Borrowing a smaller amount at a comfortable EMI is strongly recommended."
            Verdict.DONT_BORROW -> buildDontBorrowExplanation(profile, reasons)
        }

        return Triple(verdict, explanation, reasons.take(4))
    }

    private fun buildDontBorrowExplanation(profile: BorrowerProfile, reasons: List<String>): String {
        val base = "Adding a new loan at this time would increase your financial stress, not reduce it."
        val suggestion = when {
            profile.existingHighCostDebt != null && profile.existingHighCostDebt > 0 ->
                " First focus on closing or reducing high-cost debt, then reassess in 3–6 months."
            profile.recentEmiBounce ->
                " Re-establish consistent repayment for 3–6 months, then revisit this application."
            else -> " Improve your repayment position before taking on additional debt."
        }
        return base + suggestion
    }

    private fun buildEmiExplanation(
        profile: BorrowerProfile,
        affordability: AffordabilityResult,
        sanction: SanctionEstimate
    ): String {
        return "Recommended EMI ceiling based on net income of ${formatLakhCrore(profile.netMonthlyIncome)}/month, " +
            "existing EMI of ${formatLakhCrore(profile.existingMonthlyEmi)}/month, " +
            "and estimated living expenses. Using safe borrowing amount of " +
            sanction.borrowerSafeRange.formatted() + "."
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEGOTIATION CARD BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    fun buildNegotiationCard(
        result: AssessmentResult,
        request: LoanRequest
    ): NegotiationCard {
        val aprRange = RateRange(result.aprResult.estimatedAprLow, result.aprResult.estimatedAprHigh)
        return NegotiationCard(
            loanPurpose           = request.purpose,
            loanType              = request.loanType,
            requestedAmountRs     = request.requestedAmountRs,
            safeBorrowingRange    = result.sanctionEstimate.borrowerSafeRange,
            recommendedEmiCeiling = result.recommendedMaxEmi,
            fairInterestRange     = result.fairRateRange,
            estimatedAprRange     = aprRange,
            likelyLenderSanction  = result.sanctionEstimate.lenderLikelyRange,
            topReasons            = result.keyReasons.take(3),
            verdict               = result.verdict,
            confidence            = result.confidence
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun formatLakhCrore(amount: Double): String {
        val lakh  = 100_000.0
        val crore = 10_000_000.0
        return when {
            amount >= crore -> "₹${String.format("%.1f", amount / crore)}Cr"
            amount >= lakh  -> "₹${String.format("%.1f", amount / lakh)}L"
            else            -> "₹${String.format("%,.0f", amount)}"
        }
    }
}
