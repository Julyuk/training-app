package com.trainingapp.data.model

data class UserPhysical(
    val age: Int,
    val weightKg: Float,
    val heightCm: Int,
    val sex: Sex = Sex.UNSPECIFIED
)
