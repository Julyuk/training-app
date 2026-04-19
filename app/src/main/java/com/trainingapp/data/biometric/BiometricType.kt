package com.trainingapp.data.biometric

/** Hardware biometric sensor type available on this device. */
enum class BiometricType {
    FINGERPRINT,
    FACE,
    IRIS,
    MULTIPLE,   // device supports more than one type
    NONE;

    fun displayName(): String = when (this) {
        FINGERPRINT -> "Відбиток пальця"
        FACE        -> "Face ID"
        IRIS        -> "Сканування ока"
        MULTIPLE    -> "Біометрія"
        NONE        -> "Недоступно"
    }

    fun icon(): String = when (this) {
        FINGERPRINT -> "👆"
        FACE        -> "👤"
        IRIS        -> "👁"
        MULTIPLE    -> "🔐"
        NONE        -> "🚫"
    }
}
