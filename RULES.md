# Borrower Copilot — Rules Reference

Every financial threshold, rate band, and assumption used by the engine is documented here.
The single source of truth in code is `FinancialRules.kt`. Change a value there and it propagates automatically.

**Legend for Source column:**
- `MY JUDGEMENT` — reasoned assumption; not derived from a specific RBI circular or lender policy
- `RBI/PSB GUIDANCE` — broadly consistent with publicly stated RBI or public-sector bank policy
- `MARKET OBSERVATION` — based on publicly available lender rate cards (indicative, not live quotes)

---

## 1. FOIR Thresholds (Fixed Obligations to Income Ratio)

> FOIR = (existing EMI + proposed EMI) / net monthly income

| What | Value | Why | Source |
|---|---|---|---|
| Conservative FOIR ceiling — salaried, stable, no dependents | 40% | Leaves adequate buffer for emergencies and discretionary spend | MY JUDGEMENT, consistent with RBI guidance |
| Moderate FOIR ceiling — salaried with dependents or moderate stability | 45% | Slightly relaxed for borrowers with stable salary but some obligations | MY JUDGEMENT |
| High-risk FOIR ceiling — self-employed with strong collateral or very high income | 55% | Collateral mitigates repayment risk; higher ceiling is defensible | MY JUDGEMENT |
| Informal/gig FOIR ceiling | 35% | Volatile income demands tighter ceiling to absorb income dips | MY JUDGEMENT |
| Absolute FOIR maximum — triggers DONT_BORROW regardless of other factors | 65% | Beyond this level, financial distress risk is considered too high | MY JUDGEMENT |

**Important:** The engine uses the *lower* of the FOIR-based max EMI and the cash-flow-based max EMI (income − existing EMI − essential expenses × 80%). This prevents FOIR from appearing generous when actual cash flow is tight.

---

## 2. Household Expense Estimates

Used only when borrower does not state expenses. If stated, actual figure is used.

| What | Value | Why | Source |
|---|---|---|---|
| Urban household minimum per person per month | ₹8,000 | Approximate urban India basic living cost | MY JUDGEMENT |
| Rural household minimum per person per month | ₹5,000 | Lower cost of living outside metros | MY JUDGEMENT |
| Extra per dependent child per month | ₹4,000 | School, health, food incremental cost | MY JUDGEMENT |

---

## 3. Credit Score Bands

| Band | Score Range | Effect |
|---|---|---|
| POOR | < 600 | High rate adjustment (+4%), low lender multiplier |
| FAIR | 600–699 | Moderate rate adjustment (+2%) |
| GOOD | 700–749 | No adjustment from base |
| VERY_GOOD | 750–799 | Rate reduction (−0.75%) |
| EXCELLENT | 800+ | Rate reduction (−1.5%), highest lender multiplier |
| **UNKNOWN** | **Not stated** | **Range widened by ±2% on both ends; never treated as POOR or 300** |

> **Critical rule:** Unknown credit score is represented as `CreditScoreStatus.UNKNOWN`. It is never defaulted to 300 or mapped to POOR. Unknown widens the range — it does not penalise the borrower with a poor-score assumption.

Source: MY JUDGEMENT for band boundaries; CIBIL scoring framework used as general reference.

---

## 4. Credit Score Rate Adjustments

Applied as delta to the base rate band.

| Credit Status | Low-end Adj | High-end Adj | Source |
|---|---|---|---|
| EXCELLENT | −1.5% | −1.5% | MY JUDGEMENT |
| VERY_GOOD | −0.75% | −0.75% | MY JUDGEMENT |
| GOOD | 0% | 0% | MY JUDGEMENT |
| FAIR | +2.0% | +2.0% | MY JUDGEMENT |
| POOR | +4.0% | +4.0% | MY JUDGEMENT |
| UNKNOWN | −2.0% (widen low) | +2.0% (widen high) | MY JUDGEMENT |

---

## 5. Income Stability Rate Adjustments

| Income Type / Situation | Adjustment | Source |
|---|---|---|
| Salaried, large company, 5+ years | −0.5% | MY JUDGEMENT |
| Salaried, stable (3–5 years) | 0% | MY JUDGEMENT |
| Salaried, new (<1 year) | +1.0% | MY JUDGEMENT |
| Self-employed, 5+ years vintage | +0.5% | MY JUDGEMENT |
| Self-employed, <2 years vintage | +2.0% | MY JUDGEMENT |
| Informal / gig | +3.0% | MY JUDGEMENT |

---

## 6. Base Interest Rate Bands (Indicative)

These are indicative India market ranges as of 2024. **Actual lender quotes will vary.**

