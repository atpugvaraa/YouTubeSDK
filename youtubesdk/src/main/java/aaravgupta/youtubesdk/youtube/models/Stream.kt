package aaravgupta.youtubesdk.youtube.models

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class Stream(
    var url: String?,
    val itag: Int,
    val mimeType: String,
    val bitrate: Int,
    val width: Int?,
    val height: Int?,
    val contentLength: String?,
    val qualityLabel: String?,
    val audioQuality: String?,
    val approxDurationMs: String?,
    var signatureCipher: String?,
) {
    val isAudioOnly: Boolean
        get() = mimeType.startsWith("audio")

    val isVideoOnly: Boolean
        get() = mimeType.startsWith("video") && audioQuality == null

    companion object {
        fun fromJson(data: JsonObject): Stream? {
            val itag = data.int("itag") ?: return null
            val mimeType = data.string("mimeType") ?: return null
            val bitrate = data.int("bitrate") ?: return null

            return Stream(
                url = data.string("url"),
                itag = itag,
                mimeType = mimeType,
                bitrate = bitrate,
                width = data.int("width"),
                height = data.int("height"),
                contentLength = data.string("contentLength"),
                qualityLabel = data.string("qualityLabel"),
                audioQuality = data.string("audioQuality"),
                approxDurationMs = data.string("approxDurationMs"),
                signatureCipher = data.string("signatureCipher"),
            )
        }
    }
}

data class StreamingData(
    val expiresInSeconds: String?,
    val formats: List<Stream>,
    var adaptiveFormats: List<Stream>,
    val hlsManifestUrl: String?,
) {
    companion object {
        fun fromJson(data: JsonObject): StreamingData {
            val formats = data.arrayOrNull("formats").toStreamList()
            val adaptiveFormats = data.arrayOrNull("adaptiveFormats").toStreamList()

            return StreamingData(
                expiresInSeconds = data.string("expiresInSeconds"),
                formats = formats,
                adaptiveFormats = adaptiveFormats,
                hlsManifestUrl = data.string("hlsManifestUrl"),
            )
        }

        private fun JsonArray?.toStreamList(): List<Stream> {
            if (this == null) return emptyList()
            val out = mutableListOf<Stream>()
            forEach { element ->
                val item = element.jsonObjectOrNull() ?: return@forEach
                Stream.fromJson(item)?.let(out::add)
            }
            return out
        }
    }
}

internal fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

internal fun JsonObject.int(key: String): Int? {
    return this[key]?.jsonPrimitive?.intOrNull
}

internal fun JsonObject.double(key: String): Double? {
    return this[key]?.jsonPrimitive?.doubleOrNull
}

internal fun JsonObject.bool(key: String): Boolean? {
    return this[key]?.jsonPrimitive?.booleanOrNull
}
