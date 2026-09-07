package com.borrowercopilot.domain.questions

/**
 * QuestionBank — all questions defined in one place.
 * The adaptive engine uses this bank + answers to build the question sequence.
 */
object QuestionBank {

    val all: Map<QuestionId, Question> = mapOf(

        // ────────────────────────────────────────────────────────────────────
        // MUST QUESTIONS (always asked)
        // ────────────────────────────────────────────────────────────────────

        QuestionId.LOAN_PURPOSE to Question(
            id = QuestionId.LOAN_PURPOSE,
            text = "What is this loan for?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("WEDDING",           "Wedding / Family event"),
                QuestionOption("HOME_PURCHASE",     "Buying a home"),
                QuestionOption("HOME_RENOVATION",   "Home renovation"),
                QuestionOption("BUSINESS_EXPANSION","Business expansion"),
                QuestionOption("VEHICLE_PURCHASE",  "Buying a vehicle"),
                QuestionOption("EDUCATION",         "Education"),
                QuestionOption("MEDICAL",           "Medical emergency"),
                QuestionOption("DEBT_CONSOLIDATION","Paying off existing loans"),
                QuestionOption("WORKING_CAPITAL",   "Business working capital"),
                QuestionOption("OTHER",             "Something else")
            ),
            whyItMatters = "Purpose affects loan type recommendation and whether borrowing is productive."
        ),

