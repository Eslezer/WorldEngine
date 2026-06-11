package com.example.worldengine

import com.example.worldengine.domain.model.GenerationSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationSettingsTest {

    @Test
    fun defaultSettings_areFreeGeneration() {
        assertTrue(GenerationSettings().isFreeGeneration)
    }

    @Test
    fun squarePresetAtDefaultSteps_isFree() {
        assertTrue(GenerationSettings(width = 1024, height = 1024, steps = 28).isFreeGeneration)
    }

    @Test
    fun overStepLimit_isNotFree() {
        assertFalse(GenerationSettings(steps = 50).isFreeGeneration)
    }

    @Test
    fun overResolutionLimit_isNotFree() {
        assertFalse(GenerationSettings(width = 1216, height = 1216).isFreeGeneration)
    }
}
