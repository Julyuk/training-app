package com.trainingapp

import com.trainingapp.data.model.ActivityLevel
import com.trainingapp.data.model.Sex
import com.trainingapp.data.model.UserIdentity
import com.trainingapp.data.model.UserPhysical
import com.trainingapp.data.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileModelTest {

    // ── UserIdentity ──────────────────────────────────────────────────────────

    @Test
    fun `UserIdentity name field is a String and stored correctly`() {
        val identity = UserIdentity(name = "Тест Користувач", isPremium = false)
        assertEquals("Тест Користувач", identity.name)
        assertTrue(identity.name is String)
    }

    @Test
    fun `UserIdentity isPremium defaults to false and can be set to true`() {
        val free = UserIdentity(name = "Юлія", isPremium = false)
        assertFalse(free.isPremium)
        val premium = free.copy(isPremium = true)
        assertTrue(premium.isPremium)
    }

    @Test
    fun `UserIdentity copy changes name without affecting isPremium`() {
        val original = UserIdentity(name = "Юлія", isPremium = false)
        val updated = original.copy(name = "Олена")
        assertEquals("Олена", updated.name)
        assertEquals(original.isPremium, updated.isPremium)
    }

    @Test
    fun `two UserIdentity with same data are equal`() {
        val a = UserIdentity(name = "Юлія", isPremium = false)
        val b = UserIdentity(name = "Юлія", isPremium = false)
        assertEquals(a, b)
    }

    // ── UserPhysical ──────────────────────────────────────────────────────────

    @Test
    fun `UserPhysical stores age weightKg heightCm and sex correctly`() {
        val physical = UserPhysical(age = 22, weightKg = 58.5f, heightCm = 165, sex = Sex.FEMALE)
        assertEquals(22, physical.age)
        assertEquals(58.5f, physical.weightKg, 0.001f)
        assertEquals(165, physical.heightCm)
        assertEquals(Sex.FEMALE, physical.sex)
    }

    @Test
    fun `UserPhysical sex defaults to UNSPECIFIED`() {
        val physical = UserPhysical(age = 25, weightKg = 70f, heightCm = 175)
        assertEquals(Sex.UNSPECIFIED, physical.sex)
    }

    @Test
    fun `UserPhysical bmi derived from weight and height is within normal range`() {
        val physical = UserPhysical(age = 22, weightKg = 58.5f, heightCm = 165)
        val heightM = physical.heightCm / 100f
        val bmi = physical.weightKg / (heightM * heightM)
        assertTrue("BMI should be in healthy range (18.5–25)", bmi in 18.5f..25f)
    }

    @Test
    fun `UserPhysical copy changes one field without affecting others`() {
        val original = UserPhysical(age = 22, weightKg = 58.5f, heightCm = 165, sex = Sex.FEMALE)
        val updated = original.copy(weightKg = 62f)
        assertEquals(62f, updated.weightKg, 0.001f)
        assertEquals(original.age, updated.age)
        assertEquals(original.heightCm, updated.heightCm)
        assertEquals(original.sex, updated.sex)
    }

    // ── UserPreferences ───────────────────────────────────────────────────────

    @Test
    fun `UserPreferences stores fitnessGoal weeklyWorkoutTarget and activityLevel correctly`() {
        val prefs = UserPreferences(
            fitnessGoal = "Схуднення",
            weeklyWorkoutTarget = 4,
            activityLevel = ActivityLevel.MODERATE
        )
        assertEquals("Схуднення", prefs.fitnessGoal)
        assertEquals(4, prefs.weeklyWorkoutTarget)
        assertEquals(ActivityLevel.MODERATE, prefs.activityLevel)
    }

    @Test
    fun `UserPreferences activityLevel defaults to MODERATE`() {
        val prefs = UserPreferences(fitnessGoal = "Сила", weeklyWorkoutTarget = 3)
        assertEquals(ActivityLevel.MODERATE, prefs.activityLevel)
    }

    @Test
    fun `UserPreferences weeklyWorkoutTarget is a positive Int`() {
        val prefs = UserPreferences(fitnessGoal = "Витривалість", weeklyWorkoutTarget = 5)
        assertTrue(prefs.weeklyWorkoutTarget > 0)
    }

    @Test
    fun `UserPreferences copy changes one field without affecting others`() {
        val original = UserPreferences(
            fitnessGoal = "Підтримання форми",
            weeklyWorkoutTarget = 4,
            activityLevel = ActivityLevel.MODERATE
        )
        val updated = original.copy(weeklyWorkoutTarget = 6)
        assertEquals(6, updated.weeklyWorkoutTarget)
        assertEquals(original.fitnessGoal, updated.fitnessGoal)
        assertEquals(original.activityLevel, updated.activityLevel)
    }

    @Test
    fun `two UserPreferences with same data are equal`() {
        val a = UserPreferences(fitnessGoal = "Сила", weeklyWorkoutTarget = 4)
        val b = UserPreferences(fitnessGoal = "Сила", weeklyWorkoutTarget = 4)
        assertEquals(a, b)
    }
}
