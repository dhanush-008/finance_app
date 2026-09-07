# Borrower Copilot — 5-Minute Presentation Walkthrough

*This is a script for a 5-minute live presentation or recorded demo. Each section is timed.*

---

## 1. The Product Problem (45 seconds)

Most Indian borrowers walk into a loan conversation at a significant disadvantage.

They don't know how much a lender is likely to approve. They don't know whether the rate they've been quoted is fair or high. They don't know what EMI they can safely carry without becoming financially stressed. And they certainly don't know the difference between a 12% interest rate and the actual all-in cost after processing fees.

The lender has all of this information. The borrower has almost none.

Borrower Copilot flips that. It's a self-assessment tool — runs entirely on-device, no backend, no account — that gives a borrower an honest, explained picture of their position before they sit across from a lender.

The result is a Negotiation Card they can reference or show during that conversation.

---

## 2. User Journey (45 seconds)

The flow is:

**Welcome → 8–10 must questions → adaptive follow-up questions (varies by income type) → review answers → calculate → four result sections → Negotiation Card**

The must questions take about 2 minutes. The adaptive questions take another 1–2 minutes. Total is 3–5 minutes.

Nothing is stored. The app works offline. When you tap Start Over, everything is cleared.

The four outputs the app produces are:
1. Should I borrow at all? (verdict + confidence)
2. How much am I actually eligible for? (lender-likely vs borrower-safe — always two separate numbers)
3. What is a fair interest rate for me? (range + estimated APR)
4. What EMI should I agree to? (ceiling + tenure table + stress test)

Then the Negotiation Card combines everything into one screenshot-friendly screen.

---

## 3. Question Design (40 seconds)

The 8–10 must questions are sufficient to produce all four outputs. They ask: loan purpose, loan type, amount, income type, net income, existing EMIs, household expenses, age, credit score, location.

The adaptive questions depend entirely on what was answered before. A salaried borrower gets employment stability, variable income, credit utilisation, emergency savings. A self-employed borrower gets business vintage, ITR income, collateral check. An informal borrower gets app loan count, recent bounce, dependents.

Every adaptive question is gated on a rule: *if this answer would not change any output, the question is not asked.* For example, collateral value is only asked if the borrower said they own property. App loan balance is only asked if they said they have active app loans.

I deliberately did not ask questions like "what is your dream EMI?" or vague lifestyle questions. Every input maps directly to a calculation.

---

## 4. Rules Engine (40 seconds)

All financial logic lives in two files: `FinancialRules.kt` and `BorrowerAssessmentEngine.kt`.

`FinancialRules.kt` is the single source of truth for every threshold: FOIR limits, rate bands, credit score adjustments, stress test parameters, confidence scoring weights, processing fee assumptions. Nothing is scattered across UI files.

`BorrowerAssessmentEngine` takes a `BorrowerProfile` and a `LoanRequest` and returns a complete `AssessmentResult`. The UI never does financial arithmetic — it only formats and displays.

If I change the FOIR threshold from 40% to 45% in `FinancialRules.kt`, every calculation in the app and every unit test updates automatically. I can demonstrate this live.

The key design distinction is: **lender-likely amount** (what a lender might approve) and **borrower-safe amount** (what the borrower can responsibly carry) are computed separately and always shown as two different numbers. We always recommend the safe amount.

---

## 5. Priya (35 seconds)

Priya is 29, salaried at a large MNC in Bengaluru, ₹1.1L/month, 780 credit score, ₹14,000 car EMI, wants ₹8L personal loan for her wedding.

Her strong credit profile and stable employment push her rate range down to roughly 10–12.5%. The lender is likely to sanction ₹7.5L–₹9L. But because her ₹14,000 car EMI and ₹42,000 household expenses (including ₹28,000 rent) already consume a large portion of her income, her borrower-safe range is closer to ₹5L–₹6.5L.

The verdict is BORROW LESS. The app explains exactly why and gives her an EMI ceiling to stick to.

