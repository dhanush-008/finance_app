package com.borrowercopilot.domain.questions

import com.borrowercopilot.domain.model.*

/**
 * AdaptiveQuestionEngine
 *
 * Determines the ordered sequence of questions based on answers so far.
 * Every adaptive question is only shown if it will change at least one output.
 *
 * Rules:
 * - MUST questions are always in the sequence
 * - Adaptive questions are gated on: income type + prior answers
 * - A question is skipped if its prerequisite answer is not met
 */
object AdaptiveQuestionEngine {

    private val MUST_QUESTIONS = listOf(
        QuestionId.LOAN_PURPOSE,
        QuestionId.LOAN_TYPE,
        QuestionId.AMOUNT_WANTED,
        QuestionId.INCOME_TYPE,
        QuestionId.NET_MONTHLY_INCOME,
        QuestionId.EXISTING_EMI,
        QuestionId.HOUSEHOLD_EXPENSES,
        QuestionId.AGE,
        QuestionId.CREDIT_SCORE,
        QuestionId.LOCATION
    )

    /**
     * Returns the complete ordered question sequence given current answers.
     * Call this to build the full list including adaptive questions.
     * As user answers questions, call this again to insert newly-unlocked questions.
     */
    fun buildSequence(answers: Map<QuestionId, QuestionAnswer>): List<QuestionId> {
        val sequence = mutableListOf<QuestionId>()
        sequence.addAll(MUST_QUESTIONS)

        val incomeType = answers[QuestionId.INCOME_TYPE]?.selectedOption

        when (incomeType) {
            "SALARIED" -> sequence.addAll(buildSalariedSequence(answers))
            "SELF_EMPLOYED" -> sequence.addAll(buildSelfEmployedSequence(answers))
            "INFORMAL_GIG" -> sequence.addAll(buildInformalSequence(answers))
        }

        return sequence.distinct()
    }

    private fun buildSalariedSequence(answers: Map<QuestionId, QuestionAnswer>): List<QuestionId> {
        val list = mutableListOf<QuestionId>()

        // Always ask employment stability — affects rate and lender confidence
        list.add(QuestionId.EMPLOYMENT_STABILITY)

        // Ask variable income if income is meaningful to assess
        list.add(QuestionId.VARIABLE_INCOME)

        // Ask credit utilisation if credit score is unknown or below VERY_GOOD
        val creditStatus = answers[QuestionId.CREDIT_SCORE]?.selectedOption
        if (creditStatus != "EXCELLENT") {
            list.add(QuestionId.CREDIT_UTILISATION)
        }

        // Emergency savings — affects stress test
        list.add(QuestionId.EMERGENCY_SAVINGS)

        // Upcoming large expense — affects capacity
        list.add(QuestionId.UPCOMING_LARGE_EXPENSE)

        // Lender offer — only useful if they may already have one (optional)
        list.add(QuestionId.EXISTING_LENDER_OFFER)

        return list
    }

    private fun buildSelfEmployedSequence(answers: Map<QuestionId, QuestionAnswer>): List<QuestionId> {
        val list = mutableListOf<QuestionId>()

        // Business vintage — critical for business loan routing
        list.add(QuestionId.BUSINESS_VINTAGE)

        // ITR income — critical for documentation mismatch check
        list.add(QuestionId.ITR_INCOME)

        // Income variability
        list.add(QuestionId.INCOME_VARIABILITY)

        // Collateral check — can unlock LAP route
        list.add(QuestionId.BUSINESS_ASSETS_COLLATERAL)

        // If they said yes to collateral, ask value and encumbrance
        val hasCollateral = answers[QuestionId.BUSINESS_ASSETS_COLLATERAL]?.selectedOption
        if (hasCollateral == "true" || hasCollateral == null) {
            // Ask only if they answered YES or haven't answered yet (keep in queue)
            if (hasCollateral == "true") {
                list.add(QuestionId.COLLATERAL_VALUE)
                list.add(QuestionId.COLLATERAL_UNENCUMBERED)
            }
        }

        // Formal loan history
        list.add(QuestionId.FORMAL_LOANS)

        // Spouse income — can support joint application
        list.add(QuestionId.SPOUSE_INCOME)

        // Productive purpose — affects verdict weighting
        list.add(QuestionId.BUSINESS_PURPOSE_PRODUCTIVE)

        return list
    }

