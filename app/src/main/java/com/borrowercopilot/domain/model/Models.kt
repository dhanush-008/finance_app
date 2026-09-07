package com.borrowercopilot.domain.model

// ─── Enumerations ────────────────────────────────────────────────────────────

enum class IncomeType {
    SALARIED, SELF_EMPLOYED, INFORMAL_GIG
}

enum class LoanType {
    PERSONAL_LOAN,
    HOME_LOAN,
    LOAN_AGAINST_PROPERTY,
    GOLD_LOAN,
    TWO_WHEELER_LOAN,
    BUSINESS_LOAN
}

enum class LoanPurpose {
    WEDDING, HOME_PURCHASE, HOME_RENOVATION, BUSINESS_EXPANSION,
    VEHICLE_PURCHASE, EDUCATION, MEDICAL, DEBT_CONSOLIDATION,
    WORKING_CAPITAL, OTHER
}

enum class CreditScoreStatus {
    UNKNOWN,
    POOR,       // < 600
    FAIR,       // 600–699
    GOOD,       // 700–749
    VERY_GOOD,  // 750–799
    EXCELLENT   // 800+
}

enum class EmploymentStability {
    LESS_THAN_1_YEAR, ONE_TO_THREE_YEARS, THREE_TO_FIVE_YEARS, MORE_THAN_5_YEARS
}

enum class Verdict {
    BORROW, BORROW_LESS, DONT_BORROW
}

enum class Confidence {
    LOW, MEDIUM, HIGH
}

enum class StressOutcome {
    MANAGEABLE, RISKY, NOT_RECOMMENDED
}

enum class ProductRoute {
    PERSONAL_LOAN, SECURED_LAP, BUSINESS_LOAN, GOLD_LOAN, HOME_LOAN, TWO_WHEELER_LOAN
}

// ─── Domain Data Classes ─────────────────────────────────────────────────────

/**
 * Core borrower profile built from questionnaire answers.
 * Null = not answered / unknown — never silently converted to 0.
 */
data class BorrowerProfile(
    // Basic demographics
    val age: Int,
    val location: String,
    val incomeType: IncomeType,

    // Income
    val netMonthlyIncome: Double,                        // stated net income
    val incomeVariabilityPercent: Double? = null,        // % swing for self-employed / informal
    val itrAnnualIncome: Double? = null,                 // ITR declared income (self-employed)
    val spouseMonthlyIncome: Double? = null,

    // Employment / business
    val employmentStability: EmploymentStability? = null,
    val yearsWithEmployer: Double? = null,
    val businessVintageYears: Double? = null,

    // Expenses
    val monthlyRent: Double? = null,
    val estimatedHouseholdExpenses: Double? = null,
    val numberOfDependents: Int = 0,

    // Existing obligations
    val existingMonthlyEmi: Double = 0.0,
    val existingHighCostDebt: Double? = null,            // total outstanding (e.g. app loans)
    val existingHighCostRate: Double? = null,            // effective rate on those loans
    val appLoanCount: Int = 0,
    val recentEmiBounce: Boolean = false,

    // Credit
    val creditScoreStatus: CreditScoreStatus = CreditScoreStatus.UNKNOWN,
    val creditScore: Int? = null,                        // null = unknown
    val creditUtilisationPercent: Double? = null,

    // Assets / collateral
    val collateralValueLakh: Double? = null,
    val collateralDescription: String? = null,
    val isCollateralUnencumbered: Boolean = false,

    // Financial buffer
    val emergencySavingsMonths: Double? = null,
    val hasUpcomingLargeExpense: Boolean = false,
    val upcomingExpenseAmount: Double? = null,

    // Existing lender offers
    val existingLenderOfferRate: Double? = null
)

data class LoanRequest(
    val purpose: LoanPurpose,
    val loanType: LoanType,
    val requestedAmountRs: Double,
    val preferredTenureMonths: Int = 36,
    val isProductivePurpose: Boolean = false             // generates income / asset
)

/** Represents an existing debt obligation */
data class ExistingDebt(
    val lender: String,
    val outstandingRs: Double,
    val monthlyEmi: Double,
    val interestRate: Double,
    val remainingMonths: Int
)

// ─── Assessment Result Models ─────────────────────────────────────────────────

