package aaravgupta.youtubesdk

import aaravgupta.youtubesdk.youtube.YouTubeClient
import aaravgupta.youtubesdk.youtubeCharts.YouTubeChartsClient
import aaravgupta.youtubesdk.youtubeMusic.YouTubeMusicClient
import aaravgupta.youtubesdk.youtubeOAuth.YouTubeOAuthClient

/**
 * Central entry point for YouTubeSDK functionality.
 */
class YouTube(cookies: String? = null) {

    var cookies: String? = cookies ?: YouTubeOAuthClient.loadCookies()
        set(value) {
            field = value
            updateClients()
        }

    var main: YouTubeClient = YouTubeClient(cookies = this.cookies)
        private set

    var music: YouTubeMusicClient = YouTubeMusicClient(cookies = this.cookies)
        private set

    var charts: YouTubeChartsClient = YouTubeChartsClient()
        private set

    var oauth: YouTubeOAuthClient = YouTubeOAuthClient()
        private set

    @Synchronized
    private fun updateClients() {
        main = YouTubeClient(cookies = cookies)
        music = YouTubeMusicClient(cookies = cookies)
    }

    companion object {
        @JvmField
        val shared = YouTube()
    }
}
