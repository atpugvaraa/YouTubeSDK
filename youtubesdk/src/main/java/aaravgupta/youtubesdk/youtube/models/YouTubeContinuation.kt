package aaravgupta.youtubesdk.youtube.models

data class YouTubeContinuation<T>(
    val items: List<T>,
    val continuationToken: String?,
)
