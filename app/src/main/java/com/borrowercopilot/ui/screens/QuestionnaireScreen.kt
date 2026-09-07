package com.borrowercopilot.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borrowercopilot.domain.questions.*
import com.borrowercopilot.ui.components.*
import com.borrowercopilot.ui.theme.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun QuestionnaireScreen(
    currentQuestionId: QuestionId,
    answers: Map<QuestionId, QuestionAnswer>,
    questionSequence: List<QuestionId>,
    onAnswer: (QuestionId, QuestionAnswer) -> Unit,
    onSkip: (QuestionId) -> Unit,
    onBack: () -> Unit,
    onNavigateToReview: () -> Unit
) {
    val question = QuestionBank.all[currentQuestionId] ?: return
    val existingAnswer = answers[currentQuestionId]

    val answeredCount = questionSequence.count { answers[it]?.isAnswered == true }
    val totalCount = questionSequence.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Navy800)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onNavigateToReview) {
                Text("Review", color = Navy800, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Progress ──────────────────────────────────────────────────────────
        QuestionProgressBar(
            answered = answeredCount,
            total    = totalCount,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Question content ──────────────────────────────────────────────────
        AnimatedContent(
            targetState = currentQuestionId,
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            },
            label = "question_transition"
        ) { qId ->
            val q = QuestionBank.all[qId] ?: return@AnimatedContent
            val existing = answers[qId]
            QuestionContent(
                question       = q,
                existingAnswer = existing,
                onAnswer       = { ans -> onAnswer(qId, ans) },
                onSkip         = if (q.isOptional) {{ onSkip(qId) }} else null
            )
        }
    }
}

@Composable
private fun QuestionContent(
    question: Question,
    existingAnswer: QuestionAnswer?,
    onAnswer: (QuestionAnswer) -> Unit,
    onSkip: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text       = question.text,
            style      = MaterialTheme.typography.headlineLarge,
            color      = Navy900
        )

        if (question.subText != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text  = question.subText,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate700
            )
        }

        if (question.whyItMatters != null) {
            Spacer(Modifier.height(8.dp))
            ExpandableExplanation(
                label       = "Why does this matter?",
                explanation = question.whyItMatters
            )
        }

        Spacer(Modifier.height(24.dp))

        when (question.type) {
            QuestionType.SINGLE_CHOICE, QuestionType.BOOLEAN -> {
                SingleChoiceInput(
                    options        = if (question.type == QuestionType.BOOLEAN) {
                        listOf(
                            QuestionOption("true",  "Yes"),
                            QuestionOption("false", "No")
                        )
                    } else question.options,
                    selectedValue  = existingAnswer?.selectedOption,
                    onSelect       = { value ->
                        onAnswer(QuestionAnswer(question.id, selectedOption = value))
                    }
                )
            }
            QuestionType.NUMERIC_INPUT -> {
                NumericInput(
                    question       = question,
                    existingAnswer = existingAnswer,
                    onAnswer       = onAnswer
                )
            }
            QuestionType.TEXT_INPUT -> {
                TextInput(
                    question       = question,
                    existingAnswer = existingAnswer,
                    onAnswer       = onAnswer
                )
            }
            else -> {}
        }

        if (onSkip != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick  = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Skip this question", color = Slate500, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SingleChoiceInput(
    options: List<QuestionOption>,
    selectedValue: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            OptionChip(
                text     = option.displayText,
                hint     = option.hint,
                selected = selectedValue == option.value,
                onClick  = { onSelect(option.value) }
            )
        }
    }
}

@Composable
private fun NumericInput(
    question: Question,
    existingAnswer: QuestionAnswer?,
    onAnswer: (QuestionAnswer) -> Unit
) {
    var textValue by remember(question.id) {
        mutableStateOf(existingAnswer?.numericValue?.toLong()?.toString() ?: "")
    }
    val isValid = textValue.isNotBlank() && textValue.toDoubleOrNull() != null

    NumericInputField(
        value         = textValue,
        onValueChange = { textValue = it },
        label         = question.text,
        unit          = question.unit,
        placeholder   = question.placeholder ?: ""
    )
    Spacer(Modifier.height(20.dp))
    PrimaryButton(
        text    = "Continue",
        enabled = isValid,
        onClick = {
            val num = textValue.toDoubleOrNull()
            if (num != null) {
                onAnswer(QuestionAnswer(question.id, numericValue = num))
            }
        }
    )
}

@Composable
private fun TextInput(
    question: Question,
    existingAnswer: QuestionAnswer?,
    onAnswer: (QuestionAnswer) -> Unit
) {
    var textValue by remember(question.id) {
        mutableStateOf(existingAnswer?.textValue ?: "")
    }

    TextInputField(
        value         = textValue,
        onValueChange = { textValue = it },
        label         = question.text,
        placeholder   = question.placeholder ?: ""
    )
    Spacer(Modifier.height(20.dp))
    PrimaryButton(
        text    = "Continue",
        enabled = textValue.isNotBlank(),
        onClick = {
            onAnswer(QuestionAnswer(question.id, textValue = textValue))
        }
    )
}
