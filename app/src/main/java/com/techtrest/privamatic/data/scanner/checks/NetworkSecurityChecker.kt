package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue

class NetworkSecurityChecker(private val context: Context) {

    fun checkVpnConnection(): PrivacyIssue {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return PrivacyIssue(
                    check = PrivacyCheck.VPN_CONNECTION,
                    isSecure = false,
                    currentStatus = "Unable to determine",
                    technicalDetails = "Connectivity service not available on this device"
                )
            val hasVpn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } else {
                false
            }

            PrivacyIssue(
                check = PrivacyCheck.VPN_CONNECTION,
                isSecure = hasVpn,
                currentStatus = if (hasVpn) "Active" else "Not active",
                technicalDetails = if (hasVpn)
                    "Checked using NetworkCapabilities.TRANSPORT_VPN"
                else
                    "Checked using NetworkCapabilities.TRANSPORT_VPN\nTip: consider enabling Always-On VPN in Settings → Network → VPN"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.VPN_CONNECTION,
                isSecure = false,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    fun checkPrivateDns(): PrivacyIssue {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val privateDnsMode = Settings.Global.getString(
                    context.contentResolver,
                    "private_dns_mode"
                )

                val rawIssue = when (privateDnsMode) {
                    "hostname" -> {
                        val hostname = Settings.Global.getString(
                            context.contentResolver,
                            "private_dns_specifier"
                        )
                        if (!hostname.isNullOrEmpty()) {
                            PrivacyIssue(
                                check = PrivacyCheck.PRIVATE_DNS,
                                isSecure = true,
                                currentStatus = "Custom hostname configured",
                                technicalDetails = "Hostname: $hostname"
                            )
                        } else {
                            PrivacyIssue(
                                check = PrivacyCheck.PRIVATE_DNS,
                                isSecure = false,
                                currentStatus = "Hostname mode set but no hostname configured",
                                technicalDetails = "Hostname mode is active but no hostname is provided"
                            )
                        }
                    }
                    "opportunistic", null -> PrivacyIssue(
                        check = PrivacyCheck.PRIVATE_DNS,
                        isSecure = true,
                        currentStatus = "Automatic mode (opportunistic)",
                        technicalDetails = "Automatic mode"
                    )
                    "off" -> PrivacyIssue(
                        check = PrivacyCheck.PRIVATE_DNS,
                        isSecure = false,
                        currentStatus = "Not configured",
                        technicalDetails = "Disabled"
                    )
                    else -> PrivacyIssue(
                        check = PrivacyCheck.PRIVATE_DNS,
                        isSecure = false,
                        currentStatus = "Not configured",
                        technicalDetails = "Unknown mode: $privateDnsMode"
                    )
                }

                // VPN tunnels DNS through the encrypted connection — don't penalise if VPN is active
                if (!rawIssue.isSecure && isVpnActive()) {
                    PrivacyIssue(
                        check = PrivacyCheck.PRIVATE_DNS,
                        isSecure = true,
                        currentStatus = "VPN active — DNS handled by VPN",
                        technicalDetails = "Private DNS is not configured, but an active VPN handles DNS routing"
                    )
                } else {
                    rawIssue
                }
            } else {
                // Private DNS not available before Android 9
                PrivacyIssue(
                    check = PrivacyCheck.PRIVATE_DNS,
                    isSecure = true, // Don't penalize older devices
                    currentStatus = "Not available on Android < 9",
                    technicalDetails = "Private DNS requires Android 9 (API 28) or higher"
                )
            }
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.PRIVATE_DNS,
                isSecure = false,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    private fun isVpnActive(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if advertising ID has been manually verified as deleted.
     * Reads a persisted boolean from SharedPreferences written by AdIdVerificationScreen.
     * Auto-passes on devices without Google Play Services — the setting does not exist.
     */
    fun checkAdvertisingId(): PrivacyIssue {
        val isGmsInstalled = try {
            context.packageManager.getApplicationInfo(GMS_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        if (!isGmsInstalled) {
            return PrivacyIssue(
                check = PrivacyCheck.ADVERTISING_ID,
                isSecure = true,
                currentStatus = context.getString(R.string.status_ad_id_not_applicable)
            )
        }

        val prefs = context.getSharedPreferences(AD_ID_PREFS_NAME, Context.MODE_PRIVATE)
        val isVerified = prefs.getBoolean(KEY_AD_ID_VERIFIED, false)
        return if (isVerified) {
            PrivacyIssue(
                check = PrivacyCheck.ADVERTISING_ID,
                isSecure = true,
                currentStatus = "Disabled (self-reported)"
            )
        } else {
            PrivacyIssue(
                check = PrivacyCheck.ADVERTISING_ID,
                isSecure = false,
                currentStatus = "Unknown — verify manually"
            )
        }
    }

    companion object {
        internal const val AD_ID_PREFS_NAME = "ad_id_prefs"
        internal const val KEY_AD_ID_VERIFIED = "ad_id_verified"
        internal const val KEY_AD_ID_TIMESTAMP = "ad_id_verified_timestamp"
        private const val GMS_PACKAGE = "com.google.android.gms"
    }
}
