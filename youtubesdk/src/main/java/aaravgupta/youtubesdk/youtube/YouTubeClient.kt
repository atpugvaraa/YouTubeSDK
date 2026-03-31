package aaravgupta.youtubesdk.youtube

import aaravgupta.youtubesdk.shared.ClientConfig
import aaravgupta.youtubesdk.shared.InnerTubeContext
import aaravgupta.youtubesdk.shared.YouTubeError
import aaravgupta.youtubesdk.shared.YouTubeSdkConstants
import aaravgupta.youtubesdk.shared.cipher.Cipher
import aaravgupta.youtubesdk.shared.network.NetworkClient
import aaravgupta.youtubesdk.youtube.models.YouTubeAISummary
import aaravgupta.youtubesdk.youtube.models.YouTubeChannel
import aaravgupta.youtubesdk.youtube.models.YouTubeContinuation
import aaravgupta.youtubesdk.youtube.models.YouTubeItem
import aaravgupta.youtubesdk.youtube.models.YouTubePlaylist
import aaravgupta.youtubesdk.youtube.models.YouTubeShelf
import aaravgupta.youtubesdk.youtube.models.YouTubeVideo
import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.objectOrNull
import aaravgupta.youtubesdk.youtube.models.string
import aaravgupta.youtubesdk.youtubeMusic.models.YouTubeMusicSong
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class YouTubeClient(
    private val cookies: String? = null,
) {
    internal val network: NetworkClient = NetworkClient(
        context = InnerTubeContext(client = ClientConfig.ios, cookies = cookies),
    )

    internal val webSearchNetwork: NetworkClient = NetworkClient(
        context = InnerTubeContext(client = ClientConfig.web, cookies = cookies),
    )

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val httpClient = OkHttpClient()

    suspend fun getHome(
        regionCode: String? = null,
        languageCode: String? = null,
        musicOnly: Boolean = false,
    ): YouTubeContinuation<YouTubeItem> {
        val searchNetwork = makeWebSearchNetwork(regionCode = regionCode, languageCode = languageCode)

        val data = searchNetwork.get(
            endpoint = "browse",
            body = mapOf("browseId" to YouTubeSdkConstants.InternalKeys.BrowseIds.home),
        )

        var parsedHome = parseContinuationResults(data)
        if (musicOnly) {
            parsedHome = filteredMusicContinuation(parsedHome)
        }
        if (parsedHome.items.isNotEmpty()) {
            return parsedHome
        }

        val fallbackQuery = makeRegionalMusicFallbackQuery(regionCode)
        val searchFallbackData = runCatching {
            searchNetwork.get(
                endpoint = "search",
                body = mapOf("query" to fallbackQuery),
            )
        }.getOrNull()

        if (searchFallbackData != null) {
            var parsedSearchFallback = parseContinuationResults(searchFallbackData)
            if (musicOnly) {
                parsedSearchFallback = filteredMusicContinuation(parsedSearchFallback)
            }
            if (parsedSearchFallback.items.isNotEmpty()) {
                return parsedSearchFallback
            }
        }

        val trendingVideos = runCatching { getTrending() }.getOrDefault(emptyList())
        if (trendingVideos.isNotEmpty()) {
            var trendingFallback = YouTubeContinuation(
                items = trendingVideos.map { video ->
                    YouTubeItem.Video(video) as YouTubeItem
                },
                continuationToken = null,
            )

            if (musicOnly) {
                trendingFallback = filteredMusicContinuation(trendingFallback)
            }

            if (trendingFallback.items.isNotEmpty()) {
                return trendingFallback
            }
        }

        return parsedHome
    }

    suspend fun getTrending(): List<YouTubeVideo> {
        val data = network.get(
            endpoint = "browse",
            body = mapOf("browseId" to YouTubeSdkConstants.InternalKeys.BrowseIds.trending),
        )
        return parseVideos(data)
    }

    suspend fun getChannelVideos(channelId: String): List<YouTubeVideo> {
        val data = network.get(
            endpoint = "browse",
            body = mapOf("browseId" to channelId, "params" to "EgZ2aWRlb3M%3D"),
        )
        return parseVideos(data)
    }

    suspend fun getPlaylist(id: String): YouTubeContinuation<YouTubeVideo> {
        val browseId = if (id.startsWith("PL")) "VL$id" else id
        val data = network.get(endpoint = "browse", body = mapOf("browseId" to browseId))
        val root = parseRoot(data)
            ?: throw YouTubeError.ParsingError("Invalid JSON response for playlist")

        val videosRaw = findAll(YouTubeSdkConstants.InternalKeys.Renderers.playlistVideo, root)
        val videos = videosRaw.mapNotNull { (it as? JsonObject)?.let(YouTubeVideo::fromRendererJson) }

        val token = findContinuationToken(root)
        return YouTubeContinuation(items = videos, continuationToken = token)
    }

    suspend fun search(query: String): YouTubeContinuation<YouTubeItem> {
        val data = webSearchNetwork.get(endpoint = "search", body = mapOf("query" to query))
        return parseContinuationResults(data)
    }

    suspend fun fetchContinuation(
        token: String,
        musicOnly: Boolean = false,
    ): YouTubeContinuation<YouTubeItem> {
        val tokenCandidates = continuationTokenCandidates(token)
        val endpointCandidates = listOf("search", "browse")

        var lastError: Throwable? = null
        for (endpoint in endpointCandidates) {
            for (candidate in tokenCandidates) {
                val body = mapOf("continuation" to candidate)
                try {
                    val data = webSearchNetwork.get(endpoint = endpoint, body = body)
                    var continuation = parseContinuationResults(data)
                    if (musicOnly) {
                        continuation = filteredMusicContinuation(continuation)
                    }
                    return continuation
                } catch (error: Throwable) {
                    lastError = error
                    delay(250)
                }
            }
        }

        throw lastError ?: YouTubeError.ApiError("Unknown continuation failure")
    }

    suspend fun video(id: String): YouTubeVideo {
        val data = network.get(endpoint = "player", body = mapOf("videoId" to id))
        val root = parseRoot(data)
            ?: throw YouTubeError.ParsingError("Invalid JSON response for player endpoint")

        val video = YouTubeVideo.fromPlayerJson(root)
            ?: throw YouTubeError.ParsingError("Failed to parse player response for video=$id")

        if (video.requiresDeciphering) {
            val streamingData = video.streamingData
            if (streamingData != null) {
                val newFormats = streamingData.adaptiveFormats.map { stream ->
                    val cipher = stream.signatureCipher ?: return@map stream
                    runCatching {
                        val decryptedUrl = Cipher.decipher(
                            url = stream.url.orEmpty(),
                            signatureCipher = cipher,
                            network = network,
                        )
                        stream.copy(url = decryptedUrl, signatureCipher = null)
                    }.getOrElse {
                        stream
                    }
                }
                streamingData.adaptiveFormats = newFormats
                video.streamingData = streamingData
            }
        }

        return video
    }

    suspend fun getSearchSuggestions(
        query: String,
        baseUrl: String = YouTubeSdkConstants.Urls.Api.youtubeSuggestionsUrl,
    ): List<String> {
        val rootUrl = baseUrl.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL: $baseUrl")
        val requestUrl = rootUrl.newBuilder()
            .addPathSegment("search")
            .addQueryParameter("ds", "yt")
            .addQueryParameter("client", "youtube")
            .addQueryParameter("q", query)
            .build()

        val request = Request.Builder()
            .url(requestUrl)
            .get()
            .build()

        val responseString = httpClient.newCall(request).execute().use { response ->
            response.body.string()
        }

        val startBracket = responseString.indexOf('[')
        val endBracket = responseString.lastIndexOf(']')
        if (startBracket == -1 || endBracket <= startBracket) {
            return emptyList()
        }

        val jsonString = responseString.substring(startBracket, endBracket + 1)
        val jsonArray = runCatching { json.parseToJsonElement(jsonString).jsonArray }.getOrNull() ?: return emptyList()
        if (jsonArray.size <= 1) return emptyList()

        val suggestionsArray = jsonArray[1].jsonArray
        return suggestionsArray.mapNotNull { suggestion ->
            suggestion.jsonArray.firstOrNull()?.let { first ->
                (first as? JsonPrimitive)?.contentOrNull
            }
        }
    }

    suspend fun getVideoSummary(videoId: String): YouTubeAISummary {
        val data = network.get(
            endpoint = "get_panel",
            body = mapOf(
                "videoId" to videoId,
                "engagementPanelType" to "ENGAGEMENT_PANEL_TYPE_YOU_CHAT",
            ),
        )

        val root = parseRoot(data) ?: throw YouTubeError.ApiError("AI Summary not available for this video.")
        return YouTubeAISummary.fromJson(root)
            ?: throw YouTubeError.ApiError("AI Summary not available for this video.")
    }

    suspend fun createPlaylist(
        title: String,
        description: String? = null,
        privacy: String = "PRIVATE",
    ): String {
        val body = mutableMapOf<String, Any?>(
            "title" to title,
            "privacyStatus" to privacy,
        )
        if (!description.isNullOrBlank()) {
            body["description"] = description
        }

        val data = network.sendComplexRequest(endpoint = "playlist/create", body = body)
        val root = parseRoot(data) ?: throw YouTubeError.ApiError("Failed to create playlist or extract ID")
        return root.string("playlistId")
            ?: throw YouTubeError.ApiError("Failed to create playlist or extract ID")
    }

    suspend fun deletePlaylist(id: String) {
        network.sendComplexRequest(
            endpoint = "playlist/delete",
            body = mapOf("playlistId" to id),
        )
    }

    suspend fun addVideoToPlaylist(videoId: String, playlistId: String) {
        val body: Map<String, Any?> = mapOf(
            "playlistId" to playlistId,
            "actions" to listOf(
                mapOf(
                    "action" to "ACTION_ADD_VIDEO",
                    "addedVideoId" to videoId,
                ),
            ),
        )
        network.sendComplexRequest(endpoint = "browse/edit_playlist", body = body)
    }

    suspend fun removeVideoFromPlaylist(videoId: String, playlistId: String) {
        val body: Map<String, Any?> = mapOf(
            "playlistId" to playlistId,
            "actions" to listOf(
                mapOf(
                    "action" to "ACTION_REMOVE_VIDEO_BY_VIDEO_ID",
                    "removedVideoId" to videoId,
                ),
            ),
        )
        network.sendComplexRequest(endpoint = "browse/edit_playlist", body = body)
    }

    suspend fun like(videoId: String) {
        network.sendComplexRequest(
            endpoint = "like/like",
            body = mapOf("target" to mapOf("videoId" to videoId)),
        )
    }

    suspend fun removeLike(videoId: String) {
        network.sendComplexRequest(
            endpoint = "like/removelike",
            body = mapOf("target" to mapOf("videoId" to videoId)),
        )
    }

    suspend fun dislike(videoId: String) {
        network.sendComplexRequest(
            endpoint = "like/dislike",
            body = mapOf("target" to mapOf("videoId" to videoId)),
        )
    }

    suspend fun subscribe(channelId: String) {
        network.sendComplexRequest(
            endpoint = "subscription/subscribe",
            body = mapOf("channelIds" to listOf(channelId)),
        )
    }

    suspend fun unsubscribe(channelId: String) {
        network.sendComplexRequest(
            endpoint = "subscription/unsubscribe",
            body = mapOf("channelIds" to listOf(channelId)),
        )
    }

    suspend fun getGuide(): JsonObject {
        val data = network.sendComplexRequest(endpoint = "guide", body = emptyMap())
        return parseRoot(data) ?: throw YouTubeError.ParsingError("Could not parse Guide response")
    }

    private fun parseVideos(data: ByteArray): List<YouTubeVideo> {
        val root = parseRoot(data) ?: return emptyList()
        val keys = YouTubeSdkConstants.InternalKeys.Renderers

        return (
            findAll(keys.video, root) +
                findAll(keys.gridVideo, root) +
                findAll(keys.compactVideo, root) +
                findAll(keys.videoWithContext, root)
            ).mapNotNull { (it as? JsonObject)?.let(YouTubeVideo::fromRendererJson) }
    }

    private fun parseContinuationResults(data: ByteArray): YouTubeContinuation<YouTubeItem> {
        val root = parseRoot(data) ?: return YouTubeContinuation(items = emptyList(), continuationToken = null)

        val keys = YouTubeSdkConstants.InternalKeys.Renderers
        val rawVideoNodes = mutableListOf<JsonElement>()

        rawVideoNodes += findAll(keys.video, root)
        rawVideoNodes += findAll(keys.gridVideo, root)
        rawVideoNodes += findAll(keys.compactVideo, root)
        rawVideoNodes += findAll(keys.videoWithContext, root)
        rawVideoNodes += findAll(keys.reelItem, root)
        rawVideoNodes += findAll(keys.richItem, root)
        rawVideoNodes += findAll(keys.itemSection, root)
        rawVideoNodes += findAll(keys.shelf, root)
        rawVideoNodes += findAll("videoCardRenderer", root)
        rawVideoNodes += findAll("videoLockupRenderer", root)
        rawVideoNodes += findAll("videoLockup", root)
        rawVideoNodes += findAll("sectionListRenderer", root)
        rawVideoNodes += findAll("tabbedSearchResultsRenderer", root)
        rawVideoNodes += findAll("onResponseReceivedCommands", root)
        rawVideoNodes += findAll("itemWrapperRenderer", root)

        val serializedTemplates = findAll("serializedTemplateConfig", root)
        for (templateElement in serializedTemplates) {
            val templateString = (templateElement as? JsonPrimitive)?.contentOrNull ?: continue
            val candidates = listOfNotNull(
                templateString,
                percentDecode(templateString),
                decodeBase64(templateString),
                percentDecode(templateString)?.let(::decodeBase64),
            )

            for (candidate in candidates) {
                val parsed = runCatching { json.parseToJsonElement(candidate) }.getOrNull() ?: continue
                rawVideoNodes += findAll(keys.video, parsed)
                rawVideoNodes += findAll("videoCardRenderer", parsed)
                rawVideoNodes += extractPotentialVideoPayloads(parsed)
            }
        }

        val videos = rawVideoNodes.flatMap(::extractPotentialVideoPayloads)
        val channels = findAll(keys.channel, root)
        val playlists = findAll(keys.playlist, root)
        val songs = findAll(keys.musicResponsiveListItem, root)
        val shelves = findAll(keys.musicShelf, root)
        val carousels = findAll(keys.musicCarouselShelf, root)

        val results = mutableListOf<YouTubeItem>()
        val seenVideoIds = mutableSetOf<String>()

        videos.forEach { payload ->
            val video = YouTubeVideo.fromRendererJson(payload) ?: return@forEach
            if (seenVideoIds.add(video.id)) {
                results += YouTubeItem.Video(video)
            }
        }

        channels.forEach { element ->
            val channel = (element as? JsonObject)?.let(YouTubeChannel::fromJson) ?: return@forEach
            results += YouTubeItem.Channel(channel)
        }

        playlists.forEach { element ->
            val playlist = (element as? JsonObject)?.let(YouTubePlaylist::fromJson) ?: return@forEach
            results += YouTubeItem.Playlist(playlist)
        }

        songs.forEach { element ->
            val song = (element as? JsonObject)?.let(YouTubeMusicSong::fromJson) ?: return@forEach
            results += YouTubeItem.Song(song)
        }

        (shelves + carousels).forEach { element ->
            val dict = element as? JsonObject ?: return@forEach
            val title = dict.objectOrNull("title")?.string("simpleText") ?: return@forEach
            results += YouTubeItem.Shelf(YouTubeShelf(title = title, items = emptyList()))
        }

        val token = findContinuationToken(root)
        return YouTubeContinuation(items = results, continuationToken = token)
    }

    private fun extractPotentialVideoPayloads(container: JsonElement): List<JsonObject> {
        val out = mutableListOf<JsonObject>()

        when (container) {
            is JsonObject -> {
                val keys = YouTubeSdkConstants.InternalKeys.Renderers
                val directKeys = listOf(
                    keys.video,
                    keys.gridVideo,
                    keys.compactVideo,
                    keys.videoWithContext,
                    keys.reelItem,
                )
                val extraDirect = listOf(
                    "videoCardRenderer",
                    "videoLockupRenderer",
                    "videoLockup",
                    "video_model",
                    "video_model_renderer",
                )

                directKeys.forEach { key ->
                    container.objectOrNull(key)?.let(out::add)
                }
                extraDirect.forEach { key ->
                    container.objectOrNull(key)?.let(out::add)
                }

                if (container.string("videoId") != null) {
                    out += container
                }

                container["content"]?.let { out += extractPotentialVideoPayloads(it) }
                container["contents"]?.let { out += extractPotentialVideoPayloads(it) }
                container["items"]?.let { out += extractPotentialVideoPayloads(it) }
            }

            else -> {
                if (container is kotlinx.serialization.json.JsonArray) {
                    container.forEach { out += extractPotentialVideoPayloads(it) }
                }
            }
        }

        return out
    }

    private fun findAll(key: String, container: JsonElement): List<JsonElement> {
        val results = mutableListOf<JsonElement>()
        when (container) {
            is JsonObject -> {
                container[key]?.let(results::add)
                container.values.forEach { value ->
                    results += findAll(key, value)
                }
            }

            is kotlinx.serialization.json.JsonArray -> {
                container.forEach { element ->
                    results += findAll(key, element)
                }
            }

            else -> Unit
        }
        return results
    }

    private fun findContinuationToken(container: JsonElement): String? {
        when (container) {
            is JsonObject -> {
                container.string("continuation")?.let { return it }
                container.objectOrNull("continuationEndpoint")
                    ?.objectOrNull("continuationCommand")
                    ?.string("token")
                    ?.let { return it }

                container.values.forEach { value ->
                    val found = findContinuationToken(value)
                    if (found != null) return found
                }
            }

            is kotlinx.serialization.json.JsonArray -> {
                container.forEach { value ->
                    val found = findContinuationToken(value)
                    if (found != null) return found
                }
            }

            else -> Unit
        }
        return null
    }

    private fun continuationTokenCandidates(token: String): List<String> {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return emptyList()

        val candidates = mutableListOf(trimmed)
        var current = trimmed
        repeat(6) {
            val decoded = percentDecode(current) ?: return@repeat
            if (decoded == current) return@repeat
            current = decoded
            candidates += current
        }

        return candidates.distinct()
    }

    private fun percentDecode(value: String): String? {
        return runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun decodeBase64(value: String): String? {
        return runCatching {
            val decoded = java.util.Base64.getDecoder().decode(value)
            decoded.decodeToString()
        }.getOrNull()
    }

    private fun parseRoot(data: ByteArray): JsonObject? {
        return runCatching {
            json.parseToJsonElement(data.decodeToString()) as? JsonObject
        }.getOrNull()
    }

    private fun filteredMusicContinuation(continuation: YouTubeContinuation<YouTubeItem>): YouTubeContinuation<YouTubeItem> {
        val filteredItems = continuation.items.filter(::shouldKeepMusicHomeItem)
        return YouTubeContinuation(items = filteredItems, continuationToken = continuation.continuationToken)
    }

    private fun makeWebSearchNetwork(regionCode: String?, languageCode: String?): NetworkClient {
        val normalizedRegion = normalizedRegionCode(regionCode) ?: return webSearchNetwork
        val normalizedLanguage = normalizedLanguageCode(languageCode)

        val context = InnerTubeContext(
            client = ClientConfig.web,
            cookies = cookies,
            gl = normalizedRegion,
            hl = normalizedLanguage,
        )
        return NetworkClient(context = context)
    }

    private fun shouldKeepMusicHomeItem(item: YouTubeItem): Boolean {
        return when (item) {
            is YouTubeItem.Song -> true
            is YouTubeItem.Playlist -> isLikelyMusicMetadata(item.value.title, item.value.author)
            is YouTubeItem.Video -> isLikelyMusicMetadata(item.value.title, item.value.author)
            is YouTubeItem.Channel -> isLikelyArtistChannelName(item.value.title)
            is YouTubeItem.Shelf -> isLikelyMusicMetadata(item.value.title, null)
        }
    }

    private fun makeRegionalMusicFallbackQuery(regionCode: String?): String {
        val normalizedRegion = normalizedRegionCode(regionCode)
        return if (normalizedRegion != null) {
            "top music videos $normalizedRegion"
        } else {
            "top music videos"
        }
    }

    private fun normalizedRegionCode(rawRegionCode: String?): String? {
        val raw = rawRegionCode?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val upper = raw.uppercase()
        return if (upper.length == 2) upper else null
    }

    private fun normalizedLanguageCode(rawLanguageCode: String?): String {
        val raw = rawLanguageCode?.trim().orEmpty()
        if (raw.isEmpty()) return "en"

        val separatorIndex = raw.indexOfFirst { it == '-' || it == '_' }
        return if (separatorIndex >= 0) {
            raw.substring(0, separatorIndex).lowercase()
        } else {
            raw.lowercase()
        }
    }

    private fun isLikelyArtistChannelName(channelName: String): Boolean {
        val normalized = channelName.lowercase()
        val trustedSignals = listOf(
            "official artist channel",
            "- topic",
            "vevo",
            "records",
            "music",
            "band",
            "orchestra",
        )
        return trustedSignals.any { normalized.contains(it) }
    }

    private fun isLikelyMusicMetadata(title: String, secondaryText: String?): Boolean {
        val normalizedTitle = title.lowercase()
        val normalizedSecondary = secondaryText?.lowercase().orEmpty()
        val merged = "$normalizedTitle $normalizedSecondary"

        val blockedSignals = listOf(
            "#shorts",
            "/shorts/",
            "tutorial",
            "gameplay",
            "gaming",
            "reaction",
            "review",
            "podcast",
            "interview",
        )
        if (blockedSignals.any { merged.contains(it) }) {
            return false
        }

        val positiveSignals = listOf(
            "official music video",
            "official video",
            "official audio",
            "lyric",
            "lyrics",
            "visualizer",
            "audio",
            "song",
            "album",
            "single",
            "remix",
            "acoustic",
            "feat.",
            "ft.",
            "vevo",
            "topic",
        )
        if (positiveSignals.any { merged.contains(it) }) {
            return true
        }

        return isLikelyArtistChannelName(secondaryText.orEmpty())
    }
}
