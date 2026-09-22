package com.naze.nazever

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleInfoTest {

    @Test
    fun moduleNameIsSet() {
        assertEquals("app", ModuleInfo.NAME)
    }
}
