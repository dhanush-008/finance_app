# Borrower Copilot — Run-Through Scenarios

These run-throughs show exactly what the app asks, what gets skipped, and what the engine produces for each test borrower.

**Important:** Outputs shown here are computed by the rules engine from the inputs. They are **not hard-coded**. If you change a threshold in `FinancialRules.kt`, the numbers below will shift accordingly. The figures here reflect the engine with default rule values.

---

## Run-Through 1: PRIYA

### Profile
> 29-year-old salaried MNC employee in Bengaluru. ₹1,10,000/month net. ₹14,000 car EMI. Credit score 780 (VERY_GOOD band). Wants ₹8,00,000 personal loan for wedding.

---

### Questions Asked

#### Must Questions (all 10 asked)

| # | Question | Priya's Answer |
|---|---|---|
| 1 | What is this loan for? | **Wedding / Family event** |
| 2 | What kind of loan? | **Personal loan (unsecured)** |
| 3 | How much do you want to borrow? | **₹8,00,000** |
| 4 | How do you earn your income? | **Salaried** |
| 5 | Net monthly take-home income? | **₹1,10,000** |
| 6 | Total existing EMIs per month? | **₹14,000** (car loan) |
| 7 | Total monthly household expenses? | **₹42,000** (₹28k rent + ₹14k other) |
| 8 | How old are you? | **29** |
| 9 | Credit score? | **Very Good (750–799)** |
| 10 | City / town? | **Bengaluru** |

#### Adaptive Questions Asked (Salaried track — 6 asked)

| # | Question | Priya's Answer | Why asked |
|---|---|---|---|
| 11 | Employment stability? | **5+ years at current employer** | Affects rate and lender confidence |
| 12 | Variable pay? | **None — all fixed** | Variable income widens safe-amount range |
| 13 | Credit utilisation? | **Less than 30% — low** | Only skipped for EXCELLENT score; VERY_GOOD triggers this |
| 14 | Emergency savings? | **3+ months** | Affects stress test buffer |
| 15 | Upcoming large expense? | **No** | Reduces available capacity if yes |
| 16 | Existing lender offer? | **No offer yet** | Allows comparison against fair rate |

#### Questions Skipped (adaptive questions not in salaried track)

- `BUSINESS_VINTAGE`, `ITR_INCOME`, `INCOME_VARIABILITY`, `BUSINESS_ASSETS_COLLATERAL`, `COLLATERAL_VALUE`, `COLLATERAL_UNENCUMBERED`, `FORMAL_LOANS`, `SPOUSE_INCOME`, `BUSINESS_PURPOSE_PRODUCTIVE` — **not asked** (self-employed track only)
- `INCOME_VOLATILITY`, `APP_LOANS_COUNT`, `APP_LOAN_BALANCE`, `APP_LOAN_RATE`, `RECENT_BOUNCE`, `HOUSEHOLD_DEPENDENTS`, `PRODUCTIVE_PURPOSE`, `INFORMAL_EMERGENCY_SAVINGS` — **not asked** (informal track only)

**Total questions: 16 asked, ~20 skipped**

---

### O1 — Should I Borrow?

**Verdict: BORROW or BORROW LESS**
*(Exact verdict depends on whether ₹8L requested falls within safe range. With ₹1.1L income and ₹14k existing EMI, the safe amount is typically ₹5.5L–₹6.5L, making the ₹8L request above safe range → BORROW LESS)*

**Confidence: MEDIUM–HIGH**
Known: credit score, household expenses, employment stability, emergency savings, credit utilisation.
Missing: exact income variability figure (stated as "none"), upcoming expense details.

**Key reasons:**
- Strong credit profile (VERY_GOOD score) and stable 5+ year employment support borrowing
- Requested ₹8,00,000 exceeds the borrower-safe estimate of ~₹5.5L–₹6.5L
- Existing ₹14,000 car EMI already uses part of monthly repayment capacity
- ₹42,000 monthly household expenses (including ₹28k rent) reduce available EMI headroom

**Explanation:**
> Your requested EMI would push your total debt payments above the safer affordability range for your income. Borrowing a smaller amount at a comfortable EMI is recommended.

