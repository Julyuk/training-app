package com.trainingapp.data.model

/**
 * Biological sex used for the Mifflin-St Jeor BMR formula.
 * [bmrOffset] is added to the gender-neutral base:
 *   Male:        10W + 6.25H − 5A + 5
 *   Female:      10W + 6.25H − 5A − 161
 *   Unspecified: gender-neutral average offset (−78)
 */
enum class Sex(val label: String, val bmrOffset: Float) {
    MALE("Чоловіча", 5f),
    FEMALE("Жіноча", -161f),
    UNSPECIFIED("Не вказано", -78f)
}
