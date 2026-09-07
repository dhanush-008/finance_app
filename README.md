# Borrower Copilot

An Android app that helps Indian borrowers understand their position before taking a loan.

Built with Kotlin + Jetpack Compose. No backend. No login. No data stored. Works offline.

---

## What It Does

Answers four questions every borrower should know before signing:

1. **Should I borrow at all?** — Returns BORROW, BORROW LESS, or DON'T BORROW NOW
2. **How much am I eligible for?** — Shows lender-likely range vs borrower-safe range separately
3. **What is a fair interest rate?** — Shows rate range + estimated all-in APR including processing fees
4. **What EMI should I agree to?** — Shows EMI ceiling, tenure trade-off table, and a stress scenario

Produces a **Negotiation Card** the borrower can screenshot or reference with a lender.

---

## How to Run (Under 5 Minutes)

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 35
- JDK 11 or later
- An Android device or emulator running API 26+

### Steps

```bash
# 1. Open Android Studio
# File → Open → select the BorrowerCopilot folder

# 2. Let Gradle sync complete (first sync downloads dependencies ~2 min)

# 3. Run on emulator or device
# Press the green Run button (Shift+F10)
# Select a device with API 26+
```

**That's it.** No environment variables. No API keys. No backend setup.

If Gradle sync fails, try: File → Invalidate Caches → Invalidate and Restart.

### Run Unit Tests

In Android Studio:
```
Right-click app/src/test/java/com/borrowercopilot/engine/AssessmentEngineTest.kt
→ Run 'AssessmentEngineTest'
```

Or via terminal (requires Gradle wrapper):
```bash
./gradlew test
```

On Windows PowerShell:
```powershell
.\gradlew test
```

---

## Project Structure

```
BorrowerCopilot/
├── app/src/main/java/com/borrowercopilot/
│   ├── MainActivity.kt                        # Entry point, screen router
│   ├── domain/
│   │   ├── model/
│   │   │   └── Models.kt                      # All domain data classes and enums
│   │   ├── rules/
│   │   │   └── FinancialRules.kt              # ALL financial thresholds — single source of truth
│   │   ├── engine/
│   │   │   └── BorrowerAssessmentEngine.kt    # Core calculation engine
│   │   └── questions/
│   │       ├── QuestionModels.kt              # Question and answer data classes
│   │       ├── QuestionBank.kt                # All question definitions
│   │       ├── AdaptiveQuestionEngine.kt      # Sequence builder + profile converter
│   │       └── BorrowerFixtures.kt            # Test inputs for Priya, Ravi, Anita
│   └── ui/
│       ├── theme/
│       │   └── Theme.kt                       # Colour palette, typography
│       ├── components/
│       │   └── SharedComponents.kt            # Reusable Compose components
│       ├── viewmodel/
│       │   └── AssessmentViewModel.kt         # State machine, answer management
│       └── screens/
│           ├── WelcomeScreen.kt
│           ├── QuestionnaireScreen.kt
│           ├── ReviewScreen.kt
│           ├── ResultsScreen.kt
│           └── NegotiationCardScreen.kt
├── app/src/test/java/com/borrowercopilot/
│   └── engine/
│       └── AssessmentEngineTest.kt            # 30+ unit tests
├── RULES.md                                   # Every rule, threshold and assumption documented
├── RUNTHROUGHS.md                             # Priya / Ravi / Anita question-by-question walkthrough
├── WALKTHROUGH.md                             # 5-minute presentation script
└── README.md                                  # This file
```

---

## Architecture

### Core Principle

**Rules are separated from UI.** All financial thresholds live in `FinancialRules.kt`. No magic numbers exist in screens or ViewModel.

To change the FOIR threshold: edit `FinancialRules.FoirThresholds.CONSERVATIVE_MAX`.
Every calculation and unit test automatically updates.

### Calculation Flow

```
QuestionAnswers
    ↓
AdaptiveQuestionEngine.buildProfile()  →  BorrowerProfile
AdaptiveQuestionEngine.buildLoanRequest()  →  LoanRequest
    ↓
BorrowerAssessmentEngine.calculateAssessment(profile, request)
    ↓
AssessmentResult {
    verdict, confidence,
    sanctionEstimate (lenderLikely + borrowerSafe),
    fairRateRange,
    aprResult,
    tenureRows,
    stressResult,
    affordabilityResult
}
    ↓
BorrowerAssessmentEngine.buildNegotiationCard(result, request)
    ↓
NegotiationCard
```

### State Management

`AssessmentViewModel` holds a `StateFlow<AssessmentState>` with a simple screen enum:

```
WELCOME → QUESTIONNAIRE → REVIEW → RESULTS → NEGOTIATION_CARD
```

No navigation library needed — the screen enum drives `when` dispatch in `MainActivity`.

---

## How Rules Are Separated from UI

`FinancialRules.kt` is a Kotlin `object` containing only constants, nested objects, and two pure functions (`calculateEmi`, `estimateApr`). It has zero Android imports.

`BorrowerAssessmentEngine` references `FinancialRules` for every threshold. It has zero Compose or UI imports.

You can run the engine and all unit tests without the Android runtime — they are pure JVM code.

---

## How to Test the Three Borrowers

Each borrower's inputs are defined as a `Map<QuestionId, QuestionAnswer>` in `BorrowerFixtures.kt`. These are inputs only — the outputs are computed by the engine.

**In the app:**
1. Launch the app
2. Tap Start Assessment
3. Enter answers matching the borrower profile (see RUNTHROUGHS.md for exact answers)
4. Tap Calculate to see results

**Via unit tests:**
The `AssessmentEngineTest` runs all three fixtures programmatically and asserts expected behaviours.

---

## Important Financial Rules (Quick Reference)

| Rule | Value | File |
|---|---|---|
| Salaried FOIR ceiling (safe) | 40% | `FinancialRules.FoirThresholds.CONSERVATIVE_MAX` |
| Informal FOIR ceiling | 35% | `FinancialRules.FoirThresholds.INFORMAL_MAX` |
| DONT_BORROW FOIR override | 65% | `FinancialRules.FoirThresholds.ABSOLUTE_MAX` |
| Personal loan rate band | 10.5%–24% | `FinancialRules.BASE_RATE_BANDS` |
| LAP rate band | 9.5%–14% | `FinancialRules.BASE_RATE_BANDS` |
| UNKNOWN credit score treatment | Widen range ±2% | `FinancialRules.CreditScoreRateAdj.UNKNOWN_WIDEN` |
| LAP LTV maximum | 60% | `FinancialRules.CollateralAdj.LAP_MAX_LTV` |
| LAP routing trigger | Collateral ≥ 1.5× requested | `FinancialRules.ProductRouting.LAP_COLLATERAL_MULTIPLE` |
| Salaried income stress | −20% | `FinancialRules.StressTest.SALARIED_INCOME_DROP_PCT` |
| Recent bounce window | 3 months | `FinancialRules.HighCostDebtRules.BOUNCE_RECENT_MONTHS` |
| High confidence threshold | Score ≥ 60 pts | `FinancialRules.ConfidenceScoring.HIGH_THRESHOLD` |

See `RULES.md` for the complete table with rationale and sources.

---

## Limitations and Assumptions

**What this is:**
- A deterministic self-assessment tool with transparent rules
- An indicative guide to help a borrower understand their position

**What this is not:**
- A credit decision or lender approval
- A connection to any real lender system
- A legally precise APR calculator
- Connected to any credit bureau

**Key assumptions:**
- Rate bands are based on public market observation as of 2024 and labelled as such
- APR calculation is an iterative approximation, not a legally precise figure
- Lender sanction uses a simplified income-multiplier model — actual lender underwriting varies
- Household expenses are estimated when not stated (see RULES.md Section 2)
- All assumptions are documented in `FinancialRules.kt` and `RULES.md`

---

## What Would Be Built Next

- Live lender offer comparison: input a specific rate and compare against fair range
- Debt consolidation path for over-leveraged borrowers (Anita scenario)
- Full amortisation schedule view
- Dark mode (theme is structured for easy addition)
- Regional rate adjustments (metro vs tier-2/3)
- Accessibility audit with screen reader testing
- Offer to explain the difference between lender types (PSB vs NBFC vs MFI)

---

## Build Configuration

| Item | Version |
|---|---|
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Gradle | 8.9 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Java | 11 |

---

## Files to Review Before the Interview

In priority order:

1. `FinancialRules.kt` — every threshold; this is what the evaluator will ask you to change live
2. `BorrowerAssessmentEngine.kt` — core calculation; understand the verdict/sanction/rate/APR flow
3. `AdaptiveQuestionEngine.kt` — question sequencing and profile builder
4. `AssessmentEngineTest.kt` — 30+ tests; know which test covers which scenario
5. `RULES.md` — be ready to explain any number in the table
6. `RUNTHROUGHS.md` — walk through Priya/Ravi/Anita question by question
7. `BorrowerFixtures.kt` — confirm inputs are not outputs