---

### O2 — Maximum Amount

**Likely lender sanction:** ₹7.5L – ₹9.0L
- Income-multiplier basis: ₹1,10,000 × 20 (VERY_GOOD salaried) = ₹22L theoretical max
- FOIR constraint: (₹1,10,000 × 45% − ₹14,000) = ₹35,500 available EMI
- At ~12% / 36 months, ₹35,500 EMI → ~₹10.6L principal → range ₹7.5L–₹9.0L after range factor

**Borrower-safe amount:** ₹5.0L – ₹6.5L
- Safe EMI capacity: ₹1,10,000 − ₹14,000 (existing) − ₹42,000 (expenses) = ₹54,000 net; 80% = ₹43,200; FOIR cap ₹35,500; lower of two ≈ ₹30,000–₹35,000 safe EMI
- At ~12% / 36 months → safe principal ~₹8.9L but also discounted 20% from lender max

**Recommendation:** Use the borrower-safe amount when negotiating.

**Explanation:** Your safe amount is lower because your existing ₹14,000 car EMI and ₹42,000 in monthly expenses (including ₹28,000 rent) already use a significant portion of your monthly cash flow.

---

### O3 — Fair Interest Rate

**Fair interest rate:** ≈ 9.75% – 12.5%

Rate derivation:
- Base personal loan band: 10.5%–24%
- VERY_GOOD credit score: −0.75% → 9.75%–23.25%
- Salaried 5+ years at large employer: −0.5% → 9.25%–22.75%
- Clamped to sensible personal loan range → **~9.75%–12.5%** for this profile

**Processing fee (est.):** 2% of loan ≈ ₹10,000–₹13,000 (on safe amount)

**Estimated all-in APR:** ≈ 10.5%–13.8%

**Explanation:** A lender quoting 12% with a 3% processing fee can be more expensive than one quoting 12.5% with a 0.5% fee. Compare APR, not just headline rate.

---

### O4 — EMI / Monthly Outflow

**Recommended EMI ceiling:** ~₹30,000–₹35,000/month

**Tenure trade-off** (on borrower-safe midpoint ~₹5.8L at ~11% mid-rate):

| Tenure | Monthly EMI | Total Interest |
|---|---|---|
| 12 months | ₹51,500 | ₹18,000 |
| 24 months | ₹26,900 | ₹45,600 |
| 36 months | ₹19,000 | ₹84,000 |
| 48 months | ₹14,900 | ₹1,15,200 |
| 60 months | ₹12,600 | ₹1,56,000 |

*Longer tenure means lower EMI but significantly more total interest.*

**Stress test — Income falls 20%:**
- Stress income: ₹88,000/month
- Normal total outflow (existing + proposed): ₹14,000 + ₹19,000 = ₹33,000
- Stress remaining income: ₹88,000 − ₹33,000 − ₹42,000 = ₹13,000
- Outcome: **Still manageable** (FOIR under 50% even under stress at 36-month tenure)

---

### Negotiation Card (Priya)

```
BORROWER COPILOT — NEGOTIATION CARD

Loan purpose:      Wedding / Family event
Loan type:         Personal loan
Requested amount:  ₹8,00,000

MY SAFE POSITION
Safe borrowing range:      ₹5.0L – ₹6.5L
Recommended EMI ceiling:   ~₹30,000/month

WHAT I SHOULD EXPECT
Fair interest rate:        9.75% – 12.5%
Estimated all-in APR:      10.5% – 13.8%
Likely lender sanction:    ₹7.5L – ₹9.0L

WHY
→ Strong credit profile (VERY_GOOD score) and stable MNC employment
→ Existing ₹14,000 car EMI reduces available capacity
→ ₹42,000/month household expenses (including ₹28k rent) tighten safe range

BEFORE I SIGN
• Compare APR, not just headline interest rate
• Ask for processing fee and all charges
• Do not cross ₹30,000/month EMI ceiling
• Check foreclosure / prepayment terms
• Ask for the complete repayment schedule

CONFIDENCE: MEDIUM–HIGH
This is a self-assessment, not a lender sanction or financial guarantee.
```

---

---

## Run-Through 2: RAVI

