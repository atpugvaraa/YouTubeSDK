package aaravgupta.youtubesdk.shared

/**
 * Defines the static identity of a YouTube client.
 * These values mirror known InnerTube client presets.
 */
data class ClientConfig(
    val name: String,
    val version: String,
    val apiKey: String,
    val userAgent: String,
    val clientNameId: String,
) {
    companion object {
        /** Standard web client, best for charts/public data. */
        val web = ClientConfig(
            name = "WEB",
            version = "2.20240308.00.00",
            apiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            clientNameId = "1",
        )

        /** YouTube Music web client, best for metadata/lyrics. */
        val webRemix = ClientConfig(
            name = "WEB_REMIX",
            version = "1.20240308.00.00",
            apiKey = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            clientNameId = "67",
        )

        val webMusicAnalytics = ClientConfig(
            name = "WEB_MUSIC_ANALYTICS",
            version = "2.0",
            apiKey = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            clientNameId = "31",
        )

        /** iOS client, used in Swift SDK as default generic video/search client. */
        val ios = ClientConfig(
            name = "IOS",
            version = "20.11.6",
            apiKey = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
            userAgent = "com.google.ios.youtube/20.11.6 (iPhone10,4; U; CPU iOS 16_7_7 like Mac OS X)",
            clientNameId = "5",
        )

        val iosMusic = ClientConfig(
            name = "IOS_MUSIC",
            version = "6.43.52",
            apiKey = "AIzaSyBAETezhkwP0ZWA02RsqT1zu78Fpt0bC_s",
            userAgent = "com.google.ios.youtube/20.11.6 (iPhone10,4; U; CPU iOS 16_7_7 like Mac OS X)",
            clientNameId = "26",
        )

        /** Android client, useful as fallback for specific endpoints. */
        val android = ClientConfig(
            name = "ANDROID",
            version = "19.35.36",
            apiKey = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
            userAgent = "com.google.android.youtube/19.35.36 (Linux; U; Android 13; en_US) gzip",
            clientNameId = "3",
        )

        val androidMusic = ClientConfig(
            name = "ANDROID_MUSIC",
            version = "6.43.52",
            apiKey = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
            userAgent = "com.google.android.apps.youtube.music/6.43.52 (Linux; U; Android 13; en_US) gzip",
            clientNameId = "21",
        )

        /** TV client is commonly used by device/code auth flows. */
        val tv = ClientConfig(
            name = "TVHTML5",
            version = "7.20250219.14.00",
            apiKey = "AIzaSyBUPetSUmoZL-OhlxA7wSac5XinrygCqMo",
            userAgent = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Version",
            clientNameId = "7",
        )
    }
}
