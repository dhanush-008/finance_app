package com.borrowercopilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borrowercopilot.domain.model.*
import com.borrowercopilot.ui.components.*
import com.borrowercopilot.ui.theme.*

@Composable
fun ResultsScreen(
    result: AssessmentResult,
    onShowNegotiationCard: () -> Unit,
    onStartOver: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Navy800)
            }
            Spacer(Modifier.width(8.dp))
            Text("Your Assessment", style = MaterialTheme.typography.headlineMedium)
        }

        LazyColumn(
            contentPadding    = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── O1: Verdict ──────────────────────────────────────────────────
            item { VerdictSection(result) }

            // ── Missing data warnings ────────────────────────────────────────
            if (result.missingDataWarnings.isNotEmpty()) {
                item { MissingDataSection(result.missingDataWarnings) }
            }

            // ── O2: Amount ───────────────────────────────────────────────────
            item { AmountSection(result) }

            // ── O3: Rate ─────────────────────────────────────────────────────
            item { RateSection(result) }

            // ── O4: EMI ──────────────────────────────────────────────────────
            item { EmiSection(result) }

            // ── Tenure table ─────────────────────────────────────────────────
            item { TenureSection(result) }

            // ── Stress test ──────────────────────────────────────────────────
            item { StressSection(result) }

            // ── Product route note ───────────────────────────────────────────
            if (result.sanctionEstimate.routeExplanation != null) {
                item { ProductRouteSection(result.sanctionEstimate) }
            }

            // ── CTAs ─────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                PrimaryButton(text = "View Negotiation Card", onClick = onShowNegotiationCard)
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = "Start Over", onClick = onStartOver)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── Section: Verdict ────────────────────────────────────────────────────────

@Composable
private fun VerdictSection(result: AssessmentResult) {
    SectionHeader(1, "Should I borrow?")
    Spacer(Modifier.height(12.dp))
    FinancialCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VerdictBadge(result.verdict)
            Spacer(Modifier.width(12.dp))
            ConfidenceBadge(result.confidence)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text  = result.verdictExplanation,
            style = MaterialTheme.typography.bodyLarge,
            color = Navy900
        )
        if (result.keyReasons.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LightDivider()
            Spacer(Modifier.height(10.dp))
            Text("Key reasons:", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            result.keyReasons.forEach { reason ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("• ", color = Navy600, fontWeight = FontWeight.Bold)
                    Text(reason, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        ExpandableExplanation(
            label       = "Confidence explained",
            explanation = result.confidenceExplanation
        )
    }
}

// ─── Section: Missing data ────────────────────────────────────────────────────

@Composable
private fun MissingDataSection(warnings: List<String>) {
    InfoBox(
        text            = "⚠ Some inputs are unknown — ranges are wider.\n" + warnings.take(3).joinToString("\n") { "• $it" },
        backgroundColor = WarningLight,
        textColor       = Navy700
    )
}

// ─── Section: Amount ─────────────────────────────────────────────────────────

@Composable
private fun AmountSection(result: AssessmentResult) {
    SectionHeader(2, "How much can I borrow?")
    Spacer(Modifier.height(12.dp))
    FinancialCard {
        // Lender likely
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Likely lender sanction", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = result.sanctionEstimate.lenderLikelyRange.formatted(),
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Slate700
                )
                Text("Estimated — not guaranteed", style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.weight(1f)) {
                Text("Borrower-safe amount", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = result.sanctionEstimate.borrowerSafeRange.formatted(),
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Success
                )
                Text("Use this when negotiating", style = MaterialTheme.typography.bodySmall, color = Success)
            }
        }

        Spacer(Modifier.height(12.dp))
        InfoBox(
            text            = "Use the borrower-safe amount when negotiating.",
            backgroundColor = SuccessLight,
            textColor       = Success
        )
        Spacer(Modifier.height(10.dp))
        ExpandableExplanation(
            label       = "Why this number?",
            explanation = result.sanctionEstimate.explanation
        )
    }
}

// ─── Section: Rate ────────────────────────────────────────────────────────────

@Composable
private fun RateSection(result: AssessmentResult) {
    SectionHeader(3, "What is a fair rate?")
    Spacer(Modifier.height(12.dp))
    FinancialCard {
        RateRow("Fair interest rate", result.fairRateRange.formatted())
        Spacer(Modifier.height(8.dp))
        LightDivider()
        Spacer(Modifier.height(8.dp))
        RateRow(
            label = "Processing fee (est.)",
            value = "${String.format("%.1f", result.aprResult.processingFeePercent)}%  (~${formatLakh(result.aprResult.processingFeeRs)})"
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = "Estimated all-in APR",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text       = "${String.format("%.1f", result.aprResult.estimatedAprLow)}% – ${String.format("%.1f", result.aprResult.estimatedAprHigh)}%",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Navy800
            )
        }
        Spacer(Modifier.height(10.dp))
        ExpandableExplanation(
            label       = "APR vs interest rate — what's the difference?",
            explanation = result.aprResult.explanation
        )
        Spacer(Modifier.height(6.dp))
        ExpandableExplanation(
            label       = "Why this rate for me?",
            explanation = result.rateExplanation
        )
    }
}

