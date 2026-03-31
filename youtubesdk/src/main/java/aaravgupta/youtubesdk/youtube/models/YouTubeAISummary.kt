package aaravgupta.youtubesdk.youtube.models

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class YouTubeAISummary(
    val text: String,
    val highlights: List<Highlight>,
) {
    data class Highlight(
        val text: String,
        val startTimeSeconds: Int,
        val startIndex: Int,
        val length: Int,
    )

    companion object {
        fun fromJson(json: JsonObject): YouTubeAISummary? {
            val textDict = json.objectOrNull("onResponseReceivedCommand")
                ?.objectOrNull("listMutationCommand")
                ?.objectOrNull("operations")
                ?.arrayOrNull("operations")
                ?.firstOrNull()
                ?.jsonObject
                ?.objectOrNull("insertItemSectionContent")
                ?.arrayOrNull("contents")
                ?.firstOrNull()
                ?.jsonObject
                ?.objectOrNull("youChatItemViewModel")
                ?.objectOrNull("text")
                ?: return null

            val content = textDict.string("content") ?: return null

            val highlights = mutableListOf<Highlight>()
            val runs = textDict.arrayOrNull("commandRuns") ?: emptyList()

            for (run in runs) {
                val item = run.jsonObject
                val startIndex = item.int("startIndex") ?: continue
                val length = item.int("length") ?: continue
                val startTime = item.objectOrNull("onTap")
                    ?.objectOrNull("innertubeCommand")
                    ?.objectOrNull("watchEndpoint")
                    ?.int("startTimeSeconds")
                    ?: continue

                val endIndex = (startIndex + length).coerceAtMost(content.length)
                if (startIndex !in content.indices || endIndex <= startIndex) continue
                val highlightText = content.substring(startIndex, endIndex)

                highlights += Highlight(
                    text = highlightText,
                    startTimeSeconds = startTime,
                    startIndex = startIndex,
                    length = length,
                )
            }

            return YouTubeAISummary(text = content, highlights = highlights)
        }
    }
}