This shows the core product principle: lender likely ≠ borrower safe.

---

## 6. Ravi (40 seconds)

Ravi is 42, runs a kirana store in Mysuru for 14 years, cash income ₹40k–₹80k, ITR shows only ₹4.2L/year, owns a ₹45L unencumbered shop, no formal credit history, no credit score.

This scenario demonstrates two important behaviours.

First: his credit score is UNKNOWN. The app does not treat this as 300 or POOR. It widens the rate range to acknowledge uncertainty. There is a unit test specifically for this.

Second: the engine detects that his unencumbered collateral is 3× the requested loan amount and routes him toward a Loan Against Property rather than an unsecured business loan. This appears as a dedicated section in results with an explanation of why, and is reflected in the Negotiation Card. The LAP rate band (9.5%–14%) is significantly better than the unsecured business loan band (12%–24%).

The ITR gap warning is also flagged — lenders will underwrite on documented income, not cash.

---

## 7. Anita (40 seconds)

Anita is 35, delivery rider and home tailor in Hubballi, income ₹26k–₹30k, 2 children, husband not earning, 3 active app loans at 36%+, one bounce last month, wants ₹1.5L for an electric scooter.

The verdict is DON'T BORROW NOW.

This is not a product failure — it's the app working correctly. The engine accumulates risk signals: recent bounce (+3 score), 3 app loans (+2), rate above 24% threshold (+2). Total 7, threshold is 5. DONT_BORROW is triggered.

The explanation does not shame Anita. It explains the mechanics: the problem is not the scooter — the scooter is actually a productive purpose that makes sense for her livelihood. The problem is that her existing debt structure means adding any new repayment obligation right now makes her position worse, not better. The suggestion is: clear or reduce the high-cost app loans, maintain 3–6 months of on-time payments, then reassess.

Her safe EMI capacity at the time of the assessment is near zero — the tenure table confirms no option is affordable.

---

## 8. Explainability (20 seconds)

Every major number has an expandable "Why?" section. The text is generated from the actual rule that produced the number — not a generic AI sentence. For example, the safe EMI explanation names the specific income, existing EMI, and expense figures used. The rate explanation names the credit score band and employment stability adjustment applied.

This is important for trust: a borrower should understand why the number is what it is, not just accept it.

---

## 9. Limitations (30 seconds)

I want to be upfront about what this is not:

- It is not connected to any real lender system. Numbers are indicative.
- The rate bands are based on my own market observation, not live lender APIs.
- The APR is an approximation — legally precise APR requires all fees and disbursement timing.
- The lender sanction estimate uses a simplified income-multiplier model, not actual underwriting rules.
- Credit bureau data is not integrated — credit score must be self-reported.
- Confidence is LOW for borrowers with limited information. The app is honest about this.
- The app does not account for recent RBI policy changes unless rules are manually updated.

All of this is labelled in the app as "estimated" or "indicative" with appropriate disclaimers.

---

## 10. What I Would Build Next (20 seconds)

With more time I would build:

- **Rate comparison**: allow the borrower to input a specific lender offer and compare it against the fair rate range and estimated APR
- **Debt consolidation calculator**: for borrowers like Anita, show them concretely what closing one app loan first does to their capacity
- **Amortisation table view**: full month-by-month repayment schedule
- **Regional rate adjustment**: metropolitan vs semi-urban borrowers face different market rates
- **Accessibility audit**: the current UI passes basic contrast and touch-target sizes; a full screen-reader pass would follow

---

## 11. What I Deliberately Cut (15 seconds)

I deliberately did not build:

- Authentication or account system — unnecessary and adds data liability
- A chatbot interface — a structured questionnaire gives better data quality and is easier to explain
- ML-based scoring — the challenge asked for deterministic, explainable rules, and that is the right call for a product like this
- Loan product marketplace or lender referral — out of scope, and would compromise the independence of the tool
- Dark mode — nice to have, cut for time; theme is structured to add it easily

---

*End of walkthrough. Total: ~5 minutes.*
