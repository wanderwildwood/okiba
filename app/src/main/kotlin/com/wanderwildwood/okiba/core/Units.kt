package com.wanderwildwood.okiba.core

/** How distances are shown: as the phone's region measures a street, or always one way. */
enum class Units {
    Auto, Metric, Imperial;

    /** Whether to show feet and miles, for a phone whose region is [region] (a country code). */
    fun imperial(region: String): Boolean = when (this) {
        Auto -> Geo.usesImperial(region.uppercase())
        Metric -> false
        Imperial -> true
    }

    companion object {
        fun of(name: String?): Units = entries.firstOrNull { it.name == name } ?: Auto
    }
}
