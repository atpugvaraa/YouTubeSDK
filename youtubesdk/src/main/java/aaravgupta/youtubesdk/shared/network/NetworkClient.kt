package aaravgupta.youtubesdk.shared.network

import aaravgupta.youtubesdk.shared.InnerTubeContext
import aaravgupta.youtubesdk.shared.YouTubeSdkConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Networking engine for InnerTube APIs.
 */
class NetworkClient(
    private val context: InnerTubeContext,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = YouTubeSdkConstants.Urls.Api.youtubeInnerTubeUrl,
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {

    suspend inline fun <reified T> send(
        endpoint: String,
        body: Map<String, String> = emptyMap(),
    ): T {
        val data = get(endpoint = endpoint, body = body)
        return json.decodeFromString(data.decodeToString())
    }

    suspend fun get(
        endpoint: String,
        body: Map<String, String> = emptyMap(),
    ): ByteArray {
        val typedBody = body.mapValues { (_, value) -> value as Any }
        return sendRawRequest(endpoint = endpoint, body = typedBody)
    }

    suspend fun sendComplexRequest(
        endpoint: String,
        body: Map<String, Any?>,
        queryItems: List<Pair<String, String?>> = emptyList(),
        additionalHeaders: Map<String, String> = emptyMap(),
    ): ByteArray {
        return sendRawRequest(
            endpoint = endpoint,
            body = body,
            queryItems = queryItems,
            additionalHeaders = additionalHeaders,
        )
    }

    suspend fun getAbsolute(
        url: String,
        additionalHeaders: Map<String, String> = emptyMap(),
    ): ByteArray = withContext(Dispatchers.IO) {
        val targetUrl = url.toHttpUrlOrNull() ?: throw IOException("Invalid URL: $url")

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .get()

        // Reuse context headers for compatibility with YouTube responses.
        context.headers.forEach { (key, value) ->
            if (key != "Content-Type") {
                requestBuilder.header(key, value)
            }
        }

        additionalHeaders.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        val request = requestBuilder.build()
        httpClient.newCall(request).execute().use { response ->
            val bytes = response.body.bytes()
            if (response.code != 200) {
                println("YouTube GET URL: $targetUrl")
                println("YouTube GET Error (${response.code}): ${bytes.decodeToString()}")
                throw IOException("YouTube GET request failed with HTTP ${response.code}")
            }
            bytes
        }
    }

    private suspend fun sendRawRequest(
        endpoint: String,
        body: Map<String, Any?>,
        queryItems: List<Pair<String, String?>> = emptyList(),
        additionalHeaders: Map<String, String> = emptyMap(),
    ): ByteArray = withContext(Dispatchers.IO) {
        val url = makeEndpointUrl(endpoint, additionalQueryItems = queryItems)

        val payload = buildJsonObject(
            contextBody = context.body,
            requestBody = body,
        )

        val requestBuilder = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

        context.headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }
        requestBuilder.header("Accept", "application/json")

        additionalHeaders.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        url.host.let { host ->
            val origin = "https://$host"
            requestBuilder.header("Origin", origin)
            requestBuilder.header("Referer", "$origin/")
        }

        val request = requestBuilder.build()
        httpClient.newCall(request).execute().use { response ->
            val responseBodyBytes = response.body.bytes()

            if (response.code != 200) {
                val requestBody = request.body
                if (requestBody != null) {
                    val buffer = okio.Buffer()
                    requestBody.writeTo(buffer)
                    println("YouTube Request URL: ${url}")
                    println("YouTube Request Body: ${buffer.readUtf8()}")
                }
                println("YouTube Error (${response.code}): ${responseBodyBytes.decodeToString()}")
                throw IOException("YouTube request failed with HTTP ${response.code}")
            }

            responseBodyBytes
        }
    }

    private fun makeEndpointUrl(
        endpoint: String,
        additionalQueryItems: List<Pair<String, String?>> = emptyList(),
    ): HttpUrl {
        val rootUrl = baseUrl.toHttpUrlOrNull()
            ?: throw IOException("Invalid base URL: $baseUrl")

        val trimmed = endpoint.trim('/').trim()
        val endpointPath = when {
            trimmed.isEmpty() -> "v1"
            trimmed == "v1" || trimmed.startsWith("v1/") -> trimmed
            else -> "v1/$trimmed"
        }

        val builder = rootUrl.newBuilder().addPathSegments(endpointPath)
        builder.addQueryParameter("key", context.apiKey)
        additionalQueryItems.forEach { (name, value) ->
            builder.addQueryParameter(name, value)
        }

        return builder.build()
    }

    private fun buildJsonObject(
        contextBody: JsonObject,
        requestBody: Map<String, Any?>,
    ): JsonObject {
        val merged = contextBody.toMutableMap()
        requestBody.forEach { (key, value) ->
            merged[key] = value.toJsonElement()
        }
        return JsonObject(merged)
    }

    private fun Any?.toJsonElement(): JsonElement {
        return when (this) {
            null -> JsonNull
            is JsonElement -> this
            is String -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is Map<*, *> -> {
                val mapped = buildMap<String, JsonElement> {
                    this@toJsonElement.forEach { (key, value) ->
                        if (key is String) {
                            put(key, value.toJsonElement())
                        }
                    }
                }
                JsonObject(mapped)
            }
            is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
            is Array<*> -> JsonArray(this.map { it.toJsonElement() })
            else -> JsonPrimitive(this.toString())
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
