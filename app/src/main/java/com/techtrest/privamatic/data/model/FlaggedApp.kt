package com.techtrest.privamatic.data.model

data class FlaggedApp(
    val packageName: String,
    val appName: String,
    val associatedCheck: PrivacyCheck,
    val isBlacklisted: Boolean,
    val isSystemApp: Boolean
) {
    companion object {
        private val BLACKLIST_PREFIXES = listOf(
            "com.google.", "com.facebook.", "com.instagram.",
            "com.whatsapp.", "com.microsoft.", "com.amazon."
        )

        fun isBlacklisted(packageName: String): Boolean =
            BLACKLIST_PREFIXES.any { packageName.startsWith(it) }
    }
}
