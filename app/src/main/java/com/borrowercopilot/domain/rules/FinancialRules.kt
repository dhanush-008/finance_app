package com.borrowercopilot.domain.rules

import com.borrowercopilot.domain.model.*

/**
 * FINANCIAL RULES — single source of truth for every threshold, band, and assumption.
 *
 * To change a rule (e.g. FOIR limit), edit ONLY this file.
 * The engine reads from here; no magic numbers exist in UI or engine code.
 *
 * Every value is documented with a Why and Source.
 * Where it is our own judgement, it is labelled: MY JUDGEMENT
 */
object FinancialRules {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. FOIR — Fixed Obligations to Income Ratio
    //    Measures (existing EMI + proposed EMI) / net monthly income
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Conservative FOIR ceiling — borrower-safe threshold.
     * We RECOMMEND borrowers stay within this band.
     * Why: Leaves room for essential living expenses and emergencies.
     * Source: MY JUDGEMENT, consistent with RBI guidance that PSBs often use 40-50%.
     */
    object FoirThresholds {
        /** Salaried, stable employment, no dependents */
        const val CONSERVATIVE_MAX = 0.40      // 40%

        /** Salaried with dependents OR moderate income stability */
        const val MODERATE_MAX = 0.45          // 45%

        /** Self-employed with strong collateral or very high income */
        const val HIGH_RISK_MAX = 0.55         // 55%

        /** Informal / gig income — tighter ceiling due to volatility */
        const val INFORMAL_MAX = 0.35          // 35%

        /**
         * Beyond this FOIR, verdict must be DONT_BORROW or BORROW_LESS regardless.
         * Source: MY JUDGEMENT
         */
        const val ABSOLUTE_MAX = 0.65          // 65%
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. BORROWER-SAFE EMI: Household expense adjustments
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Minimum essential monthly expenses per person in household (urban India estimate).
     * Used when borrower hasn't stated household expenses.
     * Source: MY JUDGEMENT — broad India household spend approximation
     */
    object HouseholdExpenseEstimates {
        const val URBAN_PER_PERSON_MIN = 8_000.0   // ₹8,000/month/person
        const val RURAL_PER_PERSON_MIN = 5_000.0   // ₹5,000/month/person
        const val DEPENDENT_EXTRA      = 4_000.0   // extra per dependent child
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. CREDIT SCORE BANDS
    //    How we classify a stated credit score
    // ─────────────────────────────────────────────────────────────────────────

    object CreditScoreBands {
        const val POOR_MAX      = 599
        const val FAIR_MIN      = 600
        const val FAIR_MAX      = 699
        const val GOOD_MIN      = 700
        const val GOOD_MAX      = 749
        const val VERY_GOOD_MIN = 750
        const val VERY_GOOD_MAX = 799
        const val EXCELLENT_MIN = 800