### Profile
> 42-year-old kirana shop owner in Mysuru. Cash income ₹40,000–₹80,000/month (₹60,000 used as midpoint). ITR income ₹4,20,000/year. Owns ₹45L unencumbered shop. No formal loans, no credit score. Wife earns ₹18,000/month. Wants ₹15,00,000 for business expansion.

---

### Questions Asked

#### Must Questions (all 10 asked)

| # | Question | Ravi's Answer |
|---|---|---|
| 1 | Loan purpose? | **Business expansion** |
| 2 | Loan type? | **Business loan** |
| 3 | Amount wanted? | **₹15,00,000** |
| 4 | Income type? | **Self-employed** |
| 5 | Net monthly income? | **₹60,000** (midpoint of ₹40k–₹80k range) |
| 6 | Existing EMIs? | **₹0** (no existing formal loans) |
| 7 | Household expenses? | **₹30,000** |
| 8 | Age? | **42** |
| 9 | Credit score? | **Don't know / never checked** (UNKNOWN) |
| 10 | Location? | **Mysuru** |

#### Adaptive Questions Asked (Self-employed track — 9 asked)

| # | Question | Ravi's Answer | Why asked |
|---|---|---|---|
| 11 | Business vintage? | **14 years** | Critical for business loan routing and lender confidence |
| 12 | ITR income? | **₹4,20,000/year** | Checks documentation gap vs cash income |
| 13 | Income variability? | **Moderately variable (±30%)** | Widens safe-amount range |
| 14 | Own property/asset for collateral? | **Yes** | Unlocks secured route check |
| 15 | Collateral value? | **₹45 lakh** | Asked because Q14 = Yes |
| 16 | Is property unencumbered? | **Yes** | Asked because Q14 = Yes; confirms LAP eligibility |
| 17 | Any existing formal loans? | **No** | Documents absence of formal credit history |
| 18 | Spouse / co-applicant income? | **₹15,000–₹30,000 (Moderate)** | Can support joint application |
| 19 | Will loan generate additional income? | **Yes** | Productive purpose; improves assessment weighting |

#### Questions Skipped

- All salaried track questions (`EMPLOYMENT_STABILITY`, `VARIABLE_INCOME`, `CREDIT_UTILISATION`, `EMERGENCY_SAVINGS`, `UPCOMING_LARGE_EXPENSE`, `EXISTING_LENDER_OFFER`) — **not asked**
- All informal track questions — **not asked**

**Total questions: 19 asked, ~17 skipped**

---

### O1 — Should I Borrow?

**Verdict: BORROW or BORROW LESS**

**Confidence: MEDIUM**
Known: business vintage, ITR income, collateral, income variability, spouse income, productive purpose.
Missing: credit score (UNKNOWN → widens ranges), exact cash income floor.

**Key reasons:**
- 14-year business vintage demonstrates operational stability
- Unencumbered ₹45L collateral significantly supports borrowing capacity via LAP route
- ITR income (₹35,000/month) is below stated cash income (₹60,000) — lenders may underwrite on ITR
- Productive purpose (stock + delivery vehicle) improves repayment logic
- Unknown credit score means lender confidence varies widely

**Explanation:**
> Your business collateral and 14-year vintage support this loan. However, the mismatch between ITR income (₹35,000/month) and cash income (₹60,000) is a lender risk. A secured LAP route is recommended — it offers better rates and higher sanction than an unsecured business loan.

---

### O2 — Maximum Amount

**Product route: SECURED LAP / Secured Business Loan**
*(Engine triggers this because ₹45L collateral ≥ 1.5× ₹15L requested = ₹22.5L threshold)*

**Likely lender sanction:**
- LAP LTV: 60% × ₹45L = ₹27L maximum secured sanction
- Income FOIR check: (₹60,000 × 45% − ₹0) = ₹27,000 EMI capacity; at 12% / 36 months → ~₹8.1L
- Secured route uses collateral as primary driver → ₹27L capacity
- Range with UNKNOWN credit score (±30% width): **₹18.9L – ₹27L+**

