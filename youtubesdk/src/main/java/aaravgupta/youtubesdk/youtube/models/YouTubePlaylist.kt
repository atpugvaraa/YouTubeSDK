package aaravgupta.youtubesdk.youtube.models

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class YouTubePlaylist(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val videoCount: String?,
    val author: String?,
) {
    companion object {
        fun fromJson(data: JsonObject): YouTubePlaylist? {
            val id = data.string("playlistId") ?: return null

            val title = data.textObject("title") ?: "Unknown Playlist"

            val thumbnailUrl = data.arrayOrNull("thumbnails")
                ?.lastObjectOrNull()
                ?.string("url")

            val videoCount = data.string("videoCount")
                ?: data.objectOrNull("videoCountText")?.string("simpleText")

            val author = data.objectOrNull("ownerText")
                ?.arrayOrNull("runs")
                ?.firstOrNull()
                ?.jsonObject
                ?.string("text")

            return YouTubePlaylist(
                id = id,
                title = title,
                thumbnailUrl = thumbnailUrl,
                videoCount = videoCount,
                author = author,
            )
        }
    }
}
