package aaravgupta.youtubesdk.youtubeMusic.models

import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.objectOrNull
import aaravgupta.youtubesdk.youtube.models.string
import kotlinx.serialization.json.JsonObject


data class YouTubeMusicSection(
    val title: String,
    val items: List<YouTubeMusicItem>,
) {
    companion object {
        fun fromJson(data: JsonObject): YouTubeMusicSection? {
            val title = data.objectOrNull("header")
                ?.let { header ->
                    header.objectOrNull("musicCarouselShelfBasicHeaderRenderer")
                        ?: header.objectOrNull("musicShelfHeaderRenderer")
                }
                ?.objectOrNull("title")
                ?.arrayOrNull("runs")
                ?.firstOrNull()
                ?.let { run -> (run as? JsonObject)?.string("text") }
                .orEmpty()

            val contents = data.arrayOrNull("contents") ?: return null

            val items = contents.mapNotNull { element ->
                val itemDict = element as? JsonObject ?: return@mapNotNull null

                itemDict.objectOrNull("musicResponsiveListItemRenderer")
                    ?.let(YouTubeMusicSong::fromJson)
                    ?.let(YouTubeMusicItem::Song)
                    ?.let { return@mapNotNull it }

                val boxData = itemDict.objectOrNull("musicTwoRowItemRenderer") ?: return@mapNotNull null

                val pageType = boxData.objectOrNull("navigationEndpoint")
                    ?.objectOrNull("browseEndpoint")
                    ?.objectOrNull("browseEndpointContextSupportedConfigs")
                    ?.objectOrNull("browseEndpointContextMusicConfig")
                    ?.string("pageType")

                when (pageType) {
                    "MUSIC_PAGE_TYPE_ALBUM" -> {
                        YouTubeMusicAlbum.fromJson(boxData)?.let { return@mapNotNull YouTubeMusicItem.Album(it) }
                    }

                    "MUSIC_PAGE_TYPE_PLAYLIST" -> {
                        YouTubeMusicPlaylist.fromJson(boxData)?.let { return@mapNotNull YouTubeMusicItem.Playlist(it) }
                    }

                    "MUSIC_PAGE_TYPE_ARTIST" -> {
                        YouTubeMusicArtist.fromJson(boxData)?.let { return@mapNotNull YouTubeMusicItem.Artist(it) }
                    }
                }

                null
            }

            return YouTubeMusicSection(title = title, items = items)
        }
    }
}
