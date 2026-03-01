package com.techtrest.privamatic.data.model

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

/**
 * Returns count of tracking/surveillance issues (excludes system security category).
 */
fun PrivacyScore.getTrackingIssuesCount() =
    issues.count { it.check !in PrivacyCategory.systemSecurityChecksSet && !it.isSecure && !it.check.isInformational }

/**
 * Returns count of security issues (system security category only).
 */
fun PrivacyScore.getSecurityIssuesCount() =
    issues.count { it.check in PrivacyCategory.systemSecurityChecksSet && !it.isSecure }
