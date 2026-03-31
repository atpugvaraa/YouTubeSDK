package aaravgupta.youtubesdk.youtube.models

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class YouTubeVideo(
    val id: String,
    val title: String,
    val viewCount: String,
    val author: String,
    val channelId: String,
    val description: String,
    val lengthInSeconds: String,
    val thumbnailUrl: String?,
    var streamingData: StreamingData?,
    val captions: List<CaptionTrack>?,
) {
    val hlsUrl: String?
        get() = streamingData?.hlsManifestUrl

    val bestAudioStream: Stream?
        get() = streamingData
            ?.adaptiveFormats
            ?.filter { it.isAudioOnly }
            ?.maxByOrNull { it.bitrate }

    val bestMuxedStream: Stream?
        get() = streamingData
            ?.formats
            ?.maxByOrNull { it.height ?: 0 }

    val requiresDeciphering: Boolean
        get() {
            if (hlsUrl != null) return false
            val first = streamingData?.adaptiveFormats?.firstOrNull() ?: return false
            return first.url == null && first.signatureCipher != null
        }

    companion object {
        fun fromPlayerJson(data: JsonObject): YouTubeVideo? {
            val details = data.objectOrNull("videoDetails") ?: return null

            val id = details.string("videoId") ?: return null
            val title = details.string("title") ?: return null
            val viewCount = details.string("viewCount") ?: "0"
            val author = details.string("author") ?: "Unknown"
            val channelId = details.string("channelId") ?: ""
            val description = details.string("shortDescription") ?: ""
            val lengthInSeconds = details.string("lengthSeconds") ?: "0"

            val thumbnailUrl = details.objectOrNull("thumbnail")
                ?.arrayOrNull("thumbnails")
                ?.lastObjectOrNull()
                ?.string("url")

            val streamingData = data.objectOrNull("streamingData")?.let(StreamingData::fromJson)
            val captions = parseCaptions(data)

            return YouTubeVideo(
                id = id,
                title = title,
                viewCount = viewCount,
                author = author,
                channelId = channelId,
                description = description,
                lengthInSeconds = lengthInSeconds,
                thumbnailUrl = thumbnailUrl,
                streamingData = streamingData,
                captions = captions,
            )
        }

        fun fromRendererJson(data: JsonObject): YouTubeVideo? {
            val id = data.string("videoId") ?: return null

            val title = data.textObject("title") ?: "Unknown"

            val viewCount = data.objectOrNull("viewCountText")?.string("simpleText")
                ?: data.objectOrNull("shortViewCountText")?.string("simpleText")
                ?: "0"

            val bylineRuns = data.objectOrNull("ownerText")?.arrayOrNull("runs")
                ?: data.objectOrNull("longBylineText")?.arrayOrNull("runs")
                ?: data.objectOrNull("shortBylineText")?.arrayOrNull("runs")

            val author = bylineRuns
                ?.firstOrNull()
                ?.jsonObject
                ?.string("text")
                ?: "Unknown"

            val channelId = bylineRuns
                ?.firstOrNull()
                ?.jsonObject
                ?.objectOrNull("navigationEndpoint")
                ?.objectOrNull("browseEndpoint")
                ?.string("browseId")
                ?: ""

            val thumbnailUrl = data.objectOrNull("thumbnail")
                ?.arrayOrNull("thumbnails")
                ?.lastObjectOrNull()
                ?.string("url")

            val lengthInSeconds = data.objectOrNull("lengthText")?.string("simpleText") ?: ""

            return YouTubeVideo(
                id = id,
                title = title,
                viewCount = viewCount,
                author = author,
                channelId = channelId,
                description = "",
                lengthInSeconds = lengthInSeconds,
                thumbnailUrl = thumbnailUrl,
                streamingData = null,
                captions = parseCaptions(data),
            )
        }

        private fun parseCaptions(data: JsonObject): List<CaptionTrack>? {
            val trackList = data.objectOrNull("captions")
                ?.objectOrNull("playerCaptionsTracklistRenderer")
                ?.arrayOrNull("captionTracks")
                ?: return null

            val tracks = mutableListOf<CaptionTrack>()
            for (track in trackList) {
                val item = track.jsonObject
                val baseUrl = item.string("baseUrl") ?: continue
                val languageCode = item.string("languageCode") ?: continue
                val name = item.objectOrNull("name")?.string("simpleText")
                    ?: item.objectOrNull("name")
                        ?.arrayOrNull("runs")
                        ?.firstOrNull()
                        ?.jsonObject
                        ?.string("text")
                    ?: continue

                tracks += CaptionTrack(
                    baseUrl = baseUrl,
                    name = name,
                    languageCode = languageCode,
                )
            }

            return tracks
        }
    }
}

data class CaptionTrack(
    val baseUrl: String,
    val name: String,
    val languageCode: String,
)