        QuestionId.LOAN_TYPE to Question(
            id = QuestionId.LOAN_TYPE,
            text = "What kind of loan are you considering?",
            subText = "If unsure, we'll suggest the most suitable type.",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("PERSONAL_LOAN",          "Personal loan (unsecured)"),
                QuestionOption("HOME_LOAN",              "Home loan"),
                QuestionOption("LOAN_AGAINST_PROPERTY",  "Loan against property (LAP)"),
                QuestionOption("GOLD_LOAN",              "Gold loan"),
                QuestionOption("TWO_WHEELER_LOAN",       "Two-wheeler / bike loan"),
                QuestionOption("BUSINESS_LOAN",          "Business loan")
            ),
            whyItMatters = "Loan type determines rate band, tenure, and collateral requirements."
        ),

        QuestionId.AMOUNT_WANTED to Question(
            id = QuestionId.AMOUNT_WANTED,
            text = "How much do you want to borrow?",
            type = QuestionType.NUMERIC_INPUT,
            unit = "₹",
            placeholder = "e.g. 500000 for ₹5,00,000",
            whyItMatters = "This is compared to your safe borrowing capacity."
        ),

        QuestionId.NET_MONTHLY_INCOME to Question(
            id = QuestionId.NET_MONTHLY_INCOME,
            text = "What is your net monthly take-home income?",
            subText = "After tax and deductions. If variable, use a typical month.",
            type = QuestionType.NUMERIC_INPUT,
            unit = "₹ / month",
            placeholder = "e.g. 55000",
            whyItMatters = "Your income is the primary input to affordability."
        ),

        QuestionId.INCOME_TYPE to Question(
            id = QuestionId.INCOME_TYPE,
            text = "How do you earn your income?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("SALARIED",      "Salaried (company pays me every month)",
                    "Govt, PSU, private company, MNC"),
                QuestionOption("SELF_EMPLOYED", "Self-employed / business owner",
                    "Shop, freelancer, contractor, professional"),
                QuestionOption("INFORMAL_GIG",  "Informal / gig / daily wage",
                    "Delivery, tailoring, domestic work, daily labour")
            ),
            whyItMatters = "Income type changes how lenders assess your application and which questions follow."
        ),

        QuestionId.EXISTING_EMI to Question(
            id = QuestionId.EXISTING_EMI,
            text = "What are your total existing EMIs per month?",
            subText = "Include all: car loan, personal loan, credit card minimum, app loans. Enter 0 if none.",
            type = QuestionType.NUMERIC_INPUT,
            unit = "₹ / month",
            placeholder = "0 if no existing EMIs",
            whyItMatters = "Existing EMIs directly reduce how much more you can safely repay."
        ),

        QuestionId.HOUSEHOLD_EXPENSES to Question(
            id = QuestionId.HOUSEHOLD_EXPENSES,
            text = "What are your total monthly household expenses?",
            subText = "Rent, groceries, utilities, school fees, transport. Approximate is fine.",
            type = QuestionType.NUMERIC_INPUT,
            unit = "₹ / month",
            placeholder = "e.g. 30000",
            isOptional = true,
            whyItMatters = "Essential expenses reduce available capacity for EMI. If unknown, we estimate."
        ),

        QuestionId.AGE to Question(
            id = QuestionId.AGE,
            text = "How old are you?",
            type = QuestionType.NUMERIC_INPUT,
            unit = "years",
            placeholder = "e.g. 32",
            whyItMatters = "Age affects maximum tenure a lender will offer."
        ),

        QuestionId.CREDIT_SCORE to Question(
            id = QuestionId.CREDIT_SCORE,
            text = "Do you know your credit score (CIBIL / Experian)?",
            subText = "Typical range: 300–900. Select 'Don't know' if unsure — we handle it honestly.",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("UNKNOWN",   "Don't know / never checked"),
                QuestionOption("EXCELLENT", "800 or above — Excellent"),
                QuestionOption("VERY_GOOD", "750–799 — Very Good"),
                QuestionOption("GOOD",      "700–749 — Good"),
                QuestionOption("FAIR",      "600–699 — Fair"),
                QuestionOption("POOR",      "Below 600 — Poor")
            ),
            whyItMatters = "Credit score is the biggest driver of interest rate and lender approval."
        ),

        QuestionId.LOCATION to Question(
            id = QuestionId.LOCATION,
            text = "Which city / town are you in?",
            type = QuestionType.TEXT_INPUT,
            placeholder = "e.g. Bengaluru, Mysuru, Hubballi",
            isOptional = true,
            whyItMatters = "Location provides context for product availability and cost of living."
        ),

        // ────────────────────────────────────────────────────────────────────
        // SALARIED ADAPTIVE QUESTIONS
        // ────────────────────────────────────────────────────────────────────

        QuestionId.EMPLOYMENT_STABILITY to Question(
            id = QuestionId.EMPLOYMENT_STABILITY,
            text = "How stable is your current employment?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("MORE_THAN_5_YEARS",    "5+ years at current employer"),
                QuestionOption("THREE_TO_FIVE_YEARS",  "3–5 years at current employer"),
                QuestionOption("ONE_TO_THREE_YEARS",   "1–3 years at current employer"),
                QuestionOption("LESS_THAN_1_YEAR",     "Less than 1 year")
            ),
            whyItMatters = "Longer tenure lowers your rate and increases lender confidence."
        ),

        QuestionId.VARIABLE_INCOME to Question(
            id = QuestionId.VARIABLE_INCOME,
            text = "Do you receive variable pay (bonus, commission, incentives)?",
            subText = "If yes, roughly what % of your total income is variable?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("NONE",       "No variable pay — all fixed"),
                QuestionOption("LOW",        "Less than 20% variable"),
                QuestionOption("MODERATE",   "20–40% variable"),
                QuestionOption("HIGH",       "More than 40% variable")
            ),
            isOptional = true,
            whyItMatters = "High variable income increases income risk, widening the safe-amount range."
        ),

        QuestionId.CREDIT_UTILISATION to Question(
            id = QuestionId.CREDIT_UTILISATION,
            text = "How much of your credit card / credit limit are you typically using?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("UNKNOWN",  "Don't know / no credit card"),
                QuestionOption("LOW",      "Less than 30% — low utilisation"),
                QuestionOption("MODERATE", "30–60%"),
                QuestionOption("HIGH",     "More than 60% — high utilisation")
            ),
            isOptional = true,
            whyItMatters = "High credit utilisation can lower your score and affect lender's view."
        ),

        QuestionId.EMERGENCY_SAVINGS to Question(
            id = QuestionId.EMERGENCY_SAVINGS,
            text = "How many months of expenses do you have saved as emergency fund?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("UNKNOWN",     "Not sure"),
                QuestionOption("NONE",        "None"),
                QuestionOption("ONE_TO_THREE","1–3 months"),
                QuestionOption("THREE_PLUS",  "3+ months")
            ),
            isOptional = true,
            whyItMatters = "Emergency savings act as a buffer if your income dips — affects stress test."
        ),

        QuestionId.UPCOMING_LARGE_EXPENSE to Question(
            id = QuestionId.UPCOMING_LARGE_EXPENSE,
            text = "Do you have any large planned expenses in the next 12 months?",
            subText = "e.g. another major purchase, school fees, medical, family function",
            type = QuestionType.BOOLEAN,
            whyItMatters = "Upcoming large expenses reduce your capacity to comfortably service a new EMI."
        ),

        QuestionId.EXISTING_LENDER_OFFER to Question(
            id = QuestionId.EXISTING_LENDER_OFFER,
            text = "Do you already have a loan offer from a lender?",
            subText = "If yes, what is the interest rate they quoted?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("NO",          "No offer yet"),
                QuestionOption("BELOW_12",    "Yes — below 12%"),
                QuestionOption("12_TO_16",    "Yes — 12–16%"),
                QuestionOption("ABOVE_16",    "Yes — above 16%")
            ),
            isOptional = true,
            whyItMatters = "Comparing your offer against the fair rate range tells you if it's reasonable."
        ),

        // ────────────────────────────────────────────────────────────────────
        // SELF-EMPLOYED ADAPTIVE QUESTIONS
        // ────────────────────────────────────────────────────────────────────

        QuestionId.BUSINESS_VINTAGE to Question(
            id = QuestionId.BUSINESS_VINTAGE,
            text = "How many years has your business been running?",
            type = QuestionType.NUMERIC_INPUT,
            unit = "years",
            placeholder = "e.g. 14",
            whyItMatters = "Longer business vintage increases lender confidence and supports formal business loans."
        ),

        QuestionId.ITR_INCOME to Question(
            id = QuestionId.ITR_INCOME,
            text = "What income did you declare in your last ITR (tax return)?",
            subText = "Annual figure from ITR. Enter 0 if not filed.",
            type = QuestionType.NUMERIC_INPUT,
            unit = "₹ / year",
            placeholder = "e.g. 420000",
            isOptional = true,
            whyItMatters = "Lenders often use ITR income, not cash income, for underwriting. A gap matters."
        ),

        QuestionId.INCOME_VARIABILITY to Question(
            id = QuestionId.INCOME_VARIABILITY,
            text = "How much does your monthly income vary?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("STABLE",      "Fairly stable (±10%)"),
                QuestionOption("MODERATE",    "Moderately variable (±30%)"),
                QuestionOption("HIGH",        "Highly variable (±50% or more)")
            ),
            whyItMatters = "Variable income widens the safe-amount range and affects the stress test."
        ),

        QuestionId.BUSINESS_ASSETS_COLLATERAL to Question(
            id = QuestionId.BUSINESS_ASSETS_COLLATERAL,
            text = "Do you own any property, shop, or other asset that could be offered as collateral?",
            type = QuestionType.BOOLEAN,
            whyItMatters = "Collateral can unlock a secured loan with much better rates and higher sanction."
        ),

        QuestionId.COLLATERAL_VALUE to Question(
            id = QuestionId.COLLATERAL_VALUE,
            text = "Approximately what is the market value of the property / asset?",
            type = QuestionType.NUMERIC_INPUT,
            unit = "₹ (in lakhs)",
            placeholder = "e.g. 45 for ₹45 lakh",
            whyItMatters = "Collateral value determines the maximum secured loan a lender may offer."
        ),

        QuestionId.COLLATERAL_UNENCUMBERED to Question(
            id = QuestionId.COLLATERAL_UNENCUMBERED,
            text = "Is this property / asset free of any existing loans or mortgage?",
            type = QuestionType.BOOLEAN,
            whyItMatters = "Unencumbered collateral is necessary for a new secured loan."
        ),

        QuestionId.FORMAL_LOANS to Question(
            id = QuestionId.FORMAL_LOANS,
            text = "Do you have any existing formal loans (bank / NBFC)?",
            type = QuestionType.BOOLEAN,
            whyItMatters = "Formal loan history (even without credit score) helps establish credit profile."
        ),

        QuestionId.SPOUSE_INCOME to Question(
            id = QuestionId.SPOUSE_INCOME,
            text = "Does your spouse / co-applicant have a regular income?",
            subText = "If yes, approximately how much per month?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("NONE",     "No / not applicable"),
                QuestionOption("LOW",      "Below ₹15,000"),
                QuestionOption("MODERATE", "₹15,000 – ₹30,000"),
                QuestionOption("HIGH",     "Above ₹30,000")
            ),
            isOptional = true,
            whyItMatters = "Spouse income can support a joint application and improve sanction."
        ),

        QuestionId.BUSINESS_PURPOSE_PRODUCTIVE to Question(
            id = QuestionId.BUSINESS_PURPOSE_PRODUCTIVE,
            text = "Is this loan expected to generate additional income for your business?",
            subText = "e.g. new stock line, vehicle for delivery, equipment",
            type = QuestionType.BOOLEAN,
            whyItMatters = "Productive loans have better repayment logic — improves the overall assessment."
        ),

        // ────────────────────────────────────────────────────────────────────
        // INFORMAL / GIG ADAPTIVE QUESTIONS
        // ────────────────────────────────────────────────────────────────────

        QuestionId.INCOME_VOLATILITY to Question(
            id = QuestionId.INCOME_VOLATILITY,
            text = "How consistent is your income month to month?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("STABLE",   "Usually about the same"),
                QuestionOption("VARIABLE", "Sometimes up, sometimes down"),
                QuestionOption("SEASONAL", "Very seasonal or irregular")
            ),
            whyItMatters = "Volatile income lowers safe borrowing capacity and tightens EMI ceiling."
        ),

        QuestionId.APP_LOANS_COUNT to Question(
            id = QuestionId.APP_LOANS_COUNT,
            text = "How many active app-based loans do you currently have?",
            subText = "e.g. KreditBee, MoneyTap, mPokket, LazyPay, BNPL, etc.",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("NONE",  "None"),
                QuestionOption("ONE",   "1"),
                QuestionOption("TWO",   "2"),
                QuestionOption("THREE", "3"),
                QuestionOption("FOUR_PLUS", "4 or more")
            ),
            whyItMatters = "Multiple app loans signal over-leverage and can trigger a Don't Borrow result."
        ),

        QuestionId.APP_LOAN_BALANCE to Question(
            id = QuestionId.APP_LOAN_BALANCE,
            text = "What is the total outstanding balance on all your app loans?",
            type = QuestionType.NUMERIC_INPUT,
            unit = "₹",
            placeholder = "e.g. 35000",
            whyItMatters = "Outstanding high-cost debt directly reduces your repayment capacity."
        ),

        QuestionId.APP_LOAN_RATE to Question(
            id = QuestionId.APP_LOAN_RATE,
            text = "What interest rate are you paying on these app loans?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("UNKNOWN",    "Don't know"),
                QuestionOption("BELOW_24",   "Below 24% per year"),
                QuestionOption("24_TO_36",   "24–36% per year"),
                QuestionOption("ABOVE_36",   "Above 36% per year — very high")
            ),
            whyItMatters = "High-cost debt at 30%+ should typically be cleared before new borrowing."
        ),

        QuestionId.RECENT_BOUNCE to Question(
            id = QuestionId.RECENT_BOUNCE,
            text = "Have any of your EMIs or loan repayments bounced in the last 3 months?",
            type = QuestionType.BOOLEAN,
            whyItMatters = "A recent bounce is the strongest signal of current repayment stress."
        ),

        QuestionId.HOUSEHOLD_DEPENDENTS to Question(
            id = QuestionId.HOUSEHOLD_DEPENDENTS,
            text = "How many people depend on your income (children, elderly parents, spouse)?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("NONE",  "Just me"),
                QuestionOption("ONE",   "1 person"),
                QuestionOption("TWO",   "2 people"),
                QuestionOption("THREE_PLUS", "3 or more")
            ),
            whyItMatters = "More dependents mean higher essential expenses and lower available EMI capacity."
        ),

        QuestionId.PRODUCTIVE_PURPOSE to Question(
            id = QuestionId.PRODUCTIVE_PURPOSE,
            text = "Will this loan help you earn more money or save significant cost?",
            subText = "e.g. scooter for delivery work, sewing machine for tailoring",
            type = QuestionType.BOOLEAN,
            whyItMatters = "Productive purpose modestly improves the overall case but doesn't override debt stress."
        ),

        QuestionId.INFORMAL_EMERGENCY_SAVINGS to Question(
            id = QuestionId.INFORMAL_EMERGENCY_SAVINGS,
            text = "Do you have any savings set aside for emergencies?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf(
                QuestionOption("NONE",    "No savings"),
                QuestionOption("SMALL",   "Less than 1 month expenses"),
                QuestionOption("SOME",    "1–2 months expenses"),
                QuestionOption("ADEQUATE","3+ months expenses")
            ),
            isOptional = true,
            whyItMatters = "Savings buffer reduces stress-scenario impact."
        )
    )
}
