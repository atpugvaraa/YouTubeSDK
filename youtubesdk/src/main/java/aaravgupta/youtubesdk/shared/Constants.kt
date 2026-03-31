package aaravgupta.youtubesdk.shared

object YouTubeSdkConstants {
    object Urls {
        const val googleSearchBaseUrl = "https://www.google.com"
        const val youtubeBaseUrl = "https://www.youtube.com"
        const val youtubeMusicBaseUrl = "https://music.youtube.com"
        const val youtubeSuggestions = "https://suggestqueries-clients6.youtube.com"
        const val youtubeUpload = "https://upload.youtube.com"

        object Api {
            const val baseUrl = "https://youtubei.googleapis.com"

            const val youtubeInnerTubeUrl = "https://www.youtube.com/youtubei"
            const val youtubeMusicInnerTubeUrl = "https://music.youtube.com/youtubei"
            const val youtubeChartsInnerTubeUrl = "https://charts.youtube.com/youtubei"

            const val youtubeSuggestionsUrl = "https://suggestqueries-clients6.youtube.com/complete"

            const val googleApisInnerTubeUrl = "https://youtubei.googleapis.com/youtubei"
            const val stagingUrl = "https://green-youtubei.sandbox.googleapis.com/youtubei"
            const val releaseUrl = "https://release-youtubei.sandbox.googleapis.com/youtubei"
            const val testUrl = "https://test-youtubei.sandbox.googleapis.com/youtubei"
            const val camiUrl = "https://cami-youtubei.sandbox.googleapis.com/youtubei"
            const val uytfeUrl = "https://uytfe.sandbox.googleapis.com/youtubei"
        }
    }

    object InternalKeys {
        object Renderers {
            const val video = "videoRenderer"
            const val gridVideo = "gridVideoRenderer"
            const val compactVideo = "compactVideoRenderer"
            const val videoWithContext = "videoWithContextRenderer"
            const val reelItem = "reelItemRenderer"
            const val richItem = "richItemRenderer"
            const val itemSection = "itemSectionRenderer"
            const val shelf = "shelfRenderer"
            const val musicVideo = "musicVideoRenderer"
            const val musicResponsiveListItem = "musicResponsiveListItemRenderer"
            const val playlistVideo = "playlistVideoRenderer"
            const val channel = "channelRenderer"
            const val playlist = "playlistRenderer"
            const val musicShelf = "musicShelfRenderer"
            const val musicCarouselShelf = "musicCarouselShelfRenderer"
        }

        object BrowseIds {
            const val home = "FEwhat_to_watch"
            const val trending = "FEtrending"

            object Music {
                const val home = "FEmusic_home"
                const val explore = "FEmusic_explore"
                const val charts = "FEmusic_charts"
                const val newReleases = "FEmusic_new_releases"
                const val moods = "FEmusic_moods_and_genres"
                const val library = "FEmusic_library"
                const val history = "FEmusic_history"
                const val likedVideos = "FEmusic_liked_videos"
            }
        }
    }
}