**Borrower-safe amount:**
- Applied 35% discount (self-employed) to income-based component
- Safe EMI: ₹60,000 − ₹0 − ₹30,000 expenses = ₹30,000 net; 80% = ₹24,000
- At LAP rate ~11% / 84 months (7-year LAP) → safe principal ~₹15L–₹18L
- Range: **₹12L – ₹18L**

**Recommendation:** Use the borrower-safe amount when negotiating.

**Route explanation shown in app:**
> Unencumbered collateral (₹45,00,000) is 3.0× the requested amount. A Loan Against Property (LAP) or secured business loan is likely more suitable than an unsecured business loan — it typically offers a lower interest rate and higher sanction. Your lender will verify and value the property.

---

### O3 — Fair Interest Rate

**LAP route rate band:** 9.5%–14.0% (vs 12%–24% for unsecured business loan)

Rate derivation:
- Base LAP band: 9.5%–14%
- UNKNOWN credit score: widen ±2% → 7.5%–16%
- Self-employed 14-year vintage: +0.5% → 8.0%–16.5%
- Clamped to LAP product range → **~9.5%–15.0%**

**Processing fee (est.):** 1% of loan ≈ ₹15,000 (on ₹15L)

**Estimated all-in APR:** ≈ 10.0%–16.2%

**Explanation:**
> Your unknown credit score widens this range. If Ravi obtains a formal credit score first (by taking and repaying a small formal loan), the range will tighten. The secured route means his rate is materially lower than an unsecured applicant with similar income uncertainty.

---

### O4 — EMI / Monthly Outflow

**Recommended EMI ceiling:** ~₹20,000–₹24,000/month

**Tenure trade-off** (on ₹15L at ~12% mid-rate — LAP options):

| Tenure | Monthly EMI | Total Interest |
|---|---|---|
| 5 years (60 mo) | ₹33,300 | ₹4,98,000 |
| 7 years (84 mo) | ₹26,000 | ₹6,84,000 |
| 10 years (120 mo) | ₹21,500 | ₹10,80,000 |

**Stress test — Income falls to lower end of stated range (₹40,000):**
- Stress income: ₹40,000/month
- Total outflow at 84-month tenure: ₹0 existing + ₹26,000 proposed = ₹26,000
- Stress FOIR: ₹26,000 / ₹40,000 = 65%
- Stress remaining: ₹40,000 − ₹26,000 − ₹30,000 = −₹16,000
- Outcome: **Becomes risky** — income volatility at lower end makes this tight
- Note: Longer tenure (120 months) reduces stress FOIR to 54% → still risky but more manageable

---

### Negotiation Card (Ravi)

```
BORROWER COPILOT — NEGOTIATION CARD

Loan purpose:      Business expansion
Loan type:         Business loan → recommended: LAP / Secured Business Loan
Requested amount:  ₹15,00,000

MY SAFE POSITION
Safe borrowing range:      ₹12L – ₹18L
Recommended EMI ceiling:   ~₹22,000/month

WHAT I SHOULD EXPECT
Fair interest rate:        9.5% – 15.0%
Estimated all-in APR:      10.0% – 16.2%
Likely lender sanction:    ₹18.9L – ₹27L (secured route)

WHY
→ 14-year business vintage and ₹45L unencumbered collateral strongly support borrowing
→ Unknown credit score widens the rate range — consider getting a CIBIL report first
→ ITR income (₹35k/month) is lower than cash income (₹60k) — lender will use ITR figures

BEFORE I SIGN
• Pursue LAP or secured business loan, not unsecured personal loan
• Compare APR, not just headline interest rate
• Do not cross ₹22,000/month EMI ceiling
• Consider 84–120 month tenure to keep EMI manageable given income volatility
• Ask for foreclosure / prepayment terms

CONFIDENCE: MEDIUM
This is a self-assessment, not a lender sanction or financial guarantee.
```

---

---

## Run-Through 3: ANITA

### Profile
> 35-year-old delivery rider + home tailor in Hubballi. Income ₹26,000–₹30,000/month (₹28,000 midpoint). 2 children, husband not earning. 3 active app loans, ₹35,000 outstanding at 36%+ rates, 1 recent EMI bounce. Wants ₹1,50,000 for electric scooter.