    private fun buildInformalSequence(answers: Map<QuestionId, QuestionAnswer>): List<QuestionId> {
        val list = mutableListOf<QuestionId>()

        // Income volatility — tightens FOIR ceiling
        list.add(QuestionId.INCOME_VOLATILITY)

        // App loans — critical for Don't Borrow trigger
        list.add(QuestionId.APP_LOANS_COUNT)

        // If they have app loans, ask balance and rate
        val appLoanCount = answers[QuestionId.APP_LOANS_COUNT]?.selectedOption
        if (appLoanCount != null && appLoanCount != "NONE") {
            list.add(QuestionId.APP_LOAN_BALANCE)
            list.add(QuestionId.APP_LOAN_RATE)
        }

        // Recent bounce — strongest stress signal
        list.add(QuestionId.RECENT_BOUNCE)

        // Dependents — raises essential expenses
        list.add(QuestionId.HOUSEHOLD_DEPENDENTS)

        // Emergency savings
        list.add(QuestionId.INFORMAL_EMERGENCY_SAVINGS)

        // Productive purpose
        list.add(QuestionId.PRODUCTIVE_PURPOSE)

        return list
    }

    /**
     * Returns the next unanswered question in the current sequence.
     * Returns null if all questions have been answered.
     */
    fun nextQuestion(answers: Map<QuestionId, QuestionAnswer>): QuestionId? {
        val sequence = buildSequence(answers)
        return sequence.firstOrNull { qId ->
            val answer = answers[qId]
            answer == null || !answer.isAnswered
        }
    }

    /**
     * Returns progress: (answered count, total count).
     * Includes only the questions in the current adaptive sequence.
     */
    fun progress(answers: Map<QuestionId, QuestionAnswer>): Pair<Int, Int> {
        val sequence = buildSequence(answers)
        val answered = sequence.count { answers[it]?.isAnswered == true }
        return Pair(answered, sequence.size)
    }

    /**
     * Returns true if the MUST questions are all answered — enabling calculation.
     */
    fun canCalculate(answers: Map<QuestionId, QuestionAnswer>): Boolean {
        val required = listOf(
            QuestionId.LOAN_PURPOSE,
            QuestionId.LOAN_TYPE,
            QuestionId.AMOUNT_WANTED,
            QuestionId.INCOME_TYPE,
            QuestionId.NET_MONTHLY_INCOME,
            QuestionId.EXISTING_EMI,
            QuestionId.AGE,
            QuestionId.CREDIT_SCORE
        )
        return required.all { answers[it]?.isAnswered == true }
    }

