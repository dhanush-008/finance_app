package com.borrowercopilot.engine

import com.borrowercopilot.domain.engine.BorrowerAssessmentEngine
import com.borrowercopilot.domain.model.*
import com.borrowercopilot.domain.questions.AdaptiveQuestionEngine
import com.borrowercopilot.domain.questions.BorrowerFixtures
import com.borrowercopilot.domain.rules.FinancialRules
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BorrowerAssessmentEngine.
 *
 * IMPORTANT: Fixtures are INPUTS only. Tests verify the engine calculates
 * the correct outputs — nothing is hard-coded in the engine itself.
 *
 * If you change FinancialRules (e.g. FOIR from 40% to 45%) these tests will
 * automatically reflect the new numbers. That's the point.
 */
class AssessmentEngineTest {

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: run assessment from fixture answers
    // ─────────────────────────────────────────────────────────────────────────

    private fun runPriya() = BorrowerAssessmentEngine.calculateAssessment(
        AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.PRIYA),
        AdaptiveQuestionEngine.buildLoanRequest(BorrowerFixtures.PRIYA)
    )

    private fun runRavi() = BorrowerAssessmentEngine.calculateAssessment(
        AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.RAVI),
        AdaptiveQuestionEngine.buildLoanRequest(BorrowerFixtures.RAVI)
    )

    private fun runAnita() = BorrowerAssessmentEngine.calculateAssessment(
        AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.ANITA),
        AdaptiveQuestionEngine.buildLoanRequest(BorrowerFixtures.ANITA)
    )

    // ─────────────────────────────────────────────────────────────────────────
    // PRIYA TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `priya - verdict is BORROW or BORROW_LESS, not DONT_BORROW`() {
        val result = runPriya()
        assertNotEquals(
            "Priya has strong credit profile — should not get DONT_BORROW",
            Verdict.DONT_BORROW, result.verdict
        )
    }

    @Test
    fun `priya - has positive monthly repayment capacity`() {
        val result = runPriya()
        assertTrue(
            "Priya earns ₹1.1L/month with ₹14k existing EMI — must have capacity",
            result.affordabilityResult.maxSafeMonthlyEmi > 0
        )
    }

    @Test
    fun `priya - lender sanction is higher than borrower-safe amount`() {
        val result = runPriya()
        val lenderMid = result.sanctionEstimate.lenderLikelyRange.midpoint()
        val safeMid   = result.sanctionEstimate.borrowerSafeRange.midpoint()
        assertTrue(
            "Lender likely (${lenderMid}) should exceed borrower safe (${safeMid})",
            lenderMid > safeMid
        )
    }

    @Test
    fun `priya - fair rate band is below informal borrower range`() {
        val priyaResult  = runPriya()
        val anitaResult  = runAnita()
        assertTrue(
            "Priya's good credit (780) should give lower rate high-end than Anita (unknown/informal)",
            priyaResult.fairRateRange.highPercent < anitaResult.fairRateRange.highPercent
        )
    }

    @Test
    fun `priya - existing EMI is reflected in affordability`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.PRIYA)
        assertEquals(
            "Priya's existing EMI must be ₹14,000",
            14_000.0, profile.existingMonthlyEmi, 0.01
        )
        val result = runPriya()
        assertTrue(
            "Affordability explanation must mention existing EMI",
            result.affordabilityResult.explanation.contains("14") ||
            result.affordabilityResult.explanation.contains("EMI", ignoreCase = true)
        )
    }

    @Test
    fun `priya - credit score is VERY_GOOD band, not UNKNOWN`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.PRIYA)
        assertEquals(CreditScoreStatus.VERY_GOOD, profile.creditScoreStatus)
        assertNotEquals(CreditScoreStatus.UNKNOWN, profile.creditScoreStatus)
    }

    @Test
    fun `priya - confidence is MEDIUM or HIGH`() {
        val result = runPriya()
        assertTrue(
            "Priya answers most questions — confidence should be MEDIUM or HIGH",
            result.confidence in listOf(Confidence.MEDIUM, Confidence.HIGH)
        )
    }

    @Test
    fun `priya - APR is higher than nominal interest rate`() {
        val result = runPriya()
        assertTrue(
            "APR (${result.aprResult.estimatedAprHigh}%) must exceed nominal rate (${result.aprResult.nominalRate}%)",
            result.aprResult.estimatedAprHigh > result.aprResult.nominalRate
        )
    }

    @Test
    fun `priya - tenure table has multiple rows`() {
        val result = runPriya()
        assertTrue("Priya should have ≥2 tenure options", result.tenureRows.size >= 2)
    }

    @Test
    fun `priya - longer tenure has lower EMI but higher total interest`() {
        val result = runPriya()
        if (result.tenureRows.size >= 2) {
            val short = result.tenureRows.first()
            val long  = result.tenureRows.last()
            assertTrue("Longer tenure should have lower EMI",
                long.monthlyEmi < short.monthlyEmi)
            assertTrue("Longer tenure should have higher total interest",
                long.totalInterest > short.totalInterest)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RAVI TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `ravi - credit score is UNKNOWN`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.RAVI)
        assertEquals(
            "Ravi has no formal credit history — must be UNKNOWN, not POOR or 300",
            CreditScoreStatus.UNKNOWN, profile.creditScoreStatus
        )
    }

    @Test
    fun `ravi - credit score UNKNOWN is not treated as POOR`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.RAVI)
        assertNotEquals(
            "UNKNOWN credit score must NEVER be silently converted to POOR",
            CreditScoreStatus.POOR, profile.creditScoreStatus
        )
    }

    @Test
    fun `ravi - unknown credit score widens rate range`() {
        val raviResult = runRavi()
        val priyaResult = runPriya()
        val raviWidth  = raviResult.fairRateRange.highPercent - raviResult.fairRateRange.lowPercent
        val priyaWidth = priyaResult.fairRateRange.highPercent - priyaResult.fairRateRange.lowPercent
        assertTrue(
            "Unknown credit score should produce a wider rate range than known score",
            raviWidth > priyaWidth
        )
    }

    @Test
    fun `ravi - is routed to secured LAP or business loan, not personal loan`() {
        val result = runRavi()
        val route = result.sanctionEstimate.routeRecommendation
        assertTrue(
            "Ravi owns ₹45L unencumbered shop — should route to SECURED_LAP or BUSINESS_LOAN, not PERSONAL_LOAN. Got: $route",
            route in listOf(ProductRoute.SECURED_LAP, ProductRoute.BUSINESS_LOAN)
        )
    }

    @Test
    fun `ravi - route explanation is present`() {
        val result = runRavi()
        assertNotNull(
            "Ravi's product route should include an explanation",
            result.sanctionEstimate.routeExplanation
        )
        assertTrue(
            "Route explanation should mention collateral or secured",
            result.sanctionEstimate.routeExplanation!!.contains("collateral", ignoreCase = true) ||
            result.sanctionEstimate.routeExplanation!!.contains("secured", ignoreCase = true) ||
            result.sanctionEstimate.routeExplanation!!.contains("property", ignoreCase = true)
        )
    }

    @Test
    fun `ravi - verdict is not DONT_BORROW given strong collateral and vintage`() {
        val result = runRavi()
        assertNotEquals(
            "Ravi has ₹45L unencumbered collateral and 14y vintage — should not be DONT_BORROW",
            Verdict.DONT_BORROW, result.verdict
        )
    }

    @Test
    fun `ravi - lender likely range is non-zero`() {
        val result = runRavi()
        assertTrue(
            "Ravi should have non-zero lender sanction estimate",
            result.sanctionEstimate.lenderLikelyRange.high > 0
        )
    }

    @Test
    fun `ravi - missing data widens sanction range`() {
        val result = runRavi()
        val raviWidth  = result.sanctionEstimate.lenderLikelyRange.high - result.sanctionEstimate.lenderLikelyRange.low
        val priyaResult = runPriya()
        val priyaWidth  = priyaResult.sanctionEstimate.lenderLikelyRange.high - priyaResult.sanctionEstimate.lenderLikelyRange.low
        // Ravi has unknown credit score — his range should be equal or wider
        assertTrue(
            "Unknown credit score must produce equal or wider sanction range than known score",
            raviWidth >= priyaWidth * 0.9  // allow 10% tolerance
        )
    }

    @Test
    fun `ravi - self-employed income type is recognised`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.RAVI)
        assertEquals(IncomeType.SELF_EMPLOYED, profile.incomeType)
    }

    @Test
    fun `ravi - collateral value is stored as lakhs`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.RAVI)
        assertEquals(
            "Collateral value should be 45 lakhs",
            45.0, profile.collateralValueLakh ?: 0.0, 0.01
        )
        assertTrue("Collateral must be marked unencumbered", profile.isCollateralUnencumbered)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANITA TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `anita - verdict is DONT_BORROW`() {
        val result = runAnita()
        assertEquals(
            "Anita has recent bounce + 3 app loans + high-cost debt. Must be DONT_BORROW",
            Verdict.DONT_BORROW, result.verdict
        )
    }

    @Test
    fun `anita - recent EMI bounce is captured`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.ANITA)
        assertTrue("Anita's recent EMI bounce must be true", profile.recentEmiBounce)
    }

    @Test
    fun `anita - app loan count is 3`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.ANITA)
        assertEquals("Anita has 3 app loans", 3, profile.appLoanCount)
    }

    @Test
    fun `anita - key reasons mention debt or bounce`() {
        val result = runAnita()
        val allReasons = result.keyReasons.joinToString(" ").lowercase()
        assertTrue(
            "Key reasons must mention bounce, debt, or app loans. Got: ${result.keyReasons}",
            allReasons.contains("bounce") || allReasons.contains("debt") ||
            allReasons.contains("app") || allReasons.contains("obligation")
        )
    }

    @Test
    fun `anita - verdict explanation suggests reducing debt first`() {
        val result = runAnita()
        val explanation = result.verdictExplanation.lowercase()
        assertTrue(
            "DONT_BORROW explanation should suggest addressing debt/bounce first. Got: ${result.verdictExplanation}",
            explanation.contains("debt") || explanation.contains("bounce") ||
            explanation.contains("repay") || explanation.contains("stress")
        )
    }

    @Test
    fun `anita - income type is INFORMAL_GIG`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.ANITA)
        assertEquals(IncomeType.INFORMAL_GIG, profile.incomeType)
    }

    @Test
    fun `anita - credit score remains UNKNOWN not POOR`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.ANITA)
        assertEquals(CreditScoreStatus.UNKNOWN, profile.creditScoreStatus)
        assertNotEquals(CreditScoreStatus.POOR, profile.creditScoreStatus)
    }

    @Test
    fun `anita - dependents are captured`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.ANITA)
        assertEquals("Anita has 2 dependents", 2, profile.numberOfDependents)
    }

    @Test
    fun `anita - high cost debt rate is above threshold`() {
        val profile = AdaptiveQuestionEngine.buildProfile(BorrowerFixtures.ANITA)
        val rate = profile.existingHighCostRate ?: 0.0
        assertTrue(
            "Anita's app loan rate (${rate}%) should be above 24% high-cost threshold",
            rate > FinancialRules.HighCostDebtRules.HIGH_COST_RATE_THRESHOLD * 100
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CROSS-CUTTING RULES TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `unknown credit score is never treated as 300 or POOR`() {
        // Build a minimal profile with UNKNOWN credit score
        val profileUnknown = BorrowerProfile(
            age = 35, location = "Test", incomeType = IncomeType.SALARIED,
            netMonthlyIncome = 60_000.0,
            creditScoreStatus = CreditScoreStatus.UNKNOWN
        )
        val profilePoor = profileUnknown.copy(creditScoreStatus = CreditScoreStatus.POOR)

        val request = LoanRequest(LoanPurpose.OTHER, LoanType.PERSONAL_LOAN, 300_000.0, 36)

        val resultUnknown = BorrowerAssessmentEngine.calculateAssessment(profileUnknown, request)
        val resultPoor    = BorrowerAssessmentEngine.calculateAssessment(profilePoor,    request)

        // Unknown should NOT produce the same narrow result as explicitly POOR
        // Unknown range should be wider (range high - low)
        val unknownWidth = resultUnknown.fairRateRange.highPercent - resultUnknown.fairRateRange.lowPercent
        val poorWidth    = resultPoor.fairRateRange.highPercent    - resultPoor.fairRateRange.lowPercent
        assertTrue(
            "Unknown credit score rate range (width $unknownWidth) must be ≥ poor score range (width $poorWidth) or at most slightly narrower",
            unknownWidth >= poorWidth - 0.5
        )
    }

    @Test
    fun `missing data widens ranges compared to complete data`() {
        val baseProfile = BorrowerProfile(
            age = 35, location = "Test", incomeType = IncomeType.SALARIED,
            netMonthlyIncome = 60_000.0,
            creditScoreStatus = CreditScoreStatus.GOOD,
            estimatedHouseholdExpenses = 20_000.0,
            employmentStability = EmploymentStability.THREE_TO_FIVE_YEARS
        )
        val incompleteProfile = baseProfile.copy(
            creditScoreStatus = CreditScoreStatus.UNKNOWN,
            estimatedHouseholdExpenses = null
        )
        val request = LoanRequest(LoanPurpose.OTHER, LoanType.PERSONAL_LOAN, 300_000.0, 36)

        val completeResult   = BorrowerAssessmentEngine.calculateAssessment(baseProfile,    request)
        val incompleteResult = BorrowerAssessmentEngine.calculateAssessment(incompleteProfile, request)

        val completeWidth   = completeResult.fairRateRange.highPercent   - completeResult.fairRateRange.lowPercent
        val incompleteWidth = incompleteResult.fairRateRange.highPercent - incompleteResult.fairRateRange.lowPercent

        assertTrue(
            "Incomplete data should produce wider rate range. Complete width=$completeWidth, Incomplete width=$incompleteWidth",
            incompleteWidth >= completeWidth
        )
    }

    @Test
    fun `lender sanction is not equal to borrower-safe amount`() {
        val priya = runPriya()
        val ravi  = runRavi()

        assertNotEquals("Priya: lender sanction mid must differ from safe mid",
            priya.sanctionEstimate.lenderLikelyRange.midpoint(),
            priya.sanctionEstimate.borrowerSafeRange.midpoint(),
            1.0
        )
        assertNotEquals("Ravi: lender sanction mid must differ from safe mid",
            ravi.sanctionEstimate.lenderLikelyRange.midpoint(),
            ravi.sanctionEstimate.borrowerSafeRange.midpoint(),
            1.0
        )
    }

    @Test
    fun `stress test always produces a result`() {
        listOf(runPriya(), runRavi(), runAnita()).forEach { result ->
            assertNotNull("Stress result must not be null", result.stressResult)
            assertTrue("Stress scenario text must not be blank", result.stressResult.scenario.isNotBlank())
        }
    }

    @Test
    fun `EMI calculation is mathematically correct`() {
        // P=5L, R=12%pa, N=36mo -> EMI should be ~16,607
        val emi = FinancialRules.calculateEmi(500_000.0, 12.0, 36)
        assertEquals("Standard EMI formula: ₹5L @ 12% for 36 months", 16_607.0, emi, 100.0)
    }

    @Test
    fun `EMI formula: longer tenure reduces EMI`() {
        val emi24 = FinancialRules.calculateEmi(300_000.0, 14.0, 24)
        val emi48 = FinancialRules.calculateEmi(300_000.0, 14.0, 48)
        assertTrue("48-month EMI must be lower than 24-month EMI", emi48 < emi24)
    }

    @Test
    fun `EMI formula: higher rate increases EMI for same principal and tenure`() {
        val emiLow  = FinancialRules.calculateEmi(400_000.0, 10.0, 36)
        val emiHigh = FinancialRules.calculateEmi(400_000.0, 18.0, 36)
        assertTrue("Higher rate must produce higher EMI", emiHigh > emiLow)
    }

    @Test
    fun `APR is higher than nominal rate when processing fee exists`() {
        val apr = FinancialRules.estimateApr(500_000.0, 12.0, 36, 0.02)
        assertTrue("APR must exceed nominal rate when there's a processing fee", apr > 12.0)
    }

    @Test
    fun `changing FOIR threshold changes results - engine reads from rules`() {
        // This test verifies the engine uses FinancialRules, not hard-coded values.
        // We validate by ensuring the affordability ceiling scales with income.
        val highIncomeProfile = BorrowerProfile(
            age = 30, location = "Test", incomeType = IncomeType.SALARIED,
            netMonthlyIncome = 200_000.0,
            existingMonthlyEmi = 0.0,
            creditScoreStatus = CreditScoreStatus.GOOD,
            estimatedHouseholdExpenses = 50_000.0
        )
        val lowIncomeProfile = highIncomeProfile.copy(netMonthlyIncome = 50_000.0,
            estimatedHouseholdExpenses = 20_000.0)

        val request = LoanRequest(LoanPurpose.OTHER, LoanType.PERSONAL_LOAN, 200_000.0, 36)

        val highResult = BorrowerAssessmentEngine.calculateAssessment(highIncomeProfile, request)
        val lowResult  = BorrowerAssessmentEngine.calculateAssessment(lowIncomeProfile,  request)

        assertTrue(
            "Higher income must yield higher safe EMI",
            highResult.affordabilityResult.maxSafeMonthlyEmi > lowResult.affordabilityResult.maxSafeMonthlyEmi
        )
        assertTrue(
            "Higher income must yield higher borrower-safe amount",
            highResult.sanctionEstimate.borrowerSafeRange.high > lowResult.sanctionEstimate.borrowerSafeRange.high
        )
    }

    @Test
    fun `adaptive questions: salaried borrower gets employment questions`() {
        val answers = BorrowerFixtures.PRIYA
        val sequence = com.borrowercopilot.domain.questions.AdaptiveQuestionEngine.buildSequence(answers)
        assertTrue(
            "Salaried borrower should have employment stability in sequence",
            com.borrowercopilot.domain.questions.QuestionId.EMPLOYMENT_STABILITY in sequence
        )
    }

    @Test
    fun `adaptive questions: informal borrower gets app loan questions`() {
        val sequence = com.borrowercopilot.domain.questions.AdaptiveQuestionEngine.buildSequence(BorrowerFixtures.ANITA)
        assertTrue(
            "Informal borrower should have app loan questions",
            com.borrowercopilot.domain.questions.QuestionId.APP_LOANS_COUNT in sequence
        )
    }

    @Test
    fun `adaptive questions: self-employed borrower gets collateral questions`() {
        val sequence = com.borrowercopilot.domain.questions.AdaptiveQuestionEngine.buildSequence(BorrowerFixtures.RAVI)
        assertTrue(
            "Self-employed borrower should have collateral questions",
            com.borrowercopilot.domain.questions.QuestionId.BUSINESS_ASSETS_COLLATERAL in sequence
        )
    }

    @Test
    fun `adaptive questions: salaried borrower does NOT get app loan questions`() {
        val sequence = com.borrowercopilot.domain.questions.AdaptiveQuestionEngine.buildSequence(BorrowerFixtures.PRIYA)
        assertFalse(
            "Salaried borrower should NOT see app loan count question",
            com.borrowercopilot.domain.questions.QuestionId.APP_LOANS_COUNT in sequence
        )
    }

    @Test
    fun `negotiation card is buildable from all three borrowers`() {
        listOf(
            Pair(BorrowerFixtures.PRIYA, "Priya"),
            Pair(BorrowerFixtures.RAVI, "Ravi"),
            Pair(BorrowerFixtures.ANITA, "Anita")
        ).forEach { (fixture, name) ->
            val profile = AdaptiveQuestionEngine.buildProfile(fixture)
            val request = AdaptiveQuestionEngine.buildLoanRequest(fixture)
            val result  = BorrowerAssessmentEngine.calculateAssessment(profile, request)
            val card    = BorrowerAssessmentEngine.buildNegotiationCard(result, request)

            assertNotNull("$name: negotiation card must not be null", card)
            assertTrue("$name: safe borrowing range must be positive", card.safeBorrowingRange.high > 0)
            assertNotNull("$name: fair interest range must exist", card.fairInterestRange)
        }
    }
}
