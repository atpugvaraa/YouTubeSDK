package aaravgupta.youtubesdk.youtube.models

import aaravgupta.youtubesdk.youtubeMusic.models.YouTubeMusicSong

sealed class YouTubeItem {
    data class Video(val value: YouTubeVideo) : YouTubeItem()
    data class Song(val value: YouTubeMusicSong) : YouTubeItem()
    data class Playlist(val value: YouTubePlaylist) : YouTubeItem()
    data class Channel(val value: YouTubeChannel) : YouTubeItem()
    data class Shelf(val value: YouTubeShelf) : YouTubeItem()
}

data class YouTubeShelf(
    val title: String,
    val items: List<YouTubeItem>,
)
