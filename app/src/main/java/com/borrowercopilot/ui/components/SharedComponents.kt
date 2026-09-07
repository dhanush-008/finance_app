package com.borrowercopilot.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borrowercopilot.domain.model.*
import com.borrowercopilot.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// VERDICT CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VerdictBadge(verdict: Verdict, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (verdict) {
        Verdict.BORROW      -> Triple(SuccessLight, Success, "BORROW")
        Verdict.BORROW_LESS -> Triple(WarningLight, Warning, "BORROW LESS")
        Verdict.DONT_BORROW -> Triple(DangerLight,  Danger,  "DON'T BORROW NOW")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text       = label,
            color      = fg,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp,
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONFIDENCE BADGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConfidenceBadge(confidence: Confidence, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (confidence) {
        Confidence.HIGH   -> Triple(SuccessLight, Success, "HIGH CONFIDENCE")
        Confidence.MEDIUM -> Triple(WarningLight, Warning, "MEDIUM CONFIDENCE")
        Confidence.LOW    -> Triple(Slate200,     Slate700, "LOW CONFIDENCE")
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.4.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FINANCIAL CARD  — wraps a result section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FinancialCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Navy600,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content  = content
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER  — numbered section within results
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(number: Int, title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Navy800),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number.toString(), color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MONEY DISPLAY  — large rupee number with label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MoneyDisplay(
    label: String,
    value: String,
    subtext: String? = null,
    valueColor: Color = Navy800,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            text       = value,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = valueColor
        )
        if (subtext != null) {
            Spacer(Modifier.height(2.dp))
            Text(text = subtext, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIVIDER ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LightDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = Slate200)
}

// ─────────────────────────────────────────────────────────────────────────────
// INFO BOX  — callout for warnings or highlights
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InfoBox(
    text: String,
    backgroundColor: Color = InfoLight,
    textColor: Color = Navy700,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RATE ROW  — label / value / explanation inline row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RateRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = Navy800)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TENURE TABLE ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TenureTableRow(
    label: String,
    emi: String,
    totalInterest: String,
    isHeader: Boolean = false,
    modifier: Modifier = Modifier
) {
    val weight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
    val color  = if (isHeader) Slate700 else Navy900
    val bg     = if (isHeader) Slate100 else White
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label,         modifier = Modifier.weight(1.3f), fontWeight = weight, color = color, fontSize = 13.sp)
        Text(text = emi,           modifier = Modifier.weight(1.2f), fontWeight = weight, color = color, fontSize = 13.sp, textAlign = TextAlign.End)
        Text(text = totalInterest, modifier = Modifier.weight(1.5f), fontWeight = weight, color = color, fontSize = 13.sp, textAlign = TextAlign.End)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STRESS OUTCOME BADGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StressOutcomeBadge(outcome: StressOutcome) {
    val (bg, fg, label) = when (outcome) {
        StressOutcome.MANAGEABLE       -> Triple(SuccessLight, Success, "Still manageable")
        StressOutcome.RISKY            -> Triple(WarningLight, Warning, "Becomes risky")
        StressOutcome.NOT_RECOMMENDED  -> Triple(DangerLight,  Danger,  "Not recommended")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXPANDABLE WHY SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExpandableExplanation(
    label: String = "Why this number?",
    explanation: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint               = Teal500,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(text = label, color = Teal500, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        if (expanded) {
            InfoBox(
                text            = explanation,
                backgroundColor = InfoLight,
                textColor       = Navy700
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRIMARY BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled  = enabled,
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = Navy800,
            contentColor   = White,
            disabledContainerColor = Slate200,
            disabledContentColor   = Slate500
        )
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECONDARY BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, Navy800),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Navy800)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OPTION CHIP (single-select)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OptionChip(
    text: String,
    hint: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg     = if (selected) Navy800 else White
    val fg     = if (selected) White else Navy900
    val border = if (selected) Navy800 else Slate200
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = text, color = fg, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            if (hint != null) {
                Text(text = hint, color = if (selected) Slate200 else Slate500, fontSize = 12.sp)
            }
        }
        if (selected) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = White,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NUMERIC INPUT FIELD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NumericInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String? = null,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = modifier.fillMaxWidth(),
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = Slate500) },
        suffix        = if (unit != null) {{ Text(unit, color = Slate500) }} else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape         = RoundedCornerShape(12.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Navy800,
            unfocusedBorderColor = Slate200,
            focusedLabelColor    = Navy800
        ),
        singleLine    = true
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// TEXT INPUT FIELD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = modifier.fillMaxWidth(),
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = Slate500) },
        shape         = RoundedCornerShape(12.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Navy800,
            unfocusedBorderColor = Slate200,
            focusedLabelColor    = Navy800
        ),
        singleLine    = true
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESS BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuestionProgressBar(answered: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = "Question $answered of $total",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
            Text(
                text  = "${((answered.toFloat() / total) * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress       = { if (total > 0) answered.toFloat() / total else 0f },
            modifier       = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color          = Navy800,
            trackColor     = Slate200
        )
    }
}
