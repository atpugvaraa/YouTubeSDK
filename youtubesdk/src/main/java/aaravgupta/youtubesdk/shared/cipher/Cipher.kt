package aaravgupta.youtubesdk.shared.cipher

import aaravgupta.youtubesdk.shared.network.NetworkClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object Cipher {
    @JvmField
    val shared: Cipher = this

    private val engine = DecipherEngine()
    private val initLock = Mutex()

    @Volatile
    private var cachedScriptUrl: String? = null

    @Volatile
    private var isEngineReady: Boolean = false

    suspend fun getCipherScriptUrl(network: NetworkClient): String {
        cachedScriptUrl?.let { return it }

        val htmlData = network.getAbsolute("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        val html = htmlData.decodeToString()

        val pattern = Regex("/s/player/[a-zA-Z0-9]+/[a-zA-Z0-9_.]+/([a-zA-Z0-9_]{2,}/)?base\\.js")
        val path = pattern.find(html)?.value
            ?: throw IOException("Could not find player script URL in watch page HTML")

        val fullUrl = "https://www.youtube.com$path"
        cachedScriptUrl = fullUrl
        return fullUrl
    }

    suspend fun decipher(
        url: String,
        signatureCipher: String,
        network: NetworkClient,
    ): String {
        initLock.withLock {
            if (!isEngineReady) {
                val scriptUrl = getCipherScriptUrl(network)
                val scriptData = network.getAbsolute(scriptUrl)
                val script = scriptData.decodeToString()
                if (script.isBlank()) {
                    throw IOException("Player script is empty")
                }
                engine.loadCipherScript(script)
                isEngineReady = true
            }
        }

        val cipherParams = parseCipher(signatureCipher)
        val signature = cipherParams["s"] ?: return cipherParams["url"] ?: url
        val signatureParam = cipherParams["sp"] ?: return cipherParams["url"] ?: url

        val decryptedSignature = engine.decipher(signature)

        val sourceUrl = cipherParams["url"] ?: url
        val urlBuilder = sourceUrl.toHttpUrlOrNull()?.newBuilder()
            ?: throw IOException("Invalid decipher source URL")
        urlBuilder.addQueryParameter(signatureParam, decryptedSignature)

        return urlBuilder.build().toString()
    }

    fun parseCipher(cipher: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        cipher.split('&').forEach { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2) {
                val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
                val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                params[key] = value
            }
        }

        return params
    }
}