        fun classify(score: Int): CreditScoreStatus = when {
            score < FAIR_MIN      -> CreditScoreStatus.POOR
            score < GOOD_MIN      -> CreditScoreStatus.FAIR
            score < VERY_GOOD_MIN -> CreditScoreStatus.GOOD
            score < EXCELLENT_MIN -> CreditScoreStatus.VERY_GOOD
            else                  -> CreditScoreStatus.EXCELLENT
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. BASE INTEREST RATE BANDS (indicative, India market)
    //    Source: MY JUDGEMENT — based on publicly available lender ranges
    //    as of 2024. Actual lender quotes may differ.
    // ─────────────────────────────────────────────────────────────────────────

    data class RateBandDef(
        val lowPct: Double,
        val highPct: Double,
        val description: String
    )

    val BASE_RATE_BANDS: Map<LoanType, RateBandDef> = mapOf(
        LoanType.PERSONAL_LOAN         to RateBandDef(10.5, 24.0,  "Unsecured; wide spread based on profile"),
        LoanType.HOME_LOAN             to RateBandDef(8.4,  12.0,  "Secured; lowest rates, PSB/HDFC range"),
        LoanType.LOAN_AGAINST_PROPERTY to RateBandDef(9.5,  14.0,  "Secured; moderate risk"),
        LoanType.GOLD_LOAN             to RateBandDef(9.0,  18.0,  "Secured gold; varies by lender type"),
        LoanType.TWO_WHEELER_LOAN      to RateBandDef(10.0, 20.0,  "Semi-secured; wide profile range"),
        LoanType.BUSINESS_LOAN         to RateBandDef(12.0, 24.0,  "Unsecured business; higher risk premium")
    )

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CREDIT SCORE RATE ADJUSTMENTS
    //    Delta applied to base rate band based on credit quality
    //    Source: MY JUDGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    object CreditScoreRateAdj {
        const val EXCELLENT_ADJ  = -1.5   // tighten range downward
        const val VERY_GOOD_ADJ  = -0.75
        const val GOOD_ADJ       =  0.0   // no change from base
        const val FAIR_ADJ       = +2.0   // push rate up
        const val POOR_ADJ       = +4.0
        const val UNKNOWN_WIDEN  =  2.0   // widen both ends by this amount
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. INCOME STABILITY RATE ADJUSTMENTS
    //    Source: MY JUDGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    object IncomeStabilityRateAdj {
        const val SALARIED_LARGE_MNC  = -0.5
        const val SALARIED_STABLE     =  0.0
        const val SALARIED_NEW        = +1.0  // < 1 year
        const val SELF_EMPLOYED_OLD   = +0.5  // > 5 years vintage
        const val SELF_EMPLOYED_YOUNG = +2.0  // < 2 years vintage
        const val INFORMAL            = +3.0
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. COLLATERAL / SECURED LOAN ADJUSTMENTS
    //    Source: MY JUDGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    object CollateralAdj {
        const val SECURED_RATE_REDUCTION = -2.5  // secured loan rate reduction
        const val SECURED_SANCTION_BOOST = 0.6   // up to 60% of collateral value (LTV)
        const val LAP_MAX_LTV            = 0.6   // 60% LTV for Loan Against Property
        const val HOME_LOAN_MAX_LTV      = 0.75  // 75–80% LTV for home loans
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. PROCESSING FEE ASSUMPTIONS
    //    Source: MY JUDGEMENT — typical market ranges
    // ─────────────────────────────────────────────────────────────────────────

    val PROCESSING_FEE_BANDS: Map<LoanType, Double> = mapOf(
        LoanType.PERSONAL_LOAN         to 0.02,   // 2%
        LoanType.HOME_LOAN             to 0.005,  // 0.5%
        LoanType.LOAN_AGAINST_PROPERTY to 0.01,   // 1%
        LoanType.GOLD_LOAN             to 0.005,  // 0.5%
        LoanType.TWO_WHEELER_LOAN      to 0.015,  // 1.5%
        LoanType.BUSINESS_LOAN         to 0.02    // 2%
    )

    // ─────────────────────────────────────────────────────────────────────────
    // 9. LENDER SANCTION MULTIPLIERS
    //    Typical bank multiplier on net monthly income for unsecured loans
    //    Source: MY JUDGEMENT — common RBI/PSB guidelines say 20–24x NMI cap
    // ─────────────────────────────────────────────────────────────────────────

    object LenderSanctionMultipliers {
        // Max principal = multiplier × NMI (before FOIR constraint)
        const val SALARIED_EXCELLENT_CREDIT = 24.0
        const val SALARIED_GOOD_CREDIT      = 20.0
        const val SALARIED_FAIR_CREDIT      = 15.0
        const val SALARIED_POOR_CREDIT      = 10.0
        const val SELF_EMPLOYED             = 18.0
        const val INFORMAL                  = 10.0
        const val UNKNOWN_CREDIT            = 16.0  // mid-range when unknown
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. STRESS TEST ASSUMPTIONS
    //     Source: MY JUDGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    object StressTest {
        const val SALARIED_INCOME_DROP_PCT   = 0.20   // income falls 20%
        const val SELF_EMPLOYED_INCOME_FLOOR = 0.60   // falls to 60% of stated
        const val INFORMAL_INCOME_DROP_PCT   = 0.20
        const val RATE_RISE_POINTS           = 2.0    // +2 percentage points on rate
        const val MANAGEABLE_FOIR_THRESHOLD  = 0.50   // FOIR ≤ 50% = still manageable
        const val RISKY_FOIR_THRESHOLD       = 0.65   // FOIR ≤ 65% = risky
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. HIGH-COST DEBT RULES (Anita-type scenario)
    //     Source: MY JUDGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    object HighCostDebtRules {
        const val HIGH_COST_RATE_THRESHOLD   = 0.24   // > 24% = high-cost debt
        const val BOUNCE_RECENT_MONTHS       = 3      // bounce within last 3 months = flag
        const val APP_LOAN_WARNING_COUNT     = 2      // ≥ 2 app loans = flag
        const val HIGH_DEBT_FOIR_OVERRIDE    = 0.50   // if FOIR of existing > 50%, DONT_BORROW
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. PRODUCT ROUTING RULES
    //     Source: MY JUDGEMENT — based on typical Indian lending logic
    // ─────────────────────────────────────────────────────────────────────────

    object ProductRouting {
        /**
         * If borrower has unencumbered collateral worth >= this multiple of requested amount,
         * recommend secured route (LAP/Business Secured) instead of personal loan.
         */
        const val LAP_COLLATERAL_MULTIPLE    = 1.5    // collateral > 1.5x requested = LAP viable
        const val BUSINESS_VINTAGE_MIN_YEARS = 2.0    // min business age for business loan

        /**
         * If ITR income is < X% of stated cash income for self-employed,
         * flag documentation gap and lower confidence.
         */
        const val ITR_CASH_INCOME_RATIO_WARN = 0.40   // ITR < 40% of cash = warn
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 13. CONFIDENCE SCORING
    //     Points system — each meaningful known input adds points
    //     Source: MY JUDGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    object ConfidenceScoring {
        const val CREDIT_SCORE_KNOWN        = 20
        const val HOUSEHOLD_EXPENSES_KNOWN  = 10
        const val EMPLOYMENT_STABILITY_KNOWN= 10
        const val INCOME_VARIABILITY_KNOWN  = 10
        const val EMERGENCY_SAVINGS_KNOWN   = 8
        const val CREDIT_UTILISATION_KNOWN  = 8
        const val BUSINESS_VINTAGE_KNOWN    = 10
        const val ITR_INCOME_KNOWN          = 10
        const val COLLATERAL_KNOWN          = 8

        const val HIGH_THRESHOLD            = 60
        const val MEDIUM_THRESHOLD          = 30
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 14. BORROWER-SAFE RANGE DISCOUNT
    //     The borrower-safe amount is typically lower than lender-likely.
    //     Source: MY JUDGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    object SafeAmountDiscount {
        const val STANDARD_DISCOUNT_PCT = 0.20   // borrower-safe is ~20% below lender max
        const val HIGH_RISK_DISCOUNT_PCT = 0.35  // more cautious for volatile income
        const val INFORMAL_DISCOUNT_PCT  = 0.40
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 15. EMI CALCULATION HELPER
    //     Standard reducing-balance EMI formula
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calculates monthly EMI using standard reducing balance formula.
     * EMI = P × r × (1+r)^n / ((1+r)^n – 1)
     * where r = monthly interest rate, n = tenure in months
     */
    fun calculateEmi(principal: Double, annualRatePercent: Double, tenureMonths: Int): Double {
        if (principal <= 0 || tenureMonths <= 0) return 0.0
        val r = annualRatePercent / 100.0 / 12.0
        if (r == 0.0) return principal / tenureMonths
        val factor = Math.pow(1 + r, tenureMonths.toDouble())
        return principal * r * factor / (factor - 1)
    }

    /**
     * Max principal borrower can afford given an EMI ceiling and rate+tenure.
     * Inverse of EMI formula.
     */
    fun maxPrincipalFromEmi(maxEmi: Double, annualRatePercent: Double, tenureMonths: Int): Double {
        if (maxEmi <= 0 || tenureMonths <= 0) return 0.0
        val r = annualRatePercent / 100.0 / 12.0
        if (r == 0.0) return maxEmi * tenureMonths
        val factor = Math.pow(1 + r, tenureMonths.toDouble())
        return maxEmi * (factor - 1) / (r * factor)
    }

    /**
     * Simplified APR approximation accounting for upfront processing fee.
     * Uses IRR-style approximation: APR ≈ nominal rate + (fee / tenure_years * 2)
     * Not legally precise; labelled as estimate.
     * Source: MY JUDGEMENT — approximation method
     */
    fun estimateApr(
        principal: Double,
        annualRatePercent: Double,
        tenureMonths: Int,
        processingFeePercent: Double
    ): Double {
        val feeAmount = principal * processingFeePercent
        val effectivePrincipal = principal - feeAmount  // borrower receives less
        if (effectivePrincipal <= 0) return annualRatePercent
        val emi = calculateEmi(principal, annualRatePercent, tenureMonths)
        // Iterative IRR approximation
        var lo = annualRatePercent
        var hi = annualRatePercent + processingFeePercent * 100 * 3
        repeat(50) {
            val mid = (lo + hi) / 2.0
            val calcEmi = calculateEmi(effectivePrincipal, mid, tenureMonths)
            if (calcEmi < emi) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 16. TENURE OPTIONS for comparison table
    // ─────────────────────────────────────────────────────────────────────────

    fun tenureOptionsFor(loanType: LoanType): List<Int> = when (loanType) {
        LoanType.HOME_LOAN             -> listOf(120, 180, 240)  // 10, 15, 20 years
        LoanType.LOAN_AGAINST_PROPERTY -> listOf(60, 84, 120)
        LoanType.PERSONAL_LOAN         -> listOf(12, 24, 36, 48, 60)
        LoanType.BUSINESS_LOAN         -> listOf(24, 36, 60)
        LoanType.TWO_WHEELER_LOAN      -> listOf(12, 24, 36)
        LoanType.GOLD_LOAN             -> listOf(3, 6, 12)
    }
}
