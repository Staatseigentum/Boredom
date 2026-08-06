package com.staatseigentum.kollaps.core.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReleaseFeedTest {

    /** Trimmed down to the fields the updater reads, but shaped like the real GitHub payload. */
    private fun releaseJson(
        tag: String = "v1.1.0",
        draft: Boolean = false,
        prerelease: Boolean = false,
        assetName: String = "kollaps-1.1.0.apk",
    ) = """
        {
          "url": "https://api.github.com/repos/Staatseigentum/Boredom/releases/1",
          "tag_name": "$tag",
          "name": "Kollaps $tag",
          "body": "Neue Stufe, schnellere Kollektoren.",
          "draft": $draft,
          "prerelease": $prerelease,
          "created_at": "2026-08-06T10:00:00Z",
          "assets": [
            {
              "name": "$assetName",
              "size": 9444385,
              "content_type": "application/vnd.android.package-archive",
              "browser_download_url": "https://github.com/Staatseigentum/Boredom/releases/download/$tag/$assetName"
            }
          ]
        }
    """.trimIndent()

    private val current = AppVersion.parse("1.0")!!

    @Test
    fun `reads the fields it needs and ignores the rest`() {
        val release = ReleaseFeed.parse(releaseJson())
        assertNotNull(release)
        assertEquals("v1.1.0", release.tag)
        assertEquals(1, release.assets.size)
        assertEquals(9_444_385L, release.assets.first().size)
    }

    @Test
    fun `offers a newer release with an apk attached`() {
        val update = ReleaseFeed.updateFrom(ReleaseFeed.parse(releaseJson()), current)
        assertNotNull(update)
        assertEquals(listOf(1, 1, 0), update.version.numbers)
        assertEquals("Kollaps v1.1.0", update.title)
        assertEquals("Neue Stufe, schnellere Kollektoren.", update.notes)
        assertEquals(9_444_385L, update.sizeBytes)
        assertEquals(
            "https://github.com/Staatseigentum/Boredom/releases/download/v1.1.0/kollaps-1.1.0.apk",
            update.downloadUrl,
        )
    }

    @Test
    fun `stays quiet when the release is not newer`() {
        val sameVersion = ReleaseFeed.parse(releaseJson(tag = "v1.0"))
        assertNull(ReleaseFeed.updateFrom(sameVersion, current))

        val older = ReleaseFeed.parse(releaseJson(tag = "v0.9"))
        assertNull(ReleaseFeed.updateFrom(older, current))
    }

    @Test
    fun `skips drafts and prereleases unless asked for them`() {
        assertNull(ReleaseFeed.updateFrom(ReleaseFeed.parse(releaseJson(draft = true)), current))
        assertNull(ReleaseFeed.updateFrom(ReleaseFeed.parse(releaseJson(prerelease = true)), current))

        val prerelease = ReleaseFeed.parse(releaseJson(prerelease = true))
        assertNotNull(ReleaseFeed.updateFrom(prerelease, current, allowPrereleases = true))
    }

    @Test
    fun `ignores a release without an apk`() {
        val noApk = ReleaseFeed.parse(releaseJson(assetName = "quellcode.zip"))
        assertNull(ReleaseFeed.updateFrom(noApk, current))
    }

    @Test
    fun `never throws on unreadable input`() {
        assertNull(ReleaseFeed.parse(null))
        assertNull(ReleaseFeed.parse(""))
        assertNull(ReleaseFeed.parse("nicht mal json"))
        assertNull(ReleaseFeed.parse("""{"message":"Not Found"}""")?.let {
            ReleaseFeed.updateFrom(it, current)
        })
        assertNull(ReleaseFeed.updateFrom(null, current))
    }

    @Test
    fun `an unknown installed version still lets an update through`() {
        // If we cannot read the installed version we would rather offer the update than hide it.
        val update = ReleaseFeed.updateFrom(ReleaseFeed.parse(releaseJson()), current = null)
        assertNotNull(update)
    }

    @Test
    fun `picks the newest release from a list`() {
        val list = "[${releaseJson(tag = "v1.0.3")}, ${releaseJson(tag = "v1.2.0")}, ${releaseJson(tag = "v1.1.0")}]"
        val newest = ReleaseFeed.parseList(list)
        assertNotNull(newest)
        assertEquals("v1.2.0", newest.tag)
    }

    @Test
    fun `a list without any usable release yields nothing`() {
        val list = "[${releaseJson(tag = "v1.2.0", draft = true)}]"
        assertNull(ReleaseFeed.parseList(list))
        assertNull(ReleaseFeed.parseList("[]"))
        assertNull(ReleaseFeed.parseList("kaputt"))
    }
}
