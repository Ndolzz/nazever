package com.naze.nazever.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleInfoTest {

    @Test
    fun moduleNameIsSet() {
        assertEquals("core-network", ModuleInfo.NAME)
    }
}
