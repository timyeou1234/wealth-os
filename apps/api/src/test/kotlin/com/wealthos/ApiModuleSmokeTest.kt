package com.wealthos

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiModuleSmokeTest {
    @Test
    fun `tests run on the configured Java toolchain`() {
        assertEquals(21, Runtime.version().feature())
    }
}