| Loan Type | Low % | High % | Notes | Source |
|---|---|---|---|---|
| Personal Loan | 10.5% | 24.0% | Unsecured; wide spread based on profile | MARKET OBSERVATION |
| Home Loan | 8.4% | 12.0% | Secured; PSB/HDFC range | MARKET OBSERVATION |
| Loan Against Property | 9.5% | 14.0% | Secured; moderate risk | MARKET OBSERVATION |
| Gold Loan | 9.0% | 18.0% | Secured gold; varies by lender type | MARKET OBSERVATION |
| Two-Wheeler Loan | 10.0% | 20.0% | Semi-secured; wide profile range | MARKET OBSERVATION |
| Business Loan | 12.0% | 24.0% | Unsecured business; higher risk premium | MARKET OBSERVATION |

---

## 7. Processing Fee Assumptions

| Loan Type | Fee % | Source |
|---|---|---|
| Personal Loan | 2.0% | MY JUDGEMENT — typical NBFC/private bank range |
| Home Loan | 0.5% | MY JUDGEMENT — competitive market; PSBs often lower |
| Loan Against Property | 1.0% | MY JUDGEMENT |
| Gold Loan | 0.5% | MY JUDGEMENT |
| Two-Wheeler Loan | 1.5% | MY JUDGEMENT |
| Business Loan | 2.0% | MY JUDGEMENT |

---

## 8. Lender Sanction Multipliers (Unsecured loans)

> Max principal ≈ multiplier × net monthly income (further capped by FOIR)

| Borrower Type | Multiplier | Source |
|---|---|---|
| Salaried, Excellent credit | 24× NMI | MY JUDGEMENT; common PSB cap cited as 20–24× |
| Salaried, Good credit | 20× NMI | MY JUDGEMENT |
| Salaried, Fair credit | 15× NMI | MY JUDGEMENT |
| Salaried, Poor credit | 10× NMI | MY JUDGEMENT |
| Self-employed (income-based) | 18× NMI | MY JUDGEMENT |
| Informal / gig | 10× NMI | MY JUDGEMENT |
| Unknown credit score | 16× NMI (mid-range) | MY JUDGEMENT — widened range compensates for uncertainty |

For self-employed with significant unencumbered collateral, the LAP route uses LTV (see Section 9) instead of income multiplier.

---

## 9. Collateral / Secured Loan Rules

| What | Value | Source |
|---|---|---|
| Secured loan rate reduction vs unsecured equivalent | −2.5% | MY JUDGEMENT |
| LAP maximum LTV (Loan-to-Value) | 60% of collateral value | MY JUDGEMENT; broadly consistent with RBI LTV norms |
| Home Loan maximum LTV | 75–80% | RBI/PSB GUIDANCE |
| Collateral-to-request ratio to trigger LAP routing | 1.5× | MY JUDGEMENT — collateral ≥ 1.5× loan amount |
| Collateral must be unencumbered to be eligible | Yes | MY JUDGEMENT |

**Ravi routing logic:** If a self-employed borrower has unencumbered collateral worth ≥ 1.5× the requested loan, the engine routes them toward LAP / secured business loan rather than unsecured personal loan. This is explained explicitly in results.

---

## 10. Borrower-Safe Amount Discounts

The borrower-safe amount is always lower than the lender-likely estimate.

| Borrower Type | Discount from Lender Max | Source |
|---|---|---|
| Salaried (standard) | 20% | MY JUDGEMENT |
| Self-employed | 35% | MY JUDGEMENT — income variability warrants more buffer |
| Informal / gig | 40% | MY JUDGEMENT — highest variability |

Additionally, the safe amount is capped by the FOIR-based principal that can be serviced within the safe EMI ceiling.

---

## 11. Stress Test Assumptions

| What | Value | Source |
|---|---|---|
| Salaried income drop scenario | −20% of net income | MY JUDGEMENT |
| Self-employed income stress floor | 60% of stated income | MY JUDGEMENT — covers moderate income volatility |
| Informal income drop scenario | −20% of income, obligations unchanged | MY JUDGEMENT |
| Rate-sensitive loan stress (Home/LAP) | Rate rises +2 percentage points | MY JUDGEMENT |
| FOIR ≤ 50% under stress → "Still manageable" | 50% | MY JUDGEMENT |
| 50% < FOIR ≤ 65% under stress → "Becomes risky" | 65% | MY JUDGEMENT |
| FOIR > 65% under stress → "Not recommended" | >65% | MY JUDGEMENT |

---

## 12. High-Cost Debt Rules (Anita-type scenario)

| What | Value | Why | Source |
|---|---|---|---|
| Rate above which debt is "high-cost" | > 24% per annum | Above this, typical market rate; debt trap risk increases sharply | MY JUDGEMENT |
| Recent bounce window | Within 3 months | A bounce this recent is a strong current-stress signal | MY JUDGEMENT |
| App loan warning count | ≥ 2 active app loans | Multiple concurrent app loans signal over-leverage | MY JUDGEMENT |
| Existing FOIR threshold for DONT_BORROW override | > 50% | If existing EMIs alone exceed 50% of income, adding more is very high risk | MY JUDGEMENT |

**Anita verdict logic:**
1. Recent EMI bounce → +3 don't-borrow score
2. 3 app loans → +2 don't-borrow score
3. App loan rate >36% (above 24% threshold) → +2 don't-borrow score
4. Total score ≥ 5 → `DONT_BORROW`

