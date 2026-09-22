package com.naze.nazever.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalSystemTest {

    @Test
    fun coreAnimalsExist() {
        val names = AnimalKind.entries.map { it.name }
        assertTrue(names.containsAll(listOf("CAT", "DOG", "RABBIT", "BIRD")))
    }

    @Test
    fun everyAnimalHasIdentityAndSound() {
        AnimalKind.entries.forEach { kind ->
            val identity = AnimalSystem.identity(kind)
            assertEquals(kind, identity.kind)
            assertTrue(identity.soundResourceName.startsWith("noti_"))
            assertTrue(kind.emoji.isNotBlank())
        }
    }

    @Test
    fun everyAnimalHasDistinctSound() {
        val sounds = AnimalKind.entries.map { AnimalSystem.identity(it).soundResourceName }
        assertEquals(sounds.size, sounds.toSet().size)
    }
}
