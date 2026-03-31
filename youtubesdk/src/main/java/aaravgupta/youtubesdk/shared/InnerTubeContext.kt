package aaravgupta.youtubesdk.shared

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builds the required InnerTube context payload for every request.
 */
class InnerTubeContext(
    private val client: ClientConfig,
    val cookies: String? = null,
    private val gl: String = "US",
    private val hl: String = "en",
) {

    val apiKey: String
        get() = client.apiKey

    val headers: Map<String, String>
        get() {
            val defaultHeaders = linkedMapOf(
                "User-Agent" to client.userAgent,
                "Content-Type" to "application/json",
                "X-YouTube-Client-Name" to client.clientNameId,
                "X-YouTube-Client-Version" to client.version,
            )
            if (!cookies.isNullOrBlank()) {
                defaultHeaders["Cookie"] = cookies
            }
            return defaultHeaders
        }

    val body: JsonObject
        get() = buildJsonObject {
            put(
                "context",
                buildJsonObject {
                    put(
                        "client",
                        buildJsonObject {
                            put("clientName", JsonPrimitive(client.name))
                            put("clientVersion", JsonPrimitive(client.version))
                            put("gl", JsonPrimitive(gl))
                            put("hl", JsonPrimitive(hl))
                            put("timeZone", JsonPrimitive("UTC"))
                            put("utcOffsetMinutes", JsonPrimitive(0))
                        },
                    )
                    put(
                        "user",
                        buildJsonObject {
                            put("lockedSafetyMode", JsonPrimitive(false))
                        },
                    )
                },
            )
        }
}