// ─── Section: EMI ─────────────────────────────────────────────────────────────

@Composable
private fun EmiSection(result: AssessmentResult) {
    SectionHeader(4, "What EMI should I agree to?")
    Spacer(Modifier.height(12.dp))
    FinancialCard {
        Text("Recommended maximum EMI", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text       = "${formatLakh(result.recommendedMaxEmi)}/month",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            color      = Navy800
        )
        Spacer(Modifier.height(12.dp))
        ExpandableExplanation(
            label       = "How was this calculated?",
            explanation = result.emiExplanation
        )
        Spacer(Modifier.height(8.dp))
        ExpandableExplanation(
            label       = "Affordability detail",
            explanation = result.affordabilityResult.explanation
        )
    }
}

// ─── Section: Tenure Table ────────────────────────────────────────────────────

@Composable
private fun TenureSection(result: AssessmentResult) {
    FinancialCard {
        Text("Tenure trade-off", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Longer tenure = lower EMI but more total interest paid.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        // Table header
        TenureTableRow(
            label        = "Tenure",
            emi          = "Monthly EMI",
            totalInterest= "Total interest",
            isHeader     = true
        )
        LightDivider()

        result.tenureRows.forEachIndexed { idx, row ->
            TenureTableRow(
                label        = tenureLabel(row.tenureMonths),
                emi          = formatLakh(row.monthlyEmi),
                totalInterest= formatLakh(row.totalInterest)
            )
            if (idx < result.tenureRows.size - 1) LightDivider()
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Amounts shown for borrower-safe range midpoint at fair rate midpoint. Indicative only.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )
    }
}

// ─── Section: Stress Test ─────────────────────────────────────────────────────

@Composable
private fun StressSection(result: AssessmentResult) {
    val stress = result.stressResult
    FinancialCard {
        Text("Stress test", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Scenario: ${stress.scenario}",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate700
        )
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StressMetric(
                label = "Normal outflow",
                value = formatLakh(stress.normalEmi) + "/mo",
                modifier = Modifier.weight(1f)
            )
            StressMetric(
                label = "Stress outflow",
                value = formatLakh(stress.stressEmi) + "/mo",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StressMetric(
                label = "Normal remaining",
                value = formatLakh(stress.normalRemainingIncome) + "/mo",
                modifier = Modifier.weight(1f)
            )
            StressMetric(
                label = "Stress remaining",
                value = formatLakh(stress.stressRemainingIncome) + "/mo",
                valueColor = when (stress.outcome) {
                    StressOutcome.MANAGEABLE -> Success
                    StressOutcome.RISKY      -> Warning
                    else                     -> Danger
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))
        StressOutcomeBadge(stress.outcome)
        Spacer(Modifier.height(8.dp))
        Text(stress.explanation, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StressMetric(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = Navy800,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Slate100)
            .padding(12.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = valueColor)
        }
    }
}

// ─── Section: Product Route ───────────────────────────────────────────────────

@Composable
private fun ProductRouteSection(sanction: SanctionEstimate) {
    FinancialCard(accentColor = Teal500) {
        Text("Recommended loan route", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        val routeLabel = when (sanction.routeRecommendation) {
            ProductRoute.SECURED_LAP    -> "Loan Against Property (LAP) / Secured Business Loan"
            ProductRoute.BUSINESS_LOAN  -> "Formal Business Loan"
            ProductRoute.GOLD_LOAN      -> "Gold Loan"
            ProductRoute.HOME_LOAN      -> "Home Loan"
            ProductRoute.TWO_WHEELER_LOAN -> "Two-Wheeler Loan"
            ProductRoute.PERSONAL_LOAN  -> "Personal Loan"
        }
        Text(
            text       = routeLabel,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Teal500
        )
        Spacer(Modifier.height(8.dp))
        Text(sanction.routeExplanation ?: "", style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatLakh(amount: Double): String {
    val lakh = 100_000.0
    return when {
        amount >= lakh -> "₹${String.format("%.1f", amount / lakh)}L"
        else           -> "₹${String.format("%,.0f", amount)}"
    }
}

private fun tenureLabel(months: Int): String {
    val years = months / 12
    val rem   = months % 12
    return when {
        years > 0 && rem == 0 -> "${years}yr"
        years > 0             -> "${years}yr ${rem}mo"
        else                  -> "${months}mo"
    }
}
