package aaravgupta.youtubesdk.youtubeMusic.models

import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.bool
import aaravgupta.youtubesdk.youtube.models.string
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class YouTubeMusicAlbum(
    val id: String,
    val title: String,
    val artist: String?,
    val year: String?,
    val thumbnailUrl: String?,
    val explicit: Boolean,
) {
    companion object {
        fun fromJson(data: JsonObject): YouTubeMusicAlbum? {
            val id = data.string("browseId") ?: data.string("playlistId") ?: return null
            val title = data.string("title") ?: "Unknown Album"
            val year = data.string("year")
            val explicit = data.bool("isExplicit") ?: false

            val artist = data.arrayOrNull("artists")
                ?.firstOrNull()
                ?.jsonObject
                ?.string("name")

            val thumbnailUrl = data.arrayOrNull("thumbnails")
                ?.lastOrNull()
                ?.jsonObject
                ?.string("url")

            return YouTubeMusicAlbum(
                id = id,
                title = title,
                artist = artist,
                year = year,
                thumbnailUrl = thumbnailUrl,
                explicit = explicit,
            )
        }
    }
}
