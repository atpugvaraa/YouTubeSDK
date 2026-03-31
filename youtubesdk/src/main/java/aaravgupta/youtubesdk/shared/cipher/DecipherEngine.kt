package aaravgupta.youtubesdk.shared.cipher

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import java.io.IOException

internal class DecipherEngine {
    @Volatile
    private var scope: Scriptable? = null

    @Volatile
    private var decipherFunctionName: String? = null

    @Synchronized
    fun loadCipherScript(script: String) {
        val context = Context.enter()
        try {
            context.optimizationLevel = -1
            val runtimeScope = context.initStandardObjects()
            context.evaluateString(runtimeScope, script, "base.js", 1, null)

            val pattern = Regex(
                "([a-zA-Z0-9_$]+)\\s*=\\s*function\\([a-zA-Z0-9_$]+\\)\\s*\\{\\s*[a-zA-Z0-9_$]+\\s*=\\s*[a-zA-Z0-9_$]+\\.split\\(\\\"\\\"\\)[\\s\\S]*?\\}"
            )
            val functionName = pattern.find(script)?.groupValues?.getOrNull(1)
                ?: throw IOException("Could not locate decipher function in player script")

            scope = runtimeScope
            decipherFunctionName = functionName
        } finally {
            Context.exit()
        }
    }

    @Synchronized
    fun decipher(signature: String): String {
        val functionName = decipherFunctionName
            ?: throw IOException("Decipher engine not initialized")
        val runtimeScope = scope
            ?: throw IOException("Decipher scope missing")

        val context = Context.enter()
        try {
            context.optimizationLevel = -1
            val escaped = signature
                .replace("\\", "\\\\")
                .replace("'", "\\'")

            val result = context.evaluateString(
                runtimeScope,
                "$functionName('$escaped')",
                "decipher",
                1,
                null,
            )
            val decrypted = Context.toString(result)
            if (decrypted.isBlank() || decrypted == "undefined") {
                throw IOException("Decipher script returned an invalid signature")
            }
            return decrypted
        } finally {
            Context.exit()
        }
    }
}
