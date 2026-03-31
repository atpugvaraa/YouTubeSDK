package aaravgupta.youtubesdk.youtubeMusic.models

data class YouTubeMusicHomePage(
    val sections: List<YouTubeMusicSection>,
    val continuationToken: String?,
) {
    val items: List<YouTubeMusicItem>
        get() = sections.flatMap { it.items }
}
