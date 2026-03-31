package aaravgupta.youtubesdk.youtubeMusic.models

import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.string
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class YouTubeMusicPlaylist(
    val id: String,
    val title: String,
    val author: String?,
    val count: String?,
    val thumbnailUrl: String?,
) {
    companion object {
        fun fromJson(data: JsonObject): YouTubeMusicPlaylist? {
            val id = data.string("browseId") ?: data.string("playlistId") ?: return null
            val title = data.string("title") ?: "Unknown Playlist"
            val count = data.string("itemCount")

            val author = data.arrayOrNull("authors")
                ?.firstOrNull()
                ?.jsonObject
                ?.string("name")

            val thumbnailUrl = data.arrayOrNull("thumbnails")
                ?.lastOrNull()
                ?.jsonObject
                ?.string("url")

            return YouTubeMusicPlaylist(
                id = id,
                title = title,
                author = author,
                count = count,
                thumbnailUrl = thumbnailUrl,
            )
        }
    }
}
