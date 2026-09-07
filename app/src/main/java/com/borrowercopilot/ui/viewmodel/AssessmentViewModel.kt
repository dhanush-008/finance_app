package com.borrowercopilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.borrowercopilot.domain.engine.BorrowerAssessmentEngine
import com.borrowercopilot.domain.model.AssessmentResult
import com.borrowercopilot.domain.model.NegotiationCard
import com.borrowercopilot.domain.questions.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppScreen {
    WELCOME,
    QUESTIONNAIRE,
    REVIEW,
    RESULTS,
    NEGOTIATION_CARD
}

data class AssessmentState(
    val screen: AppScreen = AppScreen.WELCOME,
    val answers: Map<QuestionId, QuestionAnswer> = emptyMap(),
    val currentQuestionId: QuestionId? = null,
    val questionSequence: List<QuestionId> = emptyList(),
    val assessmentResult: AssessmentResult? = null,
    val negotiationCard: NegotiationCard? = null,
    val isCalculating: Boolean = false,
    val error: String? = null
) {
    val progressAnswered: Int get() {
        return questionSequence.count { answers[it]?.isAnswered == true }
    }
    val progressTotal: Int get() = questionSequence.size
    val canCalculate: Boolean get() = AdaptiveQuestionEngine.canCalculate(answers)
}

class AssessmentViewModel : ViewModel() {

    private val _state = MutableStateFlow(AssessmentState())
    val state: StateFlow<AssessmentState> = _state.asStateFlow()

    // ── Navigation ────────────────────────────────────────────────────────────

    fun startQuestionnaire() {
        val sequence = AdaptiveQuestionEngine.buildSequence(emptyMap())
        val firstQ   = sequence.firstOrNull()
        _state.update { it.copy(
            screen           = AppScreen.QUESTIONNAIRE,
            answers          = emptyMap(),
            questionSequence = sequence,
            currentQuestionId= firstQ,
            assessmentResult = null,
            negotiationCard  = null
        )}
    }

    fun startOver() {
        _state.update { AssessmentState() }
    }

    // ── Answer submission ─────────────────────────────────────────────────────

    fun answerQuestion(questionId: QuestionId, answer: QuestionAnswer) {
        val updatedAnswers = _state.value.answers + (questionId to answer)
        // Rebuild sequence — new answers may unlock additional questions
        val newSequence = AdaptiveQuestionEngine.buildSequence(updatedAnswers)
        val nextQ = AdaptiveQuestionEngine.nextQuestion(updatedAnswers)

        _state.update { it.copy(
            answers           = updatedAnswers,
            questionSequence  = newSequence,
            currentQuestionId = nextQ
        )}

        // If all questions in sequence answered, move to review
        if (nextQ == null) {
            _state.update { it.copy(screen = AppScreen.REVIEW) }
        }
    }

    fun skipQuestion(questionId: QuestionId) {
        // Mark as explicitly skipped but not blocking
        val updatedAnswers = _state.value.answers
        val newSequence = AdaptiveQuestionEngine.buildSequence(updatedAnswers)
        val currentIdx = newSequence.indexOf(questionId)
        val nextQ = if (currentIdx >= 0 && currentIdx + 1 < newSequence.size) {
            newSequence[currentIdx + 1]
        } else null

        _state.update { it.copy(
            questionSequence  = newSequence,
            currentQuestionId = nextQ
        )}

        if (nextQ == null) {
            _state.update { it.copy(screen = AppScreen.REVIEW) }
        }
    }

    fun navigateToPrevious() {
        val current = _state.value.currentQuestionId
        val sequence = _state.value.questionSequence
        val idx = sequence.indexOf(current)
        if (idx > 0) {
            _state.update { it.copy(currentQuestionId = sequence[idx - 1]) }
        } else if (idx == 0) {
            _state.update { it.copy(screen = AppScreen.WELCOME) }
        }
    }

    fun navigateToReview() {
        _state.update { it.copy(screen = AppScreen.REVIEW) }
    }

    fun editAnswer(questionId: QuestionId) {
        _state.update { it.copy(
            screen            = AppScreen.QUESTIONNAIRE,
            currentQuestionId = questionId
        )}
    }

    // ── Calculate ─────────────────────────────────────────────────────────────

    fun calculate() {
        val answers = _state.value.answers
        if (!AdaptiveQuestionEngine.canCalculate(answers)) {
            _state.update { it.copy(error = "Please answer the required questions first.") }
            return
        }
        _state.update { it.copy(isCalculating = true, error = null) }

        try {
            val profile  = AdaptiveQuestionEngine.buildProfile(answers)
            val request  = AdaptiveQuestionEngine.buildLoanRequest(answers)
            val result   = BorrowerAssessmentEngine.calculateAssessment(profile, request)
            val card     = BorrowerAssessmentEngine.buildNegotiationCard(result, request)

            _state.update { it.copy(
                isCalculating    = false,
                assessmentResult = result,
                negotiationCard  = card,
                screen           = AppScreen.RESULTS
            )}
        } catch (e: Exception) {
            _state.update { it.copy(
                isCalculating = false,
                error         = "Calculation error: ${e.message}"
            )}
        }
    }

    fun showNegotiationCard() {
        _state.update { it.copy(screen = AppScreen.NEGOTIATION_CARD) }
    }

    fun backToResults() {
        _state.update { it.copy(screen = AppScreen.RESULTS) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
