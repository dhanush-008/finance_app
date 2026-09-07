package com.borrowercopilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borrowercopilot.domain.model.*
import com.borrowercopilot.ui.components.*
import com.borrowercopilot.ui.theme.*

@Composable
fun NegotiationCardScreen(
    card: NegotiationCard,
    onBack: () -> Unit,
    onStartOver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate100)
            .statusBarsPadding()
    ) {
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
            Text("Negotiation Card", style = MaterialTheme.typography.headlineMedium)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ── THE CARD ────────────────────────────────────────────────────
            NegotiationCardContent(card)

            Spacer(Modifier.height(16.dp))

            // Disclaimer
            Text(
                text      = "This is a self-assessment, not a lender sanction or financial guarantee.",
                style     = MaterialTheme.typography.bodySmall,
                color     = Slate500,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            SecondaryButton(text = "Back to Results", onClick = onBack)
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick  = onStartOver,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Over", color = Slate500)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NegotiationCardContent(card: NegotiationCard) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text       = "BORROWER COPILOT",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Slate500,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text       = "Negotiation Card",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Navy900
                    )
                }
                VerdictBadge(card.verdict)
            }

            Spacer(Modifier.height(16.dp))
            LightDivider()
            Spacer(Modifier.height(16.dp))

            // ── Loan details ──────────────────────────────────────────────
            CardRow("Loan purpose",    formatPurpose(card.loanPurpose))
            CardRow("Loan type",       formatLoanType(card.loanType))
            CardRow("Requested amount",formatRupees(card.requestedAmountRs))

            Spacer(Modifier.height(16.dp))
            SectionBand("MY SAFE POSITION", Navy800, White)
            Spacer(Modifier.height(12.dp))

            CardRowBig("Safe borrowing range", card.safeBorrowingRange.formatted(), Success)
            Spacer(Modifier.height(8.dp))
            CardRowBig("Recommended EMI ceiling", "${formatLakh(card.recommendedEmiCeiling)}/month", Navy800)

            Spacer(Modifier.height(16.dp))
            SectionBand("WHAT I SHOULD EXPECT", Teal500, White)
            Spacer(Modifier.height(12.dp))

            CardRow("Fair interest rate",    card.fairInterestRange.formatted())
            Spacer(Modifier.height(6.dp))
            CardRow("Estimated all-in APR",  "${String.format("%.1f", card.estimatedAprRange.lowPercent)}% – ${String.format("%.1f", card.estimatedAprRange.highPercent)}%")
            Spacer(Modifier.height(6.dp))
            CardRow("Likely lender sanction",card.likelyLenderSanction.formatted())
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Sanction is estimated, not guaranteed.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )

            if (card.topReasons.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionBand("WHY", Navy600, White)
                Spacer(Modifier.height(12.dp))
                card.topReasons.forEach { reason ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("→ ", color = Teal500, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionBand("BEFORE I SIGN", Gold500, Navy900)
            Spacer(Modifier.height(12.dp))

            val checklist = listOf(
                "Compare APR, not just the headline interest rate",
                "Ask for all processing fees and upfront charges",
                "Do not cross the recommended EMI ceiling",
                "Check foreclosure / prepayment terms",
                "Ask for the complete repayment schedule"
            )
            checklist.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Gold500)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(item, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(20.dp))
            LightDivider()
            Spacer(Modifier.height(12.dp))

            // Confidence note
            ConfidenceBadge(card.confidence)
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "This card is based on your self-reported inputs. " +
                    "It is not a credit decision, underwriting approval, or financial advice.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )
        }
    }
}

// ─── Card sub-components ───────────────────────────────────────────────────────

@Composable
private fun CardRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text       = value,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp,
            color      = Navy900,
            modifier   = Modifier.weight(1f),
            textAlign  = TextAlign.End
        )
    }
}

@Composable
private fun CardRowBig(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            text       = value,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = valueColor
        )
    }
}

@Composable
private fun SectionBand(label: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text          = label,
            color         = textColor,
            fontWeight    = FontWeight.Bold,
            fontSize      = 11.sp,
            letterSpacing = 1.sp
        )
    }
}

// ─── Formatters ───────────────────────────────────────────────────────────────

private fun formatPurpose(p: LoanPurpose) = when (p) {
    LoanPurpose.WEDDING             -> "Wedding / Family event"
    LoanPurpose.HOME_PURCHASE       -> "Home purchase"
    LoanPurpose.HOME_RENOVATION     -> "Home renovation"
    LoanPurpose.BUSINESS_EXPANSION  -> "Business expansion"
    LoanPurpose.VEHICLE_PURCHASE    -> "Vehicle purchase"
    LoanPurpose.EDUCATION           -> "Education"
    LoanPurpose.MEDICAL             -> "Medical"
    LoanPurpose.DEBT_CONSOLIDATION  -> "Debt consolidation"
    LoanPurpose.WORKING_CAPITAL     -> "Working capital"
    LoanPurpose.OTHER               -> "Other"
}

private fun formatLoanType(t: LoanType) = when (t) {
    LoanType.PERSONAL_LOAN          -> "Personal loan"
    LoanType.HOME_LOAN              -> "Home loan"
    LoanType.LOAN_AGAINST_PROPERTY  -> "Loan against property"
    LoanType.GOLD_LOAN              -> "Gold loan"
    LoanType.TWO_WHEELER_LOAN       -> "Two-wheeler loan"
    LoanType.BUSINESS_LOAN          -> "Business loan"
}

private fun formatRupees(amount: Double): String {
    val lakh = 100_000.0
    val crore = 10_000_000.0
    return when {
        amount >= crore -> "₹${String.format("%.2f", amount / crore)} Cr"
        amount >= lakh  -> "₹${String.format("%.2f", amount / lakh)} L"
        else            -> "₹${String.format("%,.0f", amount)}"
    }
}

private fun formatLakh(amount: Double): String {
    val lakh = 100_000.0
    return if (amount >= lakh) "₹${String.format("%.1f", amount / lakh)}L"
    else "₹${String.format("%,.0f", amount)}"
}
