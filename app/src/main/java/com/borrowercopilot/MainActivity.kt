package com.borrowercopilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.borrowercopilot.ui.screens.*
import com.borrowercopilot.ui.theme.BorrowerCopilotTheme
import com.borrowercopilot.ui.viewmodel.AppScreen
import com.borrowercopilot.ui.viewmodel.AssessmentViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AssessmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BorrowerCopilotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsState()

                    // Error snackbar
                    if (state.error != null) {
                        Box(Modifier.fillMaxSize()) {
                            Snackbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                action = {
                                    TextButton(onClick = viewModel::dismissError) {
                                        Text("OK")
                                    }
                                }
                            ) {
                                Text(state.error ?: "")
                            }
                        }
                    }

                    when (state.screen) {
                        AppScreen.WELCOME -> WelcomeScreen(
                            onStart = viewModel::startQuestionnaire
                        )

                        AppScreen.QUESTIONNAIRE -> {
                            val qId = state.currentQuestionId
                            if (qId != null) {
                                QuestionnaireScreen(
                                    currentQuestionId  = qId,
                                    answers            = state.answers,
                                    questionSequence   = state.questionSequence,
                                    onAnswer           = viewModel::answerQuestion,
                                    onSkip             = viewModel::skipQuestion,
                                    onBack             = viewModel::navigateToPrevious,
                                    onNavigateToReview = viewModel::navigateToReview
                                )
                            } else {
                                // All questions answered — go to review
                                viewModel.navigateToReview()
                            }
                        }

                        AppScreen.REVIEW -> ReviewScreen(
                            answers          = state.answers,
                            questionSequence = state.questionSequence,
                            canCalculate     = state.canCalculate,
                            onEdit           = viewModel::editAnswer,
                            onCalculate      = viewModel::calculate,
                            onBack           = {
                                // Go back to last question
                                val lastQ = state.questionSequence.lastOrNull()
                                if (lastQ != null) viewModel.editAnswer(lastQ)
                            }
                        )

                        AppScreen.RESULTS -> {
                            val result = state.assessmentResult
                            if (result != null) {
                                ResultsScreen(
                                    result                = result,
                                    onShowNegotiationCard = viewModel::showNegotiationCard,
                                    onStartOver           = viewModel::startOver,
                                    onBack                = viewModel::navigateToReview
                                )
                            } else {
                                viewModel.navigateToReview()
                            }
                        }

                        AppScreen.NEGOTIATION_CARD -> {
                            val card = state.negotiationCard
                            if (card != null) {
                                NegotiationCardScreen(
                                    card        = card,
                                    onBack      = viewModel::backToResults,
                                    onStartOver = viewModel::startOver
                                )
                            } else {
                                viewModel.backToResults()
                            }
                        }
                    }

                    // Loading overlay
                    if (state.isCalculating) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
