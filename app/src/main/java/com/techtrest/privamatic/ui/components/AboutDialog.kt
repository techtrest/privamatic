package com.techtrest.privamatic.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                        append("About ")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("PRIVA")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                        append("matic")
                    }
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = """
                    Privamatic gives your Android device a privacy score — a clear, actionable number that shows how exposed you are and what to fix.

                    All analysis runs locally on your device. No telemetry, no data collection, no network requests, no Google Play Services. Everything stays with you.

                    Built for privacy-conscious users, developers, and anyone who wants to understand and improve their Android privacy — no technical knowledge required.

                    Open source and available on F-Droid.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
