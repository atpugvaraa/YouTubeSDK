package aaravgupta.youtubesdk.youtubeCharts

import aaravgupta.youtubesdk.shared.ClientConfig
import aaravgupta.youtubesdk.shared.InnerTubeContext
import aaravgupta.youtubesdk.shared.YouTubeSdkConstants
import aaravgupta.youtubesdk.shared.network.NetworkClient
import aaravgupta.youtubesdk.youtube.models.arrayOrNull
import aaravgupta.youtubesdk.youtube.models.objectOrNull
import aaravgupta.youtubesdk.youtube.models.string
import aaravgupta.youtubesdk.youtubeCharts.models.YouTubeChartItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class YouTubeChartsClient {
    internal val analyticsNetwork: NetworkClient = NetworkClient(
        context = InnerTubeContext(client = ClientConfig.webMusicAnalytics),
        baseUrl = YouTubeSdkConstants.Urls.Api.youtubeChartsInnerTubeUrl,
    )

    internal val musicNetwork: NetworkClient = NetworkClient(
        context = InnerTubeContext(client = ClientConfig.webRemix),
        baseUrl = YouTubeSdkConstants.Urls.Api.youtubeMusicInnerTubeUrl,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun getTopSongs(country: String = "ZZ"): List<YouTubeChartItem> {
        return fetchChart(
            country = country,
            type = YouTubeChartItem.ChartItemType.SONG,
            sectionKeywords = listOf("song"),
        )
    }

    suspend fun getTopVideos(country: String = "ZZ"): List<YouTubeChartItem> {
        return fetchChart(
            country = country,
            type = YouTubeChartItem.ChartItemType.VIDEO,
            sectionKeywords = listOf("video"),
        )
    }

    suspend fun getTopArtists(country: String = "ZZ"): List<YouTubeChartItem> {
        return fetchChart(
            country = country,
            type = YouTubeChartItem.ChartItemType.ARTIST,
            sectionKeywords = listOf("artist"),
        )
    }

    suspend fun getTrending(country: String = "ZZ"): List<YouTubeChartItem> {
        return fetchChart(
            country = country,
            type = YouTubeChartItem.ChartItemType.VIDEO,
            sectionKeywords = listOf("trending", "video"),
        )
    }

    private suspend fun fetchChart(
        country: String,
        type: YouTubeChartItem.ChartItemType,
        sectionKeywords: List<String>,
    ): List<YouTubeChartItem> {
        val countryCode = normalizedCountryCode(country)

        val analyticsData = runCatching {
            analyticsNetwork.sendComplexRequest(
                endpoint = "browse",
                body = mapOf(
                    "browseId" to "FEmusic_analytics_charts_home",
                    "query" to "perspective=CHART_HOME&chart_params_country_code=$countryCode",
                ),
                queryItems = listOf("alt" to "json"),
                additionalHeaders = mapOf("X-Goog-Api-Format-Version" to "2"),
            )
        }.getOrNull()

        if (analyticsData != null) {
            val parsedHome = parseCharts(analyticsData, type, sectionKeywords)
            if (parsedHome.isNotEmpty()) {
                return parsedHome
            }
        }

        val legacyBrowseId = legacyBrowseId(type)
        if (legacyBrowseId != null) {
            val legacyData = runCatching {
                analyticsNetwork.sendComplexRequest(
                    endpoint = "browse",
                    body = mapOf("browseId" to legacyBrowseId),
                    queryItems = listOf("alt" to "json"),
                    additionalHeaders = mapOf("X-Goog-Api-Format-Version" to "2"),
                )
            }.getOrNull()

            if (legacyData != null) {
                val parsedLegacy = parseCharts(legacyData, type, sectionKeywords)
                if (parsedLegacy.isNotEmpty()) {
                    return parsedLegacy
                }
            }
        }

        val fallbackData = musicNetwork.sendComplexRequest(
            endpoint = "browse",
            body = mapOf("browseId" to YouTubeSdkConstants.InternalKeys.BrowseIds.Music.charts),
        )
        return parseCharts(fallbackData, type, sectionKeywords)
    }

    private fun normalizedCountryCode(country: String): String {
        val normalized = country.trim()
        return if (
            normalized.isEmpty() ||
            normalized.equals("ZZ", ignoreCase = true) ||
            normalized.equals("global", ignoreCase = true)
        ) {
            "global"
        } else {
            normalized.lowercase()
        }
    }

    private fun legacyBrowseId(type: YouTubeChartItem.ChartItemType): String? {
        return when (type) {
            YouTubeChartItem.ChartItemType.SONG -> "FEmusic_analytics_charts_songs"
            YouTubeChartItem.ChartItemType.VIDEO -> "FEmusic_analytics_charts_videos"
            YouTubeChartItem.ChartItemType.ARTIST -> null
        }
    }

    private fun parseCharts(
        data: ByteArray,
        type: YouTubeChartItem.ChartItemType,
        sectionKeywords: List<String>,
    ): List<YouTubeChartItem> {
        val root = parseRoot(data) ?: return emptyList()

        val analyticsSections = findAll("musicAnalyticsSectionRenderer", root)
            .mapNotNull { it as? JsonObject }
        val analyticsItems = parseAnalyticsSections(
            sections = analyticsSections,
            type = type,
            sectionKeywords = sectionKeywords,
        )
        if (analyticsItems.isNotEmpty()) {
            return analyticsItems
        }

        val items = mutableListOf<YouTubeChartItem>()
        val seenIds = mutableSetOf<String>()

        val rowRenderers = findAll("musicResponsiveListItemRenderer", root) +
            findAll("musicTableRowRenderer", root)

        for (renderer in rowRenderers) {
            val dict = renderer as? JsonObject ?: continue
            val item = YouTubeChartItem.fromJson(dict, type) ?: continue
            if (seenIds.add(item.id)) {
                items += item
            }
        }

        return items
    }

    private fun parseAnalyticsSections(
        sections: List<JsonObject>,
        type: YouTubeChartItem.ChartItemType,
        sectionKeywords: List<String>,
    ): List<YouTubeChartItem> {
        if (sections.isEmpty()) return emptyList()

        val normalizedKeywords = sectionKeywords.map(String::lowercase)
        val matchingSections = sections.filter { section ->
            if (normalizedKeywords.isEmpty()) return@filter true
            val title = extractText(section["title"])?.lowercase() ?: return@filter false
            normalizedKeywords.any { keyword -> title.contains(keyword) }
        }

        val sectionsToParse = if (matchingSections.isEmpty()) sections else matchingSections
        val items = mutableListOf<YouTubeChartItem>()
        val seenIds = mutableSetOf<String>()

        for (section in sectionsToParse) {
            val content = section.objectOrNull("content") ?: section
            val entries = analyticsEntries(content, type)
            for (entry in entries) {
                val dict = entry as? JsonObject ?: continue
                val item = YouTubeChartItem.fromJson(dict, type) ?: continue
                if (seenIds.add(item.id)) {
                    items += item
                }
            }
        }

        return items
    }

    private fun analyticsEntries(
        section: JsonObject,
        type: YouTubeChartItem.ChartItemType,
    ): List<JsonElement> {
        val entries = mutableListOf<JsonElement>()

        val preferredKeys = when (type) {
            YouTubeChartItem.ChartItemType.SONG -> listOf("trackViews", "tracks", "songs")
            YouTubeChartItem.ChartItemType.VIDEO -> listOf("videoViews", "trendingVideos")
            YouTubeChartItem.ChartItemType.ARTIST -> listOf("artistViews")
        }

        for (key in preferredKeys) {
            val found = entriesArray(key, section)
            if (found.isNotEmpty()) entries += found
        }

        when (type) {
            YouTubeChartItem.ChartItemType.SONG -> {
                val trackTypes = section.arrayOrNull("trackTypes")
                trackTypes?.forEach { trackType ->
                    val dict = trackType as? JsonObject ?: return@forEach
                    val found = entriesArray("trackViews", dict)
                    if (found.isNotEmpty()) entries += found
                }
            }

            YouTubeChartItem.ChartItemType.VIDEO -> {
                val videos = section.arrayOrNull("videos")
                videos?.forEach { videosList ->
                    val dict = videosList as? JsonObject ?: return@forEach
                    val found = entriesArray("videoViews", dict)
                    if (found.isNotEmpty()) entries += found
                }
            }

            YouTubeChartItem.ChartItemType.ARTIST -> {
                val artistsContainer = section.objectOrNull("artists")
                if (artistsContainer != null) {
                    val found = entriesArray("artistViews", artistsContainer)
                    if (found.isNotEmpty()) entries += found
                }
            }
        }

        if (entries.isNotEmpty()) {
            return entries
        }

        val fallbackKeys = listOf("trackViews", "videoViews", "artists", "artistViews")
        for (key in fallbackKeys) {
            val found = entriesArray(key, section)
            if (found.isNotEmpty()) entries += found
        }

        return entries
    }

    private fun entriesArray(key: String, section: JsonObject): List<JsonElement> {
        val direct = section.arrayOrNull(key)
        if (direct != null) return direct

        val wrapped = section.objectOrNull(key)
        val items = wrapped?.arrayOrNull("items")
        if (items != null) return items

        return emptyList()
    }

    private fun extractText(value: JsonElement?): String? {
        when (value) {
            null -> return null
            is JsonPrimitive -> {
                val text = value.contentOrNull?.trim().orEmpty()
                return text.ifEmpty { null }
            }

            is JsonObject -> {
                value.string("simpleText")?.let { return it }
                value.arrayOrNull("runs")?.let { runs ->
                    val joined = runs.mapNotNull { run -> (run as? JsonObject)?.string("text") }
                        .joinToString(separator = "")
                    val trimmed = joined.trim()
                    if (trimmed.isNotEmpty()) return trimmed
                }
            }

            else -> Unit
        }
        return null
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

    private fun parseRoot(data: ByteArray): JsonObject? {
        return runCatching {
            json.parseToJsonElement(data.decodeToString()) as? JsonObject
        }.getOrNull()
    }
}
