package com.staatseigentum.kollaps.core.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun `parses the three shapes the version arrives in`() {
        assertEquals(listOf(1, 2, 0), AppVersion.parse("1.2.0")?.numbers)
        assertEquals(listOf(1, 2, 0), AppVersion.parse("v1.2.0")?.numbers)
        assertEquals(listOf(1, 2, 0), AppVersion.parse("1.2.0-debug")?.numbers)
        assertEquals(listOf(1, 0), AppVersion.parse("  1.0  ")?.numbers)
    }

    @Test
    fun `keeps the original text for display`() {
        assertEquals("v1.2.0", AppVersion.parse("v1.2.0")?.raw)
        assertEquals("1.2.0", AppVersion.parse("v1.2.0")?.canonical())
    }

    @Test
    fun `rejects text without a number`() {
        assertNull(AppVersion.parse(null))
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse("   "))
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse("v"))
    }

    @Test
    fun `orders versions by their numbers`() {
        assertTrue(version("1.2.0") > version("1.1.9"))
        assertTrue(version("2.0.0") > version("1.99.99"))
        assertTrue(version("1.10.0") > version("1.9.0"), "10 muss größer als 9 sein, nicht kleiner")
        assertTrue(version("1.0.1") > version("1.0"))
    }

    @Test
    fun `treats missing components as zero`() {
        assertEquals(0, version("1.2").compareTo(version("1.2.0")))
        assertEquals(0, version("v1.0").compareTo(version("1.0.0-debug")))
    }

    @Test
    fun `a debug build is not newer than the release it was built from`() {
        // The debug variant carries a "-debug" suffix, which must not read as a different version.
        assertTrue(version("1.0-debug") <= version("1.0"))
        assertTrue(version("1.1") > version("1.0-debug"))
    }

    private fun version(text: String) = AppVersion.parse(text)!!
}