    /**
     * Converts a set of QuestionAnswers into a BorrowerProfile + LoanRequest.
     * Unknown values remain null — never defaulted to zero.
     */
    fun buildProfile(answers: Map<QuestionId, QuestionAnswer>): BorrowerProfile {
        fun ans(id: QuestionId) = answers[id]
        fun str(id: QuestionId) = ans(id)?.selectedOption
        fun num(id: QuestionId) = ans(id)?.numericValue
        fun txt(id: QuestionId) = ans(id)?.textValue
        fun bool(id: QuestionId): Boolean = str(id) == "true"

        val incomeType = when (str(QuestionId.INCOME_TYPE)) {
            "SALARIED"      -> IncomeType.SALARIED
            "SELF_EMPLOYED" -> IncomeType.SELF_EMPLOYED
            else            -> IncomeType.INFORMAL_GIG
        }

        val creditScoreStatus = when (str(QuestionId.CREDIT_SCORE)) {
            "EXCELLENT" -> CreditScoreStatus.EXCELLENT
            "VERY_GOOD" -> CreditScoreStatus.VERY_GOOD
            "GOOD"      -> CreditScoreStatus.GOOD
            "FAIR"      -> CreditScoreStatus.FAIR
            "POOR"      -> CreditScoreStatus.POOR
            else        -> CreditScoreStatus.UNKNOWN  // NEVER treated as 0 or 300
        }

        val employmentStability = when (str(QuestionId.EMPLOYMENT_STABILITY)) {
            "MORE_THAN_5_YEARS"   -> EmploymentStability.MORE_THAN_5_YEARS
            "THREE_TO_FIVE_YEARS" -> EmploymentStability.THREE_TO_FIVE_YEARS
            "ONE_TO_THREE_YEARS"  -> EmploymentStability.ONE_TO_THREE_YEARS
            "LESS_THAN_1_YEAR"    -> EmploymentStability.LESS_THAN_1_YEAR
            else                  -> null
        }

        // App loan count
        val appLoanCount = when (str(QuestionId.APP_LOANS_COUNT)) {
            "ONE"      -> 1
            "TWO"      -> 2
            "THREE"    -> 3
            "FOUR_PLUS"-> 4
            else       -> 0
        }

        // App loan rate
        val appLoanRate = when (str(QuestionId.APP_LOAN_RATE)) {
            "BELOW_24"  -> 22.0
            "24_TO_36"  -> 30.0
            "ABOVE_36"  -> 40.0
            else        -> null
        }

        // Income variability
        val incomeVariability = when (str(QuestionId.INCOME_VARIABILITY) ?: str(QuestionId.VARIABLE_INCOME)) {
            "HIGH", "SEASONAL" -> 50.0
            "MODERATE"         -> 30.0
            "LOW"              -> 15.0
            "STABLE"           -> 10.0
            else               -> null
        }

        // Collateral value in rupees (input was in lakhs)
        val collateralValueLakh = num(QuestionId.COLLATERAL_VALUE)

        // Spouse income midpoint estimate
        val spouseIncome = when (str(QuestionId.SPOUSE_INCOME)) {
            "LOW"      -> 12_000.0
            "MODERATE" -> 22_000.0
            "HIGH"     -> 35_000.0
            else       -> null
        }

        // Emergency savings in months
        val emergencySavingsMonths = when (str(QuestionId.EMERGENCY_SAVINGS) ?: str(QuestionId.INFORMAL_EMERGENCY_SAVINGS)) {
            "NONE", "SMALL" -> 0.0
            "ONE_TO_THREE", "SOME" -> 2.0
            "THREE_PLUS", "ADEQUATE" -> 4.0
            else -> null
        }

        val creditUtilisation = when (str(QuestionId.CREDIT_UTILISATION)) {
            "LOW"      -> 20.0
            "MODERATE" -> 45.0
            "HIGH"     -> 75.0
            else       -> null
        }

        val numberOfDependents = when (str(QuestionId.HOUSEHOLD_DEPENDENTS)) {
            "ONE"       -> 1
            "TWO"       -> 2
            "THREE_PLUS"-> 3
            else        -> 0
        }

        return BorrowerProfile(
            age                     = num(QuestionId.AGE)?.toInt() ?: 30,
            location                = txt(QuestionId.LOCATION) ?: str(QuestionId.LOCATION) ?: "",
            incomeType              = incomeType,
            netMonthlyIncome        = num(QuestionId.NET_MONTHLY_INCOME) ?: 0.0,
            incomeVariabilityPercent= incomeVariability,
            itrAnnualIncome         = num(QuestionId.ITR_INCOME),
            spouseMonthlyIncome     = spouseIncome,
            employmentStability     = employmentStability,
            yearsWithEmployer       = num(QuestionId.YEARS_WITH_EMPLOYER),
            businessVintageYears    = num(QuestionId.BUSINESS_VINTAGE),
            monthlyRent             = null,  // captured inside household expenses
            estimatedHouseholdExpenses = num(QuestionId.HOUSEHOLD_EXPENSES),
            numberOfDependents      = numberOfDependents,
            existingMonthlyEmi      = num(QuestionId.EXISTING_EMI) ?: 0.0,
            existingHighCostDebt    = num(QuestionId.APP_LOAN_BALANCE),
            existingHighCostRate    = appLoanRate,
            appLoanCount            = appLoanCount,
            recentEmiBounce         = bool(QuestionId.RECENT_BOUNCE),
            creditScoreStatus       = creditScoreStatus,
            creditScore             = null,  // we store band, not exact score
            creditUtilisationPercent= creditUtilisation,
            collateralValueLakh     = collateralValueLakh,
            collateralDescription   = null,
            isCollateralUnencumbered= bool(QuestionId.COLLATERAL_UNENCUMBERED),
            emergencySavingsMonths  = emergencySavingsMonths,
            hasUpcomingLargeExpense = bool(QuestionId.UPCOMING_LARGE_EXPENSE),
            upcomingExpenseAmount   = null,
            existingLenderOfferRate = when (str(QuestionId.EXISTING_LENDER_OFFER)) {
                "BELOW_12" -> 11.0
                "12_TO_16" -> 14.0
                "ABOVE_16" -> 18.0
                else       -> null
            }
        )
    }

