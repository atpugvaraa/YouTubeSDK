package aaravgupta.youtubesdk.youtubeMusic.models

import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.string
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class YouTubeMusicArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscriberCount: String?,
) {
    companion object {
        fun fromJson(data: JsonObject): YouTubeMusicArtist? {
            val id = data.string("browseId") ?: data.string("id") ?: return null
            val name = data.string("name") ?: "Unknown Artist"

            val thumbnailUrl = data.arrayOrNull("thumbnails")
                ?.lastOrNull()
                ?.jsonObject
                ?.string("url")

            return YouTubeMusicArtist(
                id = id,
                name = name,
                thumbnailUrl = thumbnailUrl,
                subscriberCount = data.string("subscriberCount"),
            )
        }
    }
}
