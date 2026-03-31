package aaravgupta.youtubesdk.youtubeCharts.models

import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.int
import aaravgupta.youtubesdk.youtube.models.objectOrNull
import aaravgupta.youtubesdk.youtube.models.string
import aaravgupta.youtubesdk.youtube.models.textObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

data class YouTubeChartItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val rank: String,
    val thumbnailUrl: String?,
    val type: ChartItemType,
    val viewCount: String?,
    val change: String?,
) {
    enum class ChartItemType {
        SONG,
        VIDEO,
        ARTIST,
    }

    companion object {
        fun fromJson(data: JsonObject, type: ChartItemType): YouTubeChartItem? {
            val payload = unwrapAnalyticsPayload(data)
            val resolvedId = extractId(payload) ?: return null

            val title = extractText(payload["title"]) ?: extractText(payload["name"]) ?: "Unknown"

            val subtitle = extractText(payload["subtitle"])
                ?: extractText(payload["byline"])
                ?: extractText(payload["artist"])
                ?: extractText(payload["artistsText"])
                ?: extractText(payload["channelName"])
                ?: extractArtists(payload["artists"])
                ?: extractFlexSubtitle(payload)
                ?: ""

            val rank = extractRank(payload) ?: "0"
            val thumbnailUrl = extractThumbnailUrl(payload)

            val viewCount = extractText(payload["viewCountText"])
                ?: extractText(payload["viewCount"])
                ?: extractText(payload["viewsText"])

            val change = extractChange(payload)

            return YouTubeChartItem(
                id = resolvedId,
                title = title,
                subtitle = subtitle,
                rank = rank,
                thumbnailUrl = thumbnailUrl,
                type = type,
                viewCount = viewCount,
                change = change,
            )
        }

        private fun unwrapAnalyticsPayload(data: JsonObject): JsonObject {
            val modelKeys = listOf(
                "musicAnalyticsTrackViewModel",
                "musicAnalyticsVideoViewModel",
                "musicAnalyticsArtistViewModel",
            )
            for (key in modelKeys) {
                data.objectOrNull(key)?.let { return it }
            }
            return data
        }

        private fun extractId(data: JsonObject): String? {
            return data.string("videoId")
                ?: data.string("encryptedVideoId")
                ?: data.string("atvExternalVideoId")
                ?: data.string("browseId")
                ?: data.string("externalChannelId")
                ?: data.string("id")
                ?: data.string("entityId")
                ?: data.objectOrNull("navigationEndpoint")
                    ?.objectOrNull("watchEndpoint")
                    ?.string("videoId")
                ?: data.objectOrNull("navigationEndpoint")
                    ?.objectOrNull("browseEndpoint")
                    ?.string("browseId")
        }

        private fun extractText(value: JsonElement?): String? {
            when (value) {
                null -> return null
                is JsonPrimitive -> {
                    val text = value.contentOrNull ?: return null
                    val trimmed = text.trim()
                    return trimmed.ifEmpty { null }
                }

                is JsonObject -> {
                    value.string("simpleText")?.let { return it }

                    val runs = value.arrayOrNull("runs")
                    if (runs != null) {
                        val joined = runs.mapNotNull { run ->
                            (run as? JsonObject)?.string("text")
                        }.joinToString(separator = "")
                        val trimmed = joined.trim()
                        if (trimmed.isNotEmpty()) return trimmed
                    }

                    value.string("text")?.let { return it }
                    value.string("name")?.let { return it }
                }

                else -> return null
            }
            return null
        }

        private fun extractArtists(value: JsonElement?): String? {
            val artists = value as? kotlinx.serialization.json.JsonArray ?: return null
            val names = artists.mapNotNull { artist ->
                val artistObj = artist as? JsonObject ?: return@mapNotNull null
                extractText(artistObj["name"]) ?: extractText(artistObj["text"])
            }
            return names.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ")
        }

        private fun extractFlexSubtitle(data: JsonObject): String? {
            val flex = data.arrayOrNull("flexColumns") ?: return null
            if (flex.size <= 1) return null

            val textData = (flex[1] as? JsonObject)
                ?.objectOrNull("musicResponsiveListItemFlexColumnRenderer")
                ?.get("text")
                ?: return null
            return extractText(textData)
        }

        private fun extractRank(data: JsonObject): String? {
            extractText(data["rank"])?.let { return it }
            extractText(data["chartRank"])?.let { return it }

            val metadata = data.objectOrNull("chartEntryMetadata")
            metadata?.int("currentPosition")?.let { return it.toString() }
            metadata?.string("currentPosition")?.let { return it }

            val textData = data.objectOrNull("customIndexColumn")
                ?.objectOrNull("musicCustomIndexColumnRenderer")
                ?.get("text")
            return extractText(textData)
        }

        private fun extractThumbnailUrl(data: JsonObject): String? {
            val candidates = listOf(
                data["thumbnailDetails"],
                data["thumbnail"],
                data["musicThumbnailRenderer"],
                data["thumbnails"],
            )

            for (candidate in candidates) {
                val url = thumbnailUrl(candidate)
                if (!url.isNullOrBlank()) return url
            }
            return null
        }

        private fun thumbnailUrl(value: JsonElement?): String? {
            when (value) {
                null -> return null
                is JsonObject -> {
                    value.arrayOrNull("thumbnails")
                        ?.lastOrNull()
                        ?.let { thumb -> (thumb as? JsonObject)?.string("url") }
                        ?.let { return it }

                    thumbnailUrl(value["thumbnail"])?.let { return it }
                    thumbnailUrl(value["musicThumbnailRenderer"])?.let { return it }
                }

                is kotlinx.serialization.json.JsonArray -> {
                    val url = value.lastOrNull()?.let { (it as? JsonObject)?.string("url") }
                    if (!url.isNullOrBlank()) return url
                }

                else -> Unit
            }
            return null
        }

        private fun extractChange(data: JsonObject): String? {
            extractText(data["change"])?.let { return it }
            extractText(data["chartChangeText"])?.let { return it }

            val metadata = data.objectOrNull("chartEntryMetadata") ?: return null
            val current = metadata.int("currentPosition") ?: return null
            val previous = metadata.int("previousPosition") ?: return null

            if (previous <= 0) return "NEW"
            val delta = previous - current
            return when {
                delta > 0 -> "+$delta"
                delta < 0 -> "$delta"
                else -> "0"
            }
        }
    }
}
