package de.lootz.borderline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleRegistryTest {
    @Test
    fun overlayDependsOnAccessibility() {
        assertTrue(ModuleRegistry.descriptor(ModuleId.OVERLAY).dependsOn.contains(ModuleId.ACCESSIBILITY))
    }

    @Test
    fun shortcutsDependsOnOverlay() {
        assertTrue(ModuleRegistry.descriptor(ModuleId.SHORTCUTS).dependsOn.contains(ModuleId.OVERLAY))
    }

    @Test
    fun accessibilityIsRequired() {
        assertTrue(ModuleRegistry.descriptor(ModuleId.ACCESSIBILITY).required)
    }

    @Test
    fun overlayIsNotRequired() {
        assertFalse(ModuleRegistry.descriptor(ModuleId.OVERLAY).required)
    }

    @Test
    fun allModulesRegistered() {
        assertEquals(ModuleId.entries.size, ModuleRegistry.all.size)
        ModuleId.entries.forEach { id ->
            assertTrue(ModuleRegistry.all.any { it.id == id })
        }
    }
}
