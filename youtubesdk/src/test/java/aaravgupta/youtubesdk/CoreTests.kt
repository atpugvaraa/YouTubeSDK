package aaravgupta.youtubesdk

import aaravgupta.youtubesdk.shared.ClientConfig
import aaravgupta.youtubesdk.shared.InnerTubeContext
import aaravgupta.youtubesdk.youtube.models.YouTubeChannel
import aaravgupta.youtubesdk.youtube.models.objectOrNull
import aaravgupta.youtubesdk.youtube.models.string
import aaravgupta.youtubesdk.youtube.models.YouTubeItem
import aaravgupta.youtubesdk.youtube.models.YouTubeSearchResult
import aaravgupta.youtubesdk.youtube.models.YouTubeShelf
import aaravgupta.youtubesdk.youtube.models.YouTubeVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CoreTests {

    @Test
    fun contextGeneration() {
        val config = ClientConfig.android
        val context = InnerTubeContext(client = config, gl = "IN", hl = "en")

        val body = context.body
        val contextDict = body.objectOrNull("context")
        val clientDict = contextDict?.objectOrNull("client")

        assertNotNull("The body should contain a context.client object", clientDict)
        assertEquals("ANDROID", clientDict?.string("clientName"))
        assertEquals("IN", clientDict?.string("gl"))
        assertEquals(config.userAgent, context.headers["User-Agent"])
    }

    @Test
    fun chartsAnalyticsContextContract() {
        val context = InnerTubeContext(client = ClientConfig.webMusicAnalytics)
        val body = context.body
        val contextDict = body.objectOrNull("context")
        val clientDict = contextDict?.objectOrNull("client")

        assertEquals("31", context.headers["X-YouTube-Client-Name"])
        assertEquals("2.0", context.headers["X-YouTube-Client-Version"])
        assertEquals("WEB_MUSIC_ANALYTICS", clientDict?.string("clientName"))
        assertEquals("2.0", clientDict?.string("clientVersion"))
    }

    @Test
    fun searchResultParsesFromSupportedItems() {
        val video = YouTubeVideo(
            id = "video-id",
            title = "Video",
            viewCount = "10",
            author = "Author",
            channelId = "channel-id",
            description = "",
            lengthInSeconds = "60",
            thumbnailUrl = null,
            streamingData = null,
            captions = null,
        )

        val channel = YouTubeChannel(
            id = "channel-id",
            title = "Channel",
            thumbnailUrl = null,
            subscriberCount = null,
            videoCount = null,
        )

        val videoResult = YouTubeSearchResult.fromItem(YouTubeItem.Video(video))
        val channelResult = YouTubeSearchResult.fromItem(YouTubeItem.Channel(channel))

        assertEquals("video-id", videoResult?.id)
        assertEquals("channel-id", channelResult?.id)
    }

    @Test
    fun searchResultSkipsUnsupportedItems() {
        val shelf = YouTubeShelf(title = "Shelf", items = emptyList())
        val result = YouTubeSearchResult.fromItem(YouTubeItem.Shelf(shelf))
        assertNull(result)
    }
}
