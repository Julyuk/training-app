package com.trainingapp

import com.trainingapp.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UserProfile] domain model.
 *
 * Verifies that all required field types are present, construction works
 * correctly, and derived values (BMI) round-trip as expected.
 */
class UserProfileModelTest {

    private fun makeProfile(
        name: String = "Юлія Українець",
        age: Int = 22,
        weightKg: Float = 58.5f,
        heightCm: Int = 165,
        fitnessGoal: String = "Підтримання форми",
        weeklyWorkoutTarget: Int = 4,
        isPremium: Boolean = false
    ) = UserProfile(
        name = name,
        age = age,
        weightKg = weightKg,
        heightCm = heightCm,
        fitnessGoal = fitnessGoal,
        weeklyWorkoutTarget = weeklyWorkoutTarget,
        isPremium = isPremium
    )

    // ── Field types ───────────────────────────────────────────────────────────

    @Test
    fun `name field is a String and stored correctly`() {
        val profile = makeProfile(name = "Тест Користувач")
        assertEquals("Тест Користувач", profile.name)
        assertTrue(profile.name is String)
    }

    @Test
    fun `age field is an Int and stored correctly`() {
        val profile = makeProfile(age = 30)
        assertEquals(30, profile.age)
    }

    @Test
    fun `weightKg field is a Float and stored correctly`() {
        val profile = makeProfile(weightKg = 72.3f)
        assertEquals(72.3f, profile.weightKg, 0.001f)
    }

    @Test
    fun `heightCm field is an Int and stored correctly`() {
        val profile = makeProfile(heightCm = 180)
        assertEquals(180, profile.heightCm)
    }

    @Test
    fun `isPremium field is a Boolean and defaults to false`() {
        val profile = makeProfile()
        assertFalse(profile.isPremium)
        assertTrue(profile.isPremium is Boolean)
    }

    @Test
    fun `isPremium can be set to true`() {
        val profile = makeProfile(isPremium = true)
        assertTrue(profile.isPremium)
    }

    @Test
    fun `fitnessGoal is a non-blank String`() {
        val profile = makeProfile(fitnessGoal = "Схуднення")
        assertEquals("Схуднення", profile.fitnessGoal)
        assertTrue(profile.fitnessGoal.isNotBlank())
    }

    @Test
    fun `weeklyWorkoutTarget is a positive Int`() {
        val profile = makeProfile(weeklyWorkoutTarget = 5)
        assertEquals(5, profile.weeklyWorkoutTarget)
        assertTrue(profile.weeklyWorkoutTarget > 0)
    }

    // ── Construction ──────────────────────────────────────────────────────────

    @Test
    fun `constructor sets all seven fields`() {
        val profile = makeProfile(
            name = "Тест",
            age = 28,
            weightKg = 65f,
            heightCm = 170,
            fitnessGoal = "Сила",
            weeklyWorkoutTarget = 3,
            isPremium = true
        )
        assertEquals("Тест", profile.name)
        assertEquals(28, profile.age)
        assertEquals(65f, profile.weightKg, 0.001f)
        assertEquals(170, profile.heightCm)
        assertEquals("Сила", profile.fitnessGoal)
        assertEquals(3, profile.weeklyWorkoutTarget)
        assertTrue(profile.isPremium)
    }

    // ── Data class copy ───────────────────────────────────────────────────────

    @Test
    fun `copy changes one field without affecting others`() {
        val original = makeProfile()
        val updated = original.copy(age = 25)
        assertEquals(25, updated.age)
        assertEquals(original.name, updated.name)
        assertEquals(original.weightKg, updated.weightKg, 0.001f)
        assertEquals(original.heightCm, updated.heightCm)
        assertEquals(original.isPremium, updated.isPremium)
    }

    @Test
    fun `two profiles with same data are equal`() {
        val a = makeProfile()
        val b = makeProfile()
        assertEquals(a, b)
    }

    // ── BMI derived value ─────────────────────────────────────────────────────

    @Test
    fun `bmi derived from weight and height is within normal range for sample data`() {
        val profile = makeProfile(weightKg = 58.5f, heightCm = 165)
        val heightM = profile.heightCm / 100f
        val bmi = profile.weightKg / (heightM * heightM)
        assertTrue("BMI should be in healthy range (18.5–25)", bmi in 18.5f..25f)
    }
}
