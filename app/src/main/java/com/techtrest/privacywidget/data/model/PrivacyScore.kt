package com.techtrest.privacywidget.data.model

data class PrivacyScore(
    val score: Int,
    val maxScore: Int = 100,
    val issues: List<PrivacyIssue>,
    val scanTimestamp: Long = System.currentTimeMillis()
) {
    val totalDeductions: Int
        get() = issues.sumOf { it.pointDeduction }

    val secureIssues: List<PrivacyIssue>
        get() = issues.filter { it.isSecure }

    val insecureIssues: List<PrivacyIssue>
        get() = issues.filter { !it.isSecure }

    val scorePercentage: Float
        get() = (score.toFloat() / maxScore.toFloat()) * 100f
}
