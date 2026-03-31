package aaravgupta.youtubesdk.youtubeMusic

import aaravgupta.youtubesdk.shared.ClientConfig
import aaravgupta.youtubesdk.shared.InnerTubeContext
import aaravgupta.youtubesdk.shared.YouTubeSdkConstants
import aaravgupta.youtubesdk.shared.network.NetworkClient
import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.objectOrNull
import aaravgupta.youtubesdk.youtube.models.string
import aaravgupta.youtubesdk.youtubeMusic.models.YouTubeMusicArtistDetail
import aaravgupta.youtubesdk.youtubeMusic.models.YouTubeMusicHomePage
import aaravgupta.youtubesdk.youtubeMusic.models.YouTubeMusicSection
import aaravgupta.youtubesdk.youtubeMusic.models.YouTubeMusicSong
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class YouTubeMusicClient(
    internal val cookies: String? = null,
) {
    internal val network: NetworkClient = NetworkClient(
        context = InnerTubeContext(client = ClientConfig.webRemix, cookies = cookies),
        baseUrl = YouTubeSdkConstants.Urls.Api.youtubeMusicInnerTubeUrl,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun search(query: String): List<YouTubeMusicSong> {
        val data = network.get(endpoint = "search", body = mapOf("query" to query))
        return parseMusicItems(data)
    }

    suspend fun getSearchSuggestions(query: String): List<String> {
        val data = network.get(
            endpoint = "music/get_search_suggestions",
            body = mapOf("input" to query),
        )

        val root = parseRoot(data) ?: return emptyList()
        val suggestions = findAll("searchSuggestionRenderer", root)
        return suggestions.mapNotNull { item ->
            val dict = item as? JsonObject ?: return@mapNotNull null

            val queryFromEndpoint = dict.objectOrNull("navigationEndpoint")
                ?.objectOrNull("searchEndpoint")
                ?.string("query")
            if (!queryFromEndpoint.isNullOrBlank()) {
                return@mapNotNull queryFromEndpoint
            }

            val suggestionRuns = dict.objectOrNull("suggestion")
                ?.arrayOrNull("runs")
                ?.mapNotNull { run -> (run as? JsonObject)?.string("text") }
                ?: emptyList()
            if (suggestionRuns.isNotEmpty()) {
                suggestionRuns.joinToString(separator = "")
            } else {
                null
            }
        }
    }

    suspend fun getHome(): List<YouTubeMusicSection> {
        return getHomePage().sections
    }

    suspend fun getHomePage(
        regionCode: String? = null,
        languageCode: String? = null,
    ): YouTubeMusicHomePage {
        val client = makeNetwork(regionCode = regionCode, languageCode = languageCode)
        val data = client.get(
            endpoint = "browse",
            body = mapOf("browseId" to YouTubeSdkConstants.InternalKeys.BrowseIds.Music.home),
        )
        return parseHomePage(data)
    }

    suspend fun getHomeContinuation(
        token: String,
        regionCode: String? = null,
        languageCode: String? = null,
    ): YouTubeMusicHomePage {
        val client = makeNetwork(regionCode = regionCode, languageCode = languageCode)
        val data = client.get(endpoint = "browse", body = mapOf("continuation" to token))
        return parseHomePage(data)
    }

    suspend fun getCharts(): List<YouTubeMusicSection> {
        return browseSection(YouTubeSdkConstants.InternalKeys.BrowseIds.Music.charts)
    }

    suspend fun getNewReleases(): List<YouTubeMusicSection> {
        return browseSection(YouTubeSdkConstants.InternalKeys.BrowseIds.Music.newReleases)
    }

    suspend fun getMoods(): List<YouTubeMusicSection> {
        return browseSection(YouTubeSdkConstants.InternalKeys.BrowseIds.Music.moods)
    }

    suspend fun getLikedSongs(): List<YouTubeMusicSong> {
        val data = network.get(
            endpoint = "browse",
            body = mapOf("browseId" to YouTubeSdkConstants.InternalKeys.BrowseIds.Music.likedVideos),
        )
        return parseMusicItems(data)
    }

    suspend fun getHistory(): List<YouTubeMusicSong> {
        val data = network.get(
            endpoint = "browse",
            body = mapOf("browseId" to YouTubeSdkConstants.InternalKeys.BrowseIds.Music.history),
        )
        return parseMusicItems(data)
    }

    suspend fun getLibrary(): List<YouTubeMusicSection> {
        val data = network.get(
            endpoint = "browse",
            body = mapOf("browseId" to YouTubeSdkConstants.InternalKeys.BrowseIds.Music.library),
        )
        val root = parseRoot(data) ?: return emptyList()
        return parseSections(root)
    }

    suspend fun getArtist(browseId: String): YouTubeMusicArtistDetail {
        val data = network.get(endpoint = "browse", body = mapOf("browseId" to browseId))
        val root = parseRoot(data) ?: return YouTubeMusicArtistDetail(id = browseId, sections = emptyList())
        val sections = parseSections(root)
        return YouTubeMusicArtistDetail(id = browseId, sections = sections)
    }

    suspend fun getAlbum(browseId: String): List<YouTubeMusicSong> {
        val data = network.get(endpoint = "browse", body = mapOf("browseId" to browseId))
        return parseMusicItems(data)
    }

    suspend fun getPlaylist(browseId: String): List<YouTubeMusicSong> {
        val normalizedBrowseId = if (browseId.startsWith("PL")) "VL$browseId" else browseId
        val data = network.get(endpoint = "browse", body = mapOf("browseId" to normalizedBrowseId))
        return parseMusicItems(data)
    }

    suspend fun getLyrics(videoId: String): String? {
        val nextData = network.get(endpoint = "next", body = mapOf("videoId" to videoId))
        val root = parseRoot(nextData) ?: return null

        val tabs = findAll("tabRenderer", root)
        val lyricsTab = tabs.firstOrNull { tab ->
            (tab as? JsonObject)?.string("title") == "Lyrics"
        } as? JsonObject ?: return null

        val browseId = lyricsTab.objectOrNull("endpoint")
            ?.objectOrNull("browseEndpoint")
            ?.string("browseId")
            ?: return null

        val lyricsData = network.get(endpoint = "browse", body = mapOf("browseId" to browseId))
        val lyricsRoot = parseRoot(lyricsData) ?: return null

        val descriptions = findAll("musicDescriptionShelfRenderer", lyricsRoot)
        val shelf = descriptions.firstOrNull() as? JsonObject ?: return null

        val runs = shelf.objectOrNull("description")?.arrayOrNull("runs") ?: return null
        return runs.mapNotNull { run -> (run as? JsonObject)?.string("text") }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "")
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

    private suspend fun browseSection(browseId: String): List<YouTubeMusicSection> {
        val data = network.get(endpoint = "browse", body = mapOf("browseId" to browseId))
        val root = parseRoot(data) ?: return emptyList()
        return parseSections(root)
    }

    private fun makeNetwork(regionCode: String?, languageCode: String?): NetworkClient {
        val normalizedRegion = normalizedRegionCode(regionCode) ?: return network
        val normalizedLanguage = normalizedLanguageCode(languageCode)
        val context = InnerTubeContext(
            client = ClientConfig.webRemix,
            cookies = cookies,
            gl = normalizedRegion,
            hl = normalizedLanguage,
        )
        return NetworkClient(
            context = context,
            baseUrl = YouTubeSdkConstants.Urls.Api.youtubeMusicInnerTubeUrl,
        )
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

    private fun parseMusicItems(data: ByteArray): List<YouTubeMusicSong> {
        val root = parseRoot(data) ?: return emptyList()
        val items = findAll("musicResponsiveListItemRenderer", root)
        return items.mapNotNull { item -> (item as? JsonObject)?.let(YouTubeMusicSong::fromJson) }
    }

    private fun parseHomePage(data: ByteArray): YouTubeMusicHomePage {
        val root = parseRoot(data) ?: return YouTubeMusicHomePage(sections = emptyList(), continuationToken = null)
        val sections = parseSections(root)
        val continuationToken = findContinuationToken(root)
        return YouTubeMusicHomePage(sections = sections, continuationToken = continuationToken)
    }

    private fun parseSections(root: JsonObject): List<YouTubeMusicSection> {
        val sections = mutableListOf<YouTubeMusicSection>()

        val carousels = findAll("musicCarouselShelfRenderer", root)
        carousels.forEach { element ->
            val section = (element as? JsonObject)?.let(YouTubeMusicSection::fromJson)
            if (section != null) sections += section
        }

        val shelves = findAll("musicShelfRenderer", root)
        shelves.forEach { element ->
            val section = (element as? JsonObject)?.let(YouTubeMusicSection::fromJson)
            if (section != null) sections += section
        }

        return sections
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
                    val token = findContinuationToken(value)
                    if (token != null) return token
                }
            }

            is kotlinx.serialization.json.JsonArray -> {
                container.forEach { element ->
                    val token = findContinuationToken(element)
                    if (token != null) return token
                }
            }

            else -> Unit
        }
        return null
    }

    private fun parseRoot(data: ByteArray): JsonObject? {
        return runCatching {
            json.parseToJsonElement(data.decodeToString()) as? JsonObject
        }.getOrNull()
    }
}
