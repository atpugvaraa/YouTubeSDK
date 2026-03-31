package aaravgupta.youtubesdk.youtubeOAuth

import android.content.Context
import aaravgupta.youtubesdk.shared.ClientConfig
import aaravgupta.youtubesdk.shared.InnerTubeContext
import aaravgupta.youtubesdk.shared.Keychain
import aaravgupta.youtubesdk.shared.network.NetworkClient

class YouTubeOAuthClient {
    private val network: NetworkClient = NetworkClient(
        context = InnerTubeContext(client = ClientConfig.ios, cookies = loadCookies()),
    )

    suspend fun validateSession(): Boolean {
        return try {
            val data = network.get("account/account_menu")
            data.decodeToString().contains("googleAccountHeaderRenderer")
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val sharedCookieKey: String = "youtube_user_cookies"

        fun initialize(context: Context) {
            Keychain.initialize(context)
        }

        fun saveCookies(cookies: String) {
            Keychain.save(cookies, key = sharedCookieKey)
        }

        fun loadCookies(): String? = Keychain.load(key = sharedCookieKey)

        fun logout() {
            Keychain.delete(key = sharedCookieKey)
        }

        fun cookieKey(): String = sharedCookieKey
    }
}
