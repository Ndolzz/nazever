package com.naze.nazever.core.security

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleInfoTest {

    @Test
    fun moduleNameIsSet() {
        assertEquals("core-security", ModuleInfo.NAME)
    }
}
