package com.naze.nazever.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleInfoTest {

    @Test
    fun moduleNameIsSet() {
        assertEquals("core-domain", ModuleInfo.NAME)
    }
}
