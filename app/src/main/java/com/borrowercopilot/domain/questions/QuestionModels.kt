package com.borrowercopilot.domain.questions

/**
 * Question model for the adaptive questionnaire.
 * Every question has a unique ID, display text, type, and an optional
 * condition that determines whether it should be shown.
 */

enum class QuestionId {
    // ── MUST questions ──────────────────────────────────────────────────────
    LOAN_PURPOSE,
    LOAN_TYPE,
    AMOUNT_WANTED,
    NET_MONTHLY_INCOME,
    INCOME_TYPE,
    EXISTING_EMI,
    HOUSEHOLD_EXPENSES,
    AGE,
    CREDIT_SCORE,
    LOCATION,

    // ── SALARIED adaptive ───────────────────────────────────────────────────
    EMPLOYMENT_STABILITY,
    YEARS_WITH_EMPLOYER,
    VARIABLE_INCOME,
    CREDIT_UTILISATION,
    EMERGENCY_SAVINGS,
    UPCOMING_LARGE_EXPENSE,
    EXISTING_LENDER_OFFER,

    // ── SELF-EMPLOYED adaptive ──────────────────────────────────────────────
    BUSINESS_VINTAGE,
    ITR_INCOME,
    INCOME_VARIABILITY,
    BUSINESS_ASSETS_COLLATERAL,
    COLLATERAL_VALUE,
    COLLATERAL_UNENCUMBERED,
    CO_APPLICANT,
    FORMAL_LOANS,
    BUSINESS_PURPOSE_PRODUCTIVE,
    SPOUSE_INCOME,

    // ── INFORMAL adaptive ───────────────────────────────────────────────────
    INCOME_RANGE,
    INCOME_VOLATILITY,
    APP_LOANS_COUNT,
    APP_LOAN_BALANCE,
    APP_LOAN_RATE,
    RECENT_BOUNCE,
    HOUSEHOLD_DEPENDENTS,
    PRODUCTIVE_PURPOSE,
    INFORMAL_EMERGENCY_SAVINGS,
}

enum class QuestionType {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    NUMERIC_INPUT,
    TEXT_INPUT,
    BOOLEAN,
    SLIDER
}

data class QuestionOption(
    val value: String,
    val displayText: String,
    val hint: String? = null
)

data class Question(
    val id: QuestionId,
    val text: String,
    val subText: String? = null,
    val type: QuestionType,
    val options: List<QuestionOption> = emptyList(),
    val unit: String? = null,
    val placeholder: String? = null,
    val isOptional: Boolean = false,
    val whyItMatters: String? = null
)

/** Holds current answer state for the questionnaire */
data class QuestionAnswer(
    val questionId: QuestionId,
    val selectedOption: String? = null,    // for SINGLE_CHOICE / BOOLEAN
    val numericValue: Double? = null,       // for NUMERIC_INPUT / SLIDER
    val textValue: String? = null           // for TEXT_INPUT
) {
    val isAnswered: Boolean get() = selectedOption != null || numericValue != null || textValue != null
    val isSkipped: Boolean get() = !isAnswered
}