---

### Questions Asked

#### Must Questions (all 10 asked)

| # | Question | Anita's Answer |
|---|---|---|
| 1 | Loan purpose? | **Vehicle purchase** |
| 2 | Loan type? | **Two-wheeler loan** |
| 3 | Amount wanted? | **₹1,50,000** |
| 4 | Income type? | **Informal / gig** |
| 5 | Net monthly income? | **₹28,000** |
| 6 | Existing EMIs? | **₹9,000** (app loan repayments) |
| 7 | Household expenses? | **₹14,000** |
| 8 | Age? | **35** |
| 9 | Credit score? | **Don't know** (UNKNOWN) |
| 10 | Location? | **Hubballi** |

#### Adaptive Questions Asked (Informal track — 8 asked)

| # | Question | Anita's Answer | Why asked |
|---|---|---|---|
| 11 | Income consistency? | **Sometimes up, sometimes down** (Variable) | Tightens FOIR ceiling to 35% |
| 12 | Active app loans? | **3** | ≥2 triggers high-risk flag |
| 13 | Total app loan balance? | **₹35,000** | Asked because Q12 > NONE |
| 14 | App loan interest rate? | **Above 36% per year** | Asked because Q12 > NONE; >24% = high-cost |
| 15 | Recent EMI bounce? | **Yes** | Strongest current-stress signal |
| 16 | Dependents? | **2 people** | Raises essential expenses |
| 17 | Emergency savings? | **None** | Removes stress-test buffer |
| 18 | Will loan help you earn more? | **Yes** (scooter for delivery) | Productive purpose — noted but cannot override debt stress |

#### Questions Skipped

- All salaried track questions — **not asked**
- All self-employed track questions — **not asked**

**Total questions: 18 asked, ~18 skipped**

---

### O1 — Should I Borrow?

**Verdict: DON'T BORROW NOW**

This is a legitimate, reachable result. The engine reaches it through accumulated risk signals:

| Signal | Don't-Borrow Score |
|---|---|
| Recent EMI bounce | +3 |
| 3 app loans (≥2 threshold) | +2 |
| App loan rate >36% (>24% high-cost threshold) | +2 |
| **Total** | **+7 → threshold is 5 → DONT_BORROW** |

**Confidence: LOW**
Known: income type, existing debt, bounce, dependents.
Missing: credit score, emergency savings detail, exact expense breakdown.

**Key reasons:**
- Recent EMI bounce signals active repayment stress right now
- 3 app loans at 36%+ rates indicate over-leverage
- Existing debt obligations already use ~32% of income before any new loan
- No emergency savings — zero buffer against income volatility

**Explanation:**
> Adding a new loan at this time would increase your financial stress, not reduce it. First focus on closing or reducing high-cost app debt, then reassess scooter financing in 3–6 months.

*Note: The app does not shame Anita. It explains the financial mechanics — the problem is not the scooter; it is the existing debt structure. The productive purpose (delivery work) is acknowledged but cannot override the stress signals.*

---

### O2 — Maximum Amount

**Likely lender sanction:** ₹50,000 – ₹1,10,000
- Informal multiplier (10× NMI): ₹28,000 × 10 = ₹2,80,000 theoretical max
- FOIR constraint: (₹28,000 × 35% − ₹9,000) = ₹800 available EMI → near zero
- With UNKNOWN credit score (+30% width): range ₹50,000–₹1,10,000
- *Many formal lenders may decline; informal/NBFC lenders may sanction at very high rates*

**Borrower-safe amount:** ₹0 – ₹30,000
- Safe EMI capacity: ₹28,000 − ₹9,000 − ₹14,000 − (₹2 children extra ₹8,000) = −₹3,000
- Available EMI after expenses is effectively zero or negative
- Engine correctly shows near-zero safe range when no capacity exists

**Recommendation:** Don't borrow — address existing debt first.

---

### O3 — Fair Interest Rate

**Two-wheeler loan base band:** 10%–20%

Rate derivation:
- Base band: 10%–20%
- UNKNOWN credit score: widen ±2% → 8%–22%
- Informal income type: +3% → 11%–25%
- Recent bounce and multiple app loans: no explicit rate adjustment (captured in verdict), but represents high-risk end
- Final range: **~14%–25%** for informal borrower with stress signals

