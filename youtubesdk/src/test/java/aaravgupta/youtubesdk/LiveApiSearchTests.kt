package aaravgupta.youtubesdk

import aaravgupta.youtubesdk.youtube.YouTubeClient
import aaravgupta.youtubesdk.youtube.models.YouTubeItem
import aaravgupta.youtubesdk.youtubeMusic.YouTubeMusicClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.time.Instant

class LiveApiSearchTests {

    @Test
    fun liveSearchQueriesAndCaptureRealResponse() = runBlocking {
        Assume.assumeTrue(
            "Set YOUTUBESDK_RUN_LIVE_TESTS=true (env or JVM property) to run live API tests",
            liveTestsEnabled(),
        )

        val youtubeClient = YouTubeClient()
        val musicClient = YouTubeMusicClient()

        val youtubeQuery = "lofi hip hop beats"
        val musicQuery = "coldplay yellow"

        val youtubeResult = youtubeClient.search(youtubeQuery)
        val musicResult = musicClient.search(musicQuery)

        assertTrue("Expected non-empty YouTube live search results", youtubeResult.items.isNotEmpty())
        assertTrue("Expected non-empty YouTube Music live search results", musicResult.isNotEmpty())

        val report = buildString {
            appendLine("timestamp=${Instant.now()}")
            appendLine("youtube.query=$youtubeQuery")
            appendLine("youtube.count=${youtubeResult.items.size}")
            appendLine("youtube.continuationTokenPresent=${!youtubeResult.continuationToken.isNullOrBlank()}")
            appendLine("youtube.sample=")
            youtubeResult.items.take(5).forEachIndexed { index, item ->
                appendLine("  ${index + 1}. ${describeYouTubeItem(item)}")
            }

            appendLine("music.query=$musicQuery")
            appendLine("music.count=${musicResult.size}")
            appendLine("music.sample=")
            musicResult.take(5).forEachIndexed { index, song ->
                appendLine("  ${index + 1}. song(id=${song.id}, title=${song.title}, artists=${song.artistsDisplay})")
            }
        }

        val outputFile = resolveOutputFile()
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(report)

        println(report)
        println("Live API output written to: ${outputFile.absolutePath}")

        assertTrue("Expected output report to be generated", outputFile.exists())
    }

    private fun liveTestsEnabled(): Boolean {
        val env = System.getenv("YOUTUBESDK_RUN_LIVE_TESTS")
        val prop = System.getProperty("YOUTUBESDK_RUN_LIVE_TESTS")
        return env.equals("true", ignoreCase = true) || prop.equals("true", ignoreCase = true)
    }

    private fun resolveOutputFile(): File {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val moduleDir = if (userDir.name == "youtubesdk") userDir else File(userDir, "youtubesdk")
        return File(moduleDir, "build/reports/live-api-search-output.txt")
    }

    private fun describeYouTubeItem(item: YouTubeItem): String {
        return when (item) {
            is YouTubeItem.Video -> "video(id=${item.value.id}, title=${item.value.title}, author=${item.value.author})"
            is YouTubeItem.Channel -> "channel(id=${item.value.id}, title=${item.value.title})"
            is YouTubeItem.Playlist -> "playlist(id=${item.value.id}, title=${item.value.title})"
            is YouTubeItem.Song -> "song(id=${item.value.id}, title=${item.value.title}, artists=${item.value.artistsDisplay})"
            is YouTubeItem.Shelf -> "shelf(title=${item.value.title}, itemCount=${item.value.items.size})"
        }
    }
}