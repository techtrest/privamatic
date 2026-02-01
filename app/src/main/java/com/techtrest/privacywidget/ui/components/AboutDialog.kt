package com.techtrest.privacywidget.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "About Privacy Guard",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = """
                    Privacy Guard helps you understand and improve your device's privacy posture through comprehensive auditing of system settings and installed applications.

                    Built with privacy-first principles, this app performs all analysis locally on your device with no telemetry or data collection. It identifies privacy risks from invasive apps and system misconfigurations, providing actionable recommendations to strengthen your digital privacy.

                    Designed for privacy-conscious users, GrapheneOS enthusiasts, and anyone seeking to de-Google their Android experience.

                    Open source and available on F-Droid.
                """.trimIndent(),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
