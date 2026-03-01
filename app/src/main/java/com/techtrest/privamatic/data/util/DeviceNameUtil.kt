package com.techtrest.privamatic.data.util

import android.os.Build

object DeviceNameUtil {

    private val EXACT_NAME_MAP: Map<String, String> = mapOf(
        // Samsung S series
        "sm-g950f"  to "Galaxy S8",
        "sm-g950u"  to "Galaxy S8",
        "sm-g955f"  to "Galaxy S8+",
        "sm-g960f"  to "Galaxy S9",
        "sm-g960u"  to "Galaxy S9",
        "sm-g965f"  to "Galaxy S9+",
        "sm-g970f"  to "Galaxy S10e",
        "sm-g973f"  to "Galaxy S10",
        "sm-g975f"  to "Galaxy S10+",
        "sm-g980f"  to "Galaxy S20",
        "sm-g980u"  to "Galaxy S20",
        "sm-g985f"  to "Galaxy S20+",
        "sm-g988b"  to "Galaxy S20 Ultra",
        "sm-g991b"  to "Galaxy S21",
        "sm-g991u"  to "Galaxy S21",
        "sm-g996b"  to "Galaxy S21+",
        "sm-g998b"  to "Galaxy S21 Ultra",
        "sm-s901b"  to "Galaxy S22",
        "sm-s901u"  to "Galaxy S22",
        "sm-s906b"  to "Galaxy S22+",
        "sm-s908b"  to "Galaxy S22 Ultra",
        "sm-s911b"  to "Galaxy S23",
        "sm-s911u"  to "Galaxy S23",
        "sm-s916b"  to "Galaxy S23+",
        "sm-s918b"  to "Galaxy S23 Ultra",
        "sm-s921b"  to "Galaxy S24",
        "sm-s921u"  to "Galaxy S24",
        "sm-s926b"  to "Galaxy S24+",
        "sm-s928b"  to "Galaxy S24 Ultra",
        // Samsung A series
        "sm-a515f"  to "Galaxy A51",
        "sm-a525f"  to "Galaxy A52",
        "sm-a536b"  to "Galaxy A53 5G",
        "sm-a546b"  to "Galaxy A54 5G",
        "sm-a556b"  to "Galaxy A55 5G",
        // LG exact
        "lg-d802"   to "LG G2",
        "lg-d855"   to "LG G3",
        "lg-h815"   to "LG G4",
        "lg-h850"   to "LG G5",
        "lg-h870"   to "LG G6",
        "lgh870"    to "LG G6",
        "lg-g710"   to "LG G7 ThinQ",
        "lgm-g710"  to "LG G7 ThinQ",
        "lg-h932"   to "LG V30",
        "lg-v405"   to "LG V40 ThinQ",
        "lg-v500"   to "LG V50 ThinQ",
        "lg-h791"   to "LG Nexus 5X",
    )

    private val PREFIX_NAME_MAP: Map<String, String> = mapOf(
        "lg-d80"    to "LG G2",
        "lg-d85"    to "LG G3",
        "lg-h81"    to "LG G4",
        "lg-h85"    to "LG G5",
        "lg-h87"    to "LG G6",
        "lgh87"     to "LG G6",
        "lg-g710"   to "LG G7 ThinQ",
        "lgm-g710"  to "LG G7 ThinQ",
        "lg-h93"    to "LG V30",
        "lg-v40"    to "LG V40 ThinQ",
        "lg-v50"    to "LG V50 ThinQ",
        "sm-g950"   to "Galaxy S8",
        "sm-g955"   to "Galaxy S8+",
        "sm-g960"   to "Galaxy S9",
        "sm-g965"   to "Galaxy S9+",
    )

    fun getMarketingName(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL
        val modelLower = model.lowercase()

        // Pixel devices already report marketing names in Build.MODEL
        if (manufacturer == "google") {
            return model.replaceFirstChar { it.uppercase() }
        }

        EXACT_NAME_MAP[modelLower]?.let { return it }

        for ((prefix, name) in PREFIX_NAME_MAP) {
            if (modelLower.startsWith(prefix)) return name
        }

        return Build.MANUFACTURER.replaceFirstChar { it.uppercase() } + " " + model
    }
}
