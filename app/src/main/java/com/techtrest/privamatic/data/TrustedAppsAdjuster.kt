package com.techtrest.privamatic.data

import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator

object TrustedAppsAdjuster {

    fun computeAdjustedScore(rawScore: PrivacyScore, trusted: Set<String>): PrivacyScore {
        if (trusted.isEmpty()) return rawScore
        val adjustedIssues = rawScore.issues.map { adjustIssueForTrust(it, trusted) }
        return PrivacyScoreCalculator.calculateScore(adjustedIssues, rawScore.manualCheckPoints)
    }

    private fun adjustIssueForTrust(issue: PrivacyIssue, trusted: Set<String>): PrivacyIssue {
        if (issue.isSecure || issue.flaggedPackages.isEmpty()) return issue
        val untrustedCount = issue.flaggedPackages.count { it !in trusted }
        if (untrustedCount == issue.flaggedPackages.size) return issue
        return issue.copy(customPointDeduction = issue.check.cappedDeductionFor(untrustedCount))
    }
}
