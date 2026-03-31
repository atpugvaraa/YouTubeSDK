package aaravgupta.youtubesdk.youtubeMusic.models

sealed class YouTubeMusicItem {
    data class Song(val value: YouTubeMusicSong) : YouTubeMusicItem()
    data class Album(val value: YouTubeMusicAlbum) : YouTubeMusicItem()
    data class Artist(val value: YouTubeMusicArtist) : YouTubeMusicItem()
    data class Playlist(val value: YouTubeMusicPlaylist) : YouTubeMusicItem()

    val id: String
        get() = when (this) {
            is Song -> value.id
            is Album -> value.id
            is Artist -> value.id
            is Playlist -> value.id
        }
}

data class YouTubeMusicArtistDetail(
    val id: String,
    val sections: List<YouTubeMusicSection>,
)
