package com.borrowercopilot.domain.questions

/**
 * BorrowerFixtures — pre-built answer sets for the three test borrowers.
 *
 * IMPORTANT: These are INPUTS to the rules engine.
 * The outputs (results) are NOT hard-coded — they are calculated by
 * BorrowerAssessmentEngine from these answers.
 *
 * Changing a rule in FinancialRules.kt will automatically change
 * what these fixtures produce.
 */
object BorrowerFixtures {

    /**
     * PRIYA — Salaried, Bengaluru, personal loan for wedding
     * Age: 29, MNC employee, 5 years, ₹1,10,000/month, car EMI ₹14,000, score 780
     */
    val PRIYA: Map<QuestionId, QuestionAnswer> = mapOf(
        QuestionId.LOAN_PURPOSE          to qa(QuestionId.LOAN_PURPOSE, option = "WEDDING"),
        QuestionId.LOAN_TYPE             to qa(QuestionId.LOAN_TYPE, option = "PERSONAL_LOAN"),
        QuestionId.AMOUNT_WANTED         to qa(QuestionId.AMOUNT_WANTED, numeric = 800_000.0),
        QuestionId.INCOME_TYPE           to qa(QuestionId.INCOME_TYPE, option = "SALARIED"),
        QuestionId.NET_MONTHLY_INCOME    to qa(QuestionId.NET_MONTHLY_INCOME, numeric = 110_000.0),
        QuestionId.EXISTING_EMI          to qa(QuestionId.EXISTING_EMI, numeric = 14_000.0),
        QuestionId.HOUSEHOLD_EXPENSES    to qa(QuestionId.HOUSEHOLD_EXPENSES, numeric = 42_000.0),  // 28k rent + 14k other
        QuestionId.AGE                   to qa(QuestionId.AGE, numeric = 29.0),
        QuestionId.CREDIT_SCORE          to qa(QuestionId.CREDIT_SCORE, option = "VERY_GOOD"),       // 780 = VERY_GOOD band
        QuestionId.LOCATION              to qa(QuestionId.LOCATION, text = "Bengaluru"),
        // Salaried adaptive
        QuestionId.EMPLOYMENT_STABILITY  to qa(QuestionId.EMPLOYMENT_STABILITY, option = "MORE_THAN_5_YEARS"),
        QuestionId.VARIABLE_INCOME       to qa(QuestionId.VARIABLE_INCOME, option = "NONE"),
        QuestionId.CREDIT_UTILISATION    to qa(QuestionId.CREDIT_UTILISATION, option = "LOW"),
        QuestionId.EMERGENCY_SAVINGS     to qa(QuestionId.EMERGENCY_SAVINGS, option = "THREE_PLUS"),
        QuestionId.UPCOMING_LARGE_EXPENSE to qa(QuestionId.UPCOMING_LARGE_EXPENSE, option = "false"),
        QuestionId.EXISTING_LENDER_OFFER to qa(QuestionId.EXISTING_LENDER_OFFER, option = "NO")
    )

    /**
     * RAVI — Self-employed, Mysuru, shop owner seeking ₹15L for business
     * Age: 42, 14y vintage, cash ₹40-80k, ITR ₹4.2L/yr, shop ₹45L unencumbered, no credit history
     */
    val RAVI: Map<QuestionId, QuestionAnswer> = mapOf(
        QuestionId.LOAN_PURPOSE          to qa(QuestionId.LOAN_PURPOSE, option = "BUSINESS_EXPANSION"),
        QuestionId.LOAN_TYPE             to qa(QuestionId.LOAN_TYPE, option = "BUSINESS_LOAN"),
        QuestionId.AMOUNT_WANTED         to qa(QuestionId.AMOUNT_WANTED, numeric = 1_500_000.0),
        QuestionId.INCOME_TYPE           to qa(QuestionId.INCOME_TYPE, option = "SELF_EMPLOYED"),
        QuestionId.NET_MONTHLY_INCOME    to qa(QuestionId.NET_MONTHLY_INCOME, numeric = 60_000.0),  // midpoint of 40-80k range
        QuestionId.EXISTING_EMI          to qa(QuestionId.EXISTING_EMI, numeric = 0.0),
        QuestionId.HOUSEHOLD_EXPENSES    to qa(QuestionId.HOUSEHOLD_EXPENSES, numeric = 30_000.0),
        QuestionId.AGE                   to qa(QuestionId.AGE, numeric = 42.0),
        QuestionId.CREDIT_SCORE          to qa(QuestionId.CREDIT_SCORE, option = "UNKNOWN"),         // no formal credit history
        QuestionId.LOCATION              to qa(QuestionId.LOCATION, text = "Mysuru"),
        // Self-employed adaptive
        QuestionId.BUSINESS_VINTAGE      to qa(QuestionId.BUSINESS_VINTAGE, numeric = 14.0),
        QuestionId.ITR_INCOME            to qa(QuestionId.ITR_INCOME, numeric = 420_000.0),          // ₹4.2L/yr
        QuestionId.INCOME_VARIABILITY    to qa(QuestionId.INCOME_VARIABILITY, option = "MODERATE"),
        QuestionId.BUSINESS_ASSETS_COLLATERAL to qa(QuestionId.BUSINESS_ASSETS_COLLATERAL, option = "true"),
        QuestionId.COLLATERAL_VALUE      to qa(QuestionId.COLLATERAL_VALUE, numeric = 45.0),         // 45 lakhs
        QuestionId.COLLATERAL_UNENCUMBERED to qa(QuestionId.COLLATERAL_UNENCUMBERED, option = "true"),
        QuestionId.FORMAL_LOANS          to qa(QuestionId.FORMAL_LOANS, option = "false"),
        QuestionId.SPOUSE_INCOME         to qa(QuestionId.SPOUSE_INCOME, option = "MODERATE"),       // wife ₹18k
        QuestionId.BUSINESS_PURPOSE_PRODUCTIVE to qa(QuestionId.BUSINESS_PURPOSE_PRODUCTIVE, option = "true")
    )

    /**
     * ANITA — Informal/gig, Hubballi, delivery + tailoring, wants ₹1.5L for e-scooter
     * Age: 35, income ₹26-30k, 2 children, 3 app loans ₹35k outstanding, 30%+ rate, recent bounce
     */
    val ANITA: Map<QuestionId, QuestionAnswer> = mapOf(
        QuestionId.LOAN_PURPOSE          to qa(QuestionId.LOAN_PURPOSE, option = "VEHICLE_PURCHASE"),
        QuestionId.LOAN_TYPE             to qa(QuestionId.LOAN_TYPE, option = "TWO_WHEELER_LOAN"),
        QuestionId.AMOUNT_WANTED         to qa(QuestionId.AMOUNT_WANTED, numeric = 150_000.0),
        QuestionId.INCOME_TYPE           to qa(QuestionId.INCOME_TYPE, option = "INFORMAL_GIG"),
        QuestionId.NET_MONTHLY_INCOME    to qa(QuestionId.NET_MONTHLY_INCOME, numeric = 28_000.0),  // midpoint of 26-30k
        QuestionId.EXISTING_EMI          to qa(QuestionId.EXISTING_EMI, numeric = 9_000.0),         // app loan EMIs ~₹9k/month
        QuestionId.HOUSEHOLD_EXPENSES    to qa(QuestionId.HOUSEHOLD_EXPENSES, numeric = 14_000.0),
        QuestionId.AGE                   to qa(QuestionId.AGE, numeric = 35.0),
        QuestionId.CREDIT_SCORE          to qa(QuestionId.CREDIT_SCORE, option = "UNKNOWN"),
        QuestionId.LOCATION              to qa(QuestionId.LOCATION, text = "Hubballi"),
        // Informal adaptive
        QuestionId.INCOME_VOLATILITY     to qa(QuestionId.INCOME_VOLATILITY, option = "VARIABLE"),
        QuestionId.APP_LOANS_COUNT       to qa(QuestionId.APP_LOANS_COUNT, option = "THREE"),
        QuestionId.APP_LOAN_BALANCE      to qa(QuestionId.APP_LOAN_BALANCE, numeric = 35_000.0),
        QuestionId.APP_LOAN_RATE         to qa(QuestionId.APP_LOAN_RATE, option = "ABOVE_36"),
        QuestionId.RECENT_BOUNCE         to qa(QuestionId.RECENT_BOUNCE, option = "true"),
        QuestionId.HOUSEHOLD_DEPENDENTS  to qa(QuestionId.HOUSEHOLD_DEPENDENTS, option = "TWO"),
        QuestionId.INFORMAL_EMERGENCY_SAVINGS to qa(QuestionId.INFORMAL_EMERGENCY_SAVINGS, option = "NONE"),
        QuestionId.PRODUCTIVE_PURPOSE    to qa(QuestionId.PRODUCTIVE_PURPOSE, option = "true")
    )

    // Helper to create a QuestionAnswer
    private fun qa(
        id: QuestionId,
        option: String? = null,
        numeric: Double? = null,
        text: String? = null
    ) = QuestionAnswer(
        questionId     = id,
        selectedOption = option,
        numericValue   = numeric,
        textValue      = text
    )
}
