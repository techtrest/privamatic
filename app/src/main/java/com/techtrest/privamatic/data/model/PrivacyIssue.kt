package com.techtrest.privamatic.data.model

data class PrivacyIssue(
    val check: PrivacyCheck,
    val isSecure: Boolean,
    val currentStatus: String,
    val technicalDetails: String? = null,
    val customPointDeduction: Int? = null,
    val isSystemApp: Boolean = false,
    val flaggedPackages: List<String> = emptyList()
) {
    val pointDeduction: Int
        get() = if (isSecure) 0 else (customPointDeduction ?: check.pointDeduction)

    val recommendation: String
        get() = check.recommendation
}