The verdict explanation avoids shaming language and instead focuses on the financial mechanics.

---

## 13. Product Routing Rules

| Condition | Recommended Route | Source |
|---|---|---|
| Self-employed + unencumbered collateral ≥ 1.5× requested + business purpose | LAP or Secured Business Loan | MY JUDGEMENT |
| Self-employed + business vintage ≥ 2 years + business purpose, no significant collateral | Business Loan | MY JUDGEMENT |
| Requested loan type = Home Loan | Home Loan | Pass-through |
| Requested loan type = Gold Loan | Gold Loan | Pass-through |
| Requested loan type = Two-Wheeler | Two-Wheeler Loan | Pass-through |
| All other cases | Personal Loan | Default |

**ITR gap warning:** If ITR annual income / 12 < 40% of stated cash monthly income for self-employed, a warning is added that lenders may underwrite on ITR figures, not cash income.

---

## 14. Confidence Scoring

Points are accumulated for each meaningful known input. More points = higher confidence.

| Known Input | Points | Source |
|---|---|---|
| Credit score (not UNKNOWN) | 20 | MY JUDGEMENT — biggest single driver |
| Household expenses stated | 10 | MY JUDGEMENT |
| Employment stability stated | 10 | MY JUDGEMENT |
| Income variability stated | 10 | MY JUDGEMENT |
| Business vintage stated | 10 | MY JUDGEMENT |
| ITR income stated | 10 | MY JUDGEMENT |
| Emergency savings stated | 8 | MY JUDGEMENT |
| Credit utilisation stated | 8 | MY JUDGEMENT |
| Collateral value stated | 8 | MY JUDGEMENT |

| Threshold | Confidence Level |
|---|---|
| Score ≥ 60 | HIGH |
| Score 30–59 | MEDIUM |
| Score < 30 | LOW |

Source: MY JUDGEMENT for all thresholds.

---

## 15. APR Calculation

The estimated APR uses an iterative IRR-style approximation:

1. Calculate standard EMI on full principal at nominal rate
2. Reduce principal by processing fee (borrower receives less)
3. Solve for the rate that produces the same EMI on the reduced principal
4. That rate is the estimated APR

This is **not a legally precise APR** as defined by RBI's Fair Practices Code. It is clearly labelled as an estimate. Exact APR requires knowledge of all fees, disbursement timing, and lender-specific terms.

Source: MY JUDGEMENT — approximation method.

---

## 16. EMI Formula

Standard reducing-balance formula used throughout:

```
EMI = P × r × (1+r)^n / ((1+r)^n − 1)
```

Where:
- P = principal
- r = monthly interest rate (annual% / 100 / 12)
- n = tenure in months

Source: Standard financial mathematics; universally used by Indian lenders.

---

## 17. Priya Rate Logic

Priya (salaried, MNC, 5+ years, VERY_GOOD credit) qualifies for:
- Base personal loan rate band: 10.5%–24%
- Credit score VERY_GOOD adjustment: −0.75% on both ends → 9.75%–23.25%
- Employment stability (5+ years, large employer): −0.5% → 9.25%–22.75%
- Final range clamped to minimum 6%: **≈ 9.75%–22.25%** (exact values depend on runtime calculation)

This is lower than Ravi or Anita because of stable employment and known good credit. The "Why this rate?" explanation in the app is generated from this exact logic.

---

## 18. Ravi Secured-Product Routing Logic

Ravi owns a ₹45L unencumbered shop and requests ₹15L.

1. Collateral check: ₹45L ≥ 1.5 × ₹15L = ₹22.5L → **YES, LAP route triggered**
2. LAP rate band: 9.5%–14% (vs. 12%–24% for unsecured business loan)
3. LAP LTV: 60% × ₹45L = ₹27L sanction capacity → covers ₹15L comfortably
4. Route explanation is shown explicitly in results and negotiation card

Even if Ravi chose "Business Loan" as loan type, the engine overrides toward LAP when collateral conditions are met.

---

## 19. Unknown Value Treatment

| Input | Unknown Treatment |
|---|---|
| Credit score | `CreditScoreStatus.UNKNOWN` — widens rate range by ±2%, uses mid-range lender multiplier |
| Household expenses | Estimated from dependents + location type; warning added |
| Emergency savings | Set to null; stress test notes absence of buffer |
| Credit utilisation | Set to null; no credit score impact applied |
| Income variability | Set to null; stable assumption used but confidence reduced |
| Collateral value | Set to null; secured route not triggered |

**What we never do:** Convert null to 0, convert UNKNOWN credit to POOR, or assume best-case when information is missing. Missing information widens ranges and lowers confidence.

---

## 20. What Would Require Re-evaluation

The following changes would require review of this rules document and the corresponding `FinancialRules.kt` constants:

- RBI changes to LTV norms for LAP or home loans
- Significant market rate movement (e.g., repo rate change)
- Evidence that FOIR thresholds are materially different across lender segments
- New research on informal borrower debt-trap thresholds