data class MoneyRange(
    val low: Double,
    val high: Double
) {
    fun midpoint() = (low + high) / 2
    fun formatted() = "₹${formatIndian(low)} – ₹${formatIndian(high)}"

    private fun formatIndian(amount: Double): String {
        val lakh = 100_000.0
        val crore = 10_000_000.0
        return when {
            amount >= crore -> "${String.format("%.1f", amount / crore)}Cr"
            amount >= lakh  -> "${String.format("%.1f", amount / lakh)}L"
            else            -> String.format("%,.0f", amount)
        }
    }
}

data class RateRange(
    val lowPercent: Double,
    val highPercent: Double
) {
    fun formatted() = "${String.format("%.1f", lowPercent)}% – ${String.format("%.1f", highPercent)}%"
}

data class AffordabilityResult(
    val maxSafeMonthlyEmi: Double,
    val currentFoir: Double,           // existing EMI / income
    val proposedFoir: Double,          // (existing + proposed) / income
    val availableMonthlyCapacity: Double,
    val safeCapacityUsed: Double,      // how much of safe capacity is used
    val explanation: String
)

data class APRResult(
    val nominalRate: Double,
    val processingFeePercent: Double,
    val processingFeeRs: Double,
    val estimatedAprLow: Double,
    val estimatedAprHigh: Double,
    val explanation: String
)

data class TenureRow(
    val tenureMonths: Int,
    val monthlyEmi: Double,
    val totalInterest: Double,
    val totalPayment: Double
)

data class StressResult(
    val scenario: String,
    val normalEmi: Double,
    val stressEmi: Double,
    val normalRemainingIncome: Double,
    val stressRemainingIncome: Double,
    val outcome: StressOutcome,
    val explanation: String
)

data class RateBand(
    val loanType: LoanType,
    val baseRateLow: Double,
    val baseRateHigh: Double,
    val explanation: String
)

data class SanctionEstimate(
    val lenderLikelyRange: MoneyRange,
    val borrowerSafeRange: MoneyRange,
    val explanation: String,
    val recommendedAmount: Double,
    val routeRecommendation: ProductRoute,
    val routeExplanation: String?
)

data class AssessmentResult(
    // O1 — Verdict
    val verdict: Verdict,
    val verdictExplanation: String,
    val keyReasons: List<String>,
    val confidence: Confidence,
    val confidenceExplanation: String,

    // O2 — Amount
    val sanctionEstimate: SanctionEstimate,

    // O3 — Rate
    val fairRateRange: RateRange,
    val aprResult: APRResult,
    val rateExplanation: String,

    // O4 — EMI
    val recommendedMaxEmi: Double,
    val emiExplanation: String,
    val tenureRows: List<TenureRow>,
    val stressResult: StressResult,
    val affordabilityResult: AffordabilityResult,

    // Metadata
    val missingDataWarnings: List<String>
)

data class NegotiationCard(
    val loanPurpose: LoanPurpose,
    val loanType: LoanType,
    val requestedAmountRs: Double,
    val safeBorrowingRange: MoneyRange,
    val recommendedEmiCeiling: Double,
    val fairInterestRange: RateRange,
    val estimatedAprRange: RateRange,
    val likelyLenderSanction: MoneyRange,
    val topReasons: List<String>,
    val verdict: Verdict,
    val confidence: Confidence
)

// ─── Utility ─────────────────────────────────────────────────────────────────

/** Indian number formatting: 1,00,000 style */
fun formatIndianRupees(amount: Double): String {
    if (amount <= 0) return "₹0"
    val lakh   = 100_000.0
    val crore  = 10_000_000.0
    return when {
        amount >= crore -> "₹${String.format("%.2f", amount / crore)} Cr"
        amount >= lakh  -> "₹${String.format("%.2f", amount / lakh)} L"
        else -> {
            // Indian comma style: last 3 digits, then groups of 2
            val intPart = amount.toLong()
            val str = intPart.toString()
            val formatted = buildString {
                val n = str.length
                if (n <= 3) {
                    append(str)
                } else {
                    append(str.substring(0, n - 3))
                    append(",")
                    append(str.substring(n - 3))
                }
            }
            "₹$formatted"
        }
    }
}

fun formatLakhCrore(amount: Double): String {
    val lakh  = 100_000.0
    val crore = 10_000_000.0
    return when {
        amount >= crore -> "₹${String.format("%.2f", amount / crore)} Cr"
        amount >= lakh  -> "₹${String.format("%.2f", amount / lakh)} L"
        else            -> formatIndianRupees(amount)
    }
}
