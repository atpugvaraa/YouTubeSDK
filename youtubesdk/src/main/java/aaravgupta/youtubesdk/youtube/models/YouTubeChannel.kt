package aaravgupta.youtubesdk.youtube.models

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class YouTubeChannel(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val subscriberCount: String?,
    val videoCount: String?,
) {
    companion object {
        fun fromJson(data: JsonObject): YouTubeChannel? {
            val id = data.string("channelId") ?: data.string("browseId") ?: return null

            val title = data.textObject("title")
                ?: data.string("title")
                ?: "Unknown Channel"

            val thumbsContainer = data.objectOrNull("thumbnail") ?: data.objectOrNull("avatar")
            val thumbnailUrl = thumbsContainer
                ?.arrayOrNull("thumbnails")
                ?.lastObjectOrNull()
                ?.string("url")

            val subscriberCount = data.objectOrNull("subscriberCountText")?.string("simpleText")
            val videoCount = data.objectOrNull("videoCountText")?.string("simpleText")

            return YouTubeChannel(
                id = id,
                title = title,
                thumbnailUrl = thumbnailUrl,
                subscriberCount = subscriberCount,
                videoCount = videoCount,
            )
        }
    }
}

internal fun JsonObject.string(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull
}

internal fun JsonObject.objectOrNull(key: String): JsonObject? {
    return this[key] as? JsonObject
}

internal fun JsonObject.arrayOrNull(key: String): JsonArray? {
    return this[key] as? JsonArray
}

internal fun JsonArray.lastObjectOrNull(): JsonObject? {
    return lastOrNull() as? JsonObject
}

internal fun JsonObject.textObject(key: String): String? {
    val obj = objectOrNull(key) ?: return null
    obj.string("simpleText")?.let { return it }

    val runs = obj.arrayOrNull("runs") ?: return null
    for (run in runs) {
        val text = (run as? JsonObject)?.string("text")
        if (!text.isNullOrBlank()) return text
    }
    return null
}
