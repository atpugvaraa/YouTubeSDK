package aaravgupta.youtubesdk.youtube.models

sealed class YouTubeSearchResult {
    abstract val id: String

    data class Video(val value: YouTubeVideo) : YouTubeSearchResult() {
        override val id: String
            get() = value.id
    }

    data class Channel(val value: YouTubeChannel) : YouTubeSearchResult() {
        override val id: String
            get() = value.id
    }

    data class Playlist(val value: YouTubePlaylist) : YouTubeSearchResult() {
        override val id: String
            get() = value.id
    }

    companion object {
        fun fromItem(item: YouTubeItem): YouTubeSearchResult? {
            return when (item) {
                is YouTubeItem.Video -> Video(item.value)
                is YouTubeItem.Channel -> Channel(item.value)
                is YouTubeItem.Playlist -> Playlist(item.value)
                is YouTubeItem.Song,
                is YouTubeItem.Shelf,
                -> null
            }
        }
    }
}