    fun buildLoanRequest(answers: Map<QuestionId, QuestionAnswer>): LoanRequest {
        fun str(id: QuestionId) = answers[id]?.selectedOption
        fun num(id: QuestionId) = answers[id]?.numericValue
        fun bool(id: QuestionId) = str(id) == "true"

        val loanType = when (str(QuestionId.LOAN_TYPE)) {
            "HOME_LOAN"             -> LoanType.HOME_LOAN
            "LOAN_AGAINST_PROPERTY" -> LoanType.LOAN_AGAINST_PROPERTY
            "GOLD_LOAN"             -> LoanType.GOLD_LOAN
            "TWO_WHEELER_LOAN"      -> LoanType.TWO_WHEELER_LOAN
            "BUSINESS_LOAN"         -> LoanType.BUSINESS_LOAN
            else                    -> LoanType.PERSONAL_LOAN
        }

        val purpose = when (str(QuestionId.LOAN_PURPOSE)) {
            "HOME_PURCHASE"     -> LoanPurpose.HOME_PURCHASE
            "HOME_RENOVATION"   -> LoanPurpose.HOME_RENOVATION
            "BUSINESS_EXPANSION"-> LoanPurpose.BUSINESS_EXPANSION
            "VEHICLE_PURCHASE"  -> LoanPurpose.VEHICLE_PURCHASE
            "EDUCATION"         -> LoanPurpose.EDUCATION
            "MEDICAL"           -> LoanPurpose.MEDICAL
            "DEBT_CONSOLIDATION"-> LoanPurpose.DEBT_CONSOLIDATION
            "WORKING_CAPITAL"   -> LoanPurpose.WORKING_CAPITAL
            else                -> LoanPurpose.OTHER
        }

        val isProductive = bool(QuestionId.BUSINESS_PURPOSE_PRODUCTIVE) ||
            bool(QuestionId.PRODUCTIVE_PURPOSE) ||
            purpose in listOf(LoanPurpose.BUSINESS_EXPANSION, LoanPurpose.WORKING_CAPITAL)

        // Default tenure by loan type
        val defaultTenure = when (loanType) {
            LoanType.HOME_LOAN             -> 240
            LoanType.LOAN_AGAINST_PROPERTY -> 84
            LoanType.PERSONAL_LOAN         -> 36
            LoanType.BUSINESS_LOAN         -> 36
            LoanType.TWO_WHEELER_LOAN      -> 24
            LoanType.GOLD_LOAN             -> 12
        }

        return LoanRequest(
            purpose              = purpose,
            loanType             = loanType,
            requestedAmountRs    = num(QuestionId.AMOUNT_WANTED) ?: 0.0,
            preferredTenureMonths= defaultTenure,
            isProductivePurpose  = isProductive
        )
    }
}