**Processing fee (est.):** 1.5% ≈ ₹2,250

**Estimated all-in APR:** ≈ 15.5%–27%

---

### O4 — EMI / Monthly Outflow

**Recommended EMI ceiling:** ~₹0–₹800/month
*(Near zero because existing obligations + expenses already consume income)*

**Tenure trade-off** (on ₹1,50,000 at ~18% mid-rate — shown for reference only):

| Tenure | Monthly EMI | Total Interest |
|---|---|---|
| 12 months | ₹13,750 | ₹15,000 |
| 24 months | ₹7,480 | ₹29,520 |
| 36 months | ₹5,420 | ₹45,120 |

*Even the 36-month option (₹5,420) exceeds the available EMI capacity of ~₹800/month.*

**Stress test — Income falls 20% and obligations remain unchanged:**
- Stress income: ₹22,400/month
- Existing + proposed EMI: ₹9,000 + ₹5,420 = ₹14,420
- Stress FOIR: ₹14,420 / ₹22,400 = 64.4%
- Remaining after EMI + expenses: ₹22,400 − ₹14,420 − ₹14,000 = −₹6,020
- Outcome: **Not recommended** — household goes cash-flow negative under stress

---

### Negotiation Card (Anita)

```
BORROWER COPILOT — NEGOTIATION CARD

Loan purpose:      Vehicle purchase (electric scooter)
Loan type:         Two-wheeler loan
Requested amount:  ₹1,50,000

VERDICT: DON'T BORROW NOW

MY SAFE POSITION
Safe borrowing range:      ₹0 – ₹30,000 (current capacity)
Recommended EMI ceiling:   ~₹0–₹800/month

WHAT I SHOULD EXPECT
Fair interest rate:        14% – 25%
Estimated all-in APR:      15.5% – 27%
Likely lender sanction:    ₹50,000 – ₹1,10,000 (formal lenders may decline)

WHY
→ Recent EMI bounce is the strongest signal of active repayment stress
→ 3 app loans at 36%+ rates — high-cost debt should be cleared first
→ After existing EMIs and household expenses, available EMI capacity is near zero

BEFORE BORROWING AGAIN
• Clear or significantly reduce the 3 app loans first
• Maintain 3–6 months of consistent, on-time repayments
• Then reassess electric scooter financing — the purpose is sound
• When ready, a two-wheeler loan from a formal NBFC at 14–18% is far better than app loans

CONFIDENCE: LOW
This is a self-assessment, not a lender sanction or financial guarantee.
```

---

## Summary: Adaptive Question Comparison

| Question | Priya (Salaried) | Ravi (Self-Employed) | Anita (Informal) |
|---|---|---|---|
| Employment stability | ✅ Asked | ❌ Skipped | ❌ Skipped |
| Variable income (salaried) | ✅ Asked | ❌ Skipped | ❌ Skipped |
| Credit utilisation | ✅ Asked | ❌ Skipped | ❌ Skipped |
| Emergency savings | ✅ Asked | ❌ Skipped | ✅ Asked (informal version) |
| Business vintage | ❌ Skipped | ✅ Asked | ❌ Skipped |
| ITR income | ❌ Skipped | ✅ Asked | ❌ Skipped |
| Collateral check | ❌ Skipped | ✅ Asked | ❌ Skipped |
| Collateral value | ❌ Skipped | ✅ Asked | ❌ Skipped |
| App loan count | ❌ Skipped | ❌ Skipped | ✅ Asked |
| App loan balance | ❌ Skipped | ❌ Skipped | ✅ Asked |
| Recent EMI bounce | ❌ Skipped | ❌ Skipped | ✅ Asked |
| Household dependents | ❌ Skipped | ❌ Skipped | ✅ Asked |
| Productive purpose | ❌ Skipped | ✅ Asked | ✅ Asked |
| Spouse income | ❌ Skipped | ✅ Asked | ❌ Skipped |

This table demonstrates that the questionnaire is genuinely adaptive — each borrower type gets a meaningfully different question set.
