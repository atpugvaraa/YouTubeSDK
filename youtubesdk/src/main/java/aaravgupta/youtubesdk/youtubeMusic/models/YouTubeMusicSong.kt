package aaravgupta.youtubesdk.youtubeMusic.models

import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.objectOrNull
import aaravgupta.youtubesdk.youtube.models.string
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class YouTubeMusicSong(
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String?,
    val duration: Double?,
    val thumbnailUrl: String?,
    val videoId: String,
    val isExplicit: Boolean,
) {
    val artistsDisplay: String
        get() = artists.joinToString(", ")

    companion object {
        fun fromJson(data: JsonObject): YouTubeMusicSong? {
            val extractedId = data.string("videoId")
                ?: data.objectOrNull("playlistItemData")?.string("videoId")
                ?: data.objectOrNull("navigationEndpoint")
                    ?.objectOrNull("watchEndpoint")
                    ?.string("videoId")
                ?: return null

            val title = data.arrayOrNull("flexColumns")
                ?.firstOrNull()
                ?.jsonObject
                ?.objectOrNull("musicResponsiveListItemFlexColumnRenderer")
                ?.objectOrNull("text")
                ?.arrayOrNull("runs")
                ?.firstOrNull()
                ?.jsonObject
                ?.string("text")
                ?: "Unknown Title"

            val foundArtists = mutableListOf<String>()
            var foundAlbum: String? = null
            var foundDuration: Double? = null

            val columns = data.arrayOrNull("flexColumns")
            if (columns != null && columns.size > 1) {
                val secondCol = columns[1].jsonObject
                val runs = secondCol
                    .objectOrNull("musicResponsiveListItemFlexColumnRenderer")
                    ?.objectOrNull("text")
                    ?.arrayOrNull("runs")
                    ?: emptyList()

                for (run in runs) {
                    val runObject = run.jsonObject
                    val text = runObject.string("text") ?: continue

                    val pageType = runObject
                        .objectOrNull("navigationEndpoint")
                        ?.objectOrNull("browseEndpoint")
                        ?.objectOrNull("browseEndpointContextSupportedConfigs")
                        ?.objectOrNull("browseEndpointContextMusicConfig")
                        ?.string("pageType")

                    when (pageType) {
                        "MUSIC_PAGE_TYPE_ARTIST" -> foundArtists += text
                        "MUSIC_PAGE_TYPE_ALBUM" -> foundAlbum = text
                        else -> {
                            if (text.contains(':')) {
                                foundDuration = parseDuration(text)
                            }
                        }
                    }
                }
            }

            val thumbnailUrl = data.objectOrNull("thumbnail")
                ?.objectOrNull("musicThumbnailRenderer")
                ?.objectOrNull("thumbnail")
                ?.arrayOrNull("thumbnails")
                ?.lastOrNull()
                ?.jsonObject
                ?.string("url")

            var explicitBadgeFound = false
            val badges = data.arrayOrNull("badges")
            if (badges != null) {
                for (badge in badges) {
                    val iconType = badge.jsonObject
                        .objectOrNull("musicInlineBadgeRenderer")
                        ?.objectOrNull("icon")
                        ?.string("iconType")
                    if (iconType == "MUSIC_EXPLICIT_BADGE") {
                        explicitBadgeFound = true
                        break
                    }
                }
            }

            return YouTubeMusicSong(
                id = extractedId,
                title = title,
                artists = foundArtists,
                album = foundAlbum,
                duration = foundDuration,
                thumbnailUrl = thumbnailUrl,
                videoId = extractedId,
                isExplicit = explicitBadgeFound,
            )
        }

        private fun parseDuration(value: String): Double? {
            val parts = value.split(':').mapNotNull { it.toDoubleOrNull() }
            return when (parts.size) {
                2 -> (parts[0] * 60.0) + parts[1]
                3 -> (parts[0] * 3600.0) + (parts[1] * 60.0) + parts[2]
                else -> null
            }
        }
    }
}
