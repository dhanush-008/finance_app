package com.borrowercopilot.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borrowercopilot.domain.questions.*
import com.borrowercopilot.ui.components.PrimaryButton
import com.borrowercopilot.ui.theme.*

@Composable
fun ReviewScreen(
    answers: Map<QuestionId, QuestionAnswer>,
    questionSequence: List<QuestionId>,
    canCalculate: Boolean,
    onEdit: (QuestionId) -> Unit,
    onCalculate: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Navy800)
            }
            Spacer(Modifier.width(8.dp))
            Text("Review Answers", style = MaterialTheme.typography.headlineMedium)
        }

        LazyColumn(
            contentPadding    = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group questions by section
            val mustIds    = questionSequence.filter { it in mustQIds }
            val adaptiveIds = questionSequence.filter { it !in mustQIds }

            if (mustIds.isNotEmpty()) {
                item { SectionLabel("Core Questions") }
                items(mustIds) { qId ->
                    ReviewRow(qId, answers[qId], onEdit = { onEdit(qId) })
                }
            }

            if (adaptiveIds.isNotEmpty()) {
                item { SectionLabel("Additional Questions") }
                items(adaptiveIds) { qId ->
                    ReviewRow(qId, answers[qId], onEdit = { onEdit(qId) })
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                if (!canCalculate) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WarningLight),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Please answer all required questions to calculate your assessment.",
                            modifier = Modifier.padding(14.dp),
                            color    = Warning,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                PrimaryButton(
                    text    = "Calculate My Assessment",
                    enabled = canCalculate,
                    onClick = onCalculate
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text     = label.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color    = Slate500,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ReviewRow(
    questionId: QuestionId,
    answer: QuestionAnswer?,
    onEdit: () -> Unit
) {
    val question = QuestionBank.all[questionId] ?: return
    val answerText = formatAnswer(question, answer)
    val isUnanswered = answer == null || !answer.isAnswered

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isUnanswered) WarningLight else White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onEdit)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = question.text,
                    fontSize = 13.sp,
                    color    = Slate700,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = answerText,
                    fontSize = 15.sp,
                    color = if (isUnanswered) Warning else Navy900,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint     = Slate500,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
        }
    }
}

private fun formatAnswer(question: Question, answer: QuestionAnswer?): String {
    if (answer == null || !answer.isAnswered) return "Not answered"
    return when (question.type) {
        QuestionType.NUMERIC_INPUT -> {
            val v = answer.numericValue ?: return "Not answered"
            when {
                question.unit?.contains("lakh", ignoreCase = true) == true -> "₹${v.toLong()} lakh"
                question.unit?.contains("year", ignoreCase = true) == true -> "${v.toInt()} years"
                question.unit?.contains("month", ignoreCase = true) == true -> "₹${formatNum(v)}/month"
                question.unit == "₹" -> "₹${formatNum(v)}"
                else -> "${v.toLong()} ${question.unit ?: ""}"
            }
        }
        QuestionType.TEXT_INPUT -> answer.textValue ?: ""
        QuestionType.BOOLEAN -> if (answer.selectedOption == "true") "Yes" else "No"
        else -> {
            val sel = answer.selectedOption ?: return "Not answered"
            question.options.find { it.value == sel }?.displayText ?: sel
        }
    }
}

private fun formatNum(v: Double): String {
    val lakh = 100_000.0
    return if (v >= lakh) "${String.format("%.0f", v / lakh)}L"
    else String.format("%,.0f", v)
}

private val mustQIds = setOf(
    QuestionId.LOAN_PURPOSE, QuestionId.LOAN_TYPE, QuestionId.AMOUNT_WANTED,
    QuestionId.NET_MONTHLY_INCOME, QuestionId.INCOME_TYPE, QuestionId.EXISTING_EMI,
    QuestionId.HOUSEHOLD_EXPENSES, QuestionId.AGE, QuestionId.CREDIT_SCORE, QuestionId.LOCATION
)
