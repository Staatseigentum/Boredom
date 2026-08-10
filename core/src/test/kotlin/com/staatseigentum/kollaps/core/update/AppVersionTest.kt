package com.staatseigentum.kollaps.core.update

import kotlin.test.Test
import kotlin.test.assertFalse
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
    /**
     * Which steps are big enough to insist on.
     *
     * The rule decides whether a player can put an update off, so it is worth pinning: a patch is
     * optional, a minor or major is not, and something that is not newer at all is never either.
     */
    @Test
    fun `a new minor or major is a big step, a patch is not`() {
        fun step(from: String, to: String): Boolean =
            AppVersion.parse(to)!!.isBigStepFrom(AppVersion.parse(from)!!)

        assertTrue(step("2.5.1", "2.6.0"), "neue Minor")
        assertTrue(step("2.6.3", "3.0.0"), "neue Major")
        assertTrue(step("2.5", "2.6"), "auch ohne dritte Zahl")

        assertFalse(step("2.6.0", "2.6.1"), "Patch")
        assertFalse(step("2.6.0", "2.6.0"), "dieselbe")
        assertFalse(step("2.6.0", "2.5.9"), "rückwärts ist nie groß")
        assertFalse(step("2.6.1", "2.6.0"), "auch nicht als Patch rückwärts")
    }

    /** The `v` prefix and a build suffix must not change the answer. */
    @Test
    fun `how the version is written down does not decide whether it is mandatory`() {
        val installed = AppVersion.parse("2.5.1-debug")!!
        assertTrue(AppVersion.parse("v2.6.0")!!.isBigStepFrom(installed))
        assertFalse(AppVersion.parse("v2.5.2")!!.isBigStepFrom(installed))
    }

}
