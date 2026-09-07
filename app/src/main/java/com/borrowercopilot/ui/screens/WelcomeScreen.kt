package com.borrowercopilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.borrowercopilot.ui.components.PrimaryButton
import com.borrowercopilot.ui.theme.*

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // Logo / brand mark
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Gold500),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "BC",
                    color      = Navy900,
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text       = "Borrower Copilot",
                color      = White,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = "Your independent guide to smart borrowing",
                color     = Slate200,
                fontSize  = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // 4 key outputs
            val outputs = listOf(
                "Should I borrow at all?" to "Get an honest verdict — including Don't Borrow.",
                "How much am I eligible for?" to "Safe amount vs. what a lender might sanction.",
                "What is a fair interest rate?" to "Indicative rate range based on your profile.",
                "What EMI should I agree to?" to "Your ceiling, with a tenure trade-off table."
            )

            outputs.forEach { (q, a) ->
                OutputRow(question = q, answer = a)
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(32.dp))

            // Disclaimer chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Navy700)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text      = "Self-assessment only. Not a lender decision or financial guarantee.",
                    color     = Slate200,
                    fontSize  = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // CTA
            Button(
                onClick  = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor   = Navy900
                )
            ) {
                Text(
                    text       = "Start Assessment",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text      = "Takes about 3–5 minutes  •  No data stored  •  Works offline",
                color     = Slate500,
                fontSize  = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun OutputRow(question: String, answer: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Navy800)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Gold500)
                .padding(top = 6.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = question, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(text = answer, color = Slate200, fontSize = 13.sp)
        }
    }
}
