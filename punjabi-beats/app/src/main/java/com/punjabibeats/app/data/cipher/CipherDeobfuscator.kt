package com.punjabibeats.app.data.cipher

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deobfuscates YouTube `signatureCipher` values (`s` / `sp` / `url` params)
 * and the throttling `n` parameter by executing the logic found in the live
 * player script — first attempting a pure-Kotlin fast path for the classic
 * inline operation sequence (reverse / splice / swap), then falling back to
 * WebView execution of the extracted function source.
 *
 * This is an original implementation of the technique documented by NewPipe,
 * Invidious and the zemer-cipher project; no code is copied from them.
 */
@Singleton
class CipherDeobfuscator @Inject constructor(
    private val scriptRunner: WebViewScriptRunner,
) {

    // ------------------------------------------------------------- signature

    /**
     * Deciphers the scrambled `s` value using the supplied player script.
     * Returns null when the operations cannot be understood (the caller then
     * skips this source and the resolver chain continues).
     */
    suspend fun decipherSignature(playerJs: String, scrambled: String): String? {
        val (name, source) = extractDecipherFunction(playerJs) ?: return null
        inlineOps(source)?.let { ops -> return applyOps(scrambled, ops) }
        val script = "(function(){ try { $source; " +
            "PbBridge.result(String($name('${escapeJs(scrambled)}'))); } " +
            "catch (e) { PbBridge.result(\"\"); } })()"
        return scriptRunner.evaluate(script)?.takeIf { it.isNotBlank() }
    }

    /**
     * Transforms the `n` parameter with the challenge function extracted from
     * the player script (the split/join function that manipulates characters
     * with `charCodeAt`). Returns the transformed value, or null when the
     * challenge cannot be executed — callers keep the un-transformed URL and
     * let [StreamValidator] decide whether it is still usable.
     */
    suspend fun transformNParam(playerJs: String, nParam: String): String? {
        val source = extractNTransformFunction(playerJs) ?: return null
        val script = "(function(){ try { var pbN = ($source)('${escapeJs(nParam)}'); " +
            "PbBridge.result(String(pbN)); } catch (e) { PbBridge.result(\"\"); } })()"
        return scriptRunner.evaluate(script)?.takeIf { it.isNotBlank() }
    }

    // ------------------------------------------------------------- extraction

    /**
     * Locates the decipher function (both assignment `xx=function(a){…}` and
     * declaration `function xx(a){…}` shapes) and slices the complete balanced
     * body, from the function start to the brace after `return …join("")`.
     */
    private fun extractDecipherFunction(playerJs: String): Pair<String, String>? {
        for (regex in listOf(ASSIGNMENT_DECIPHER_RE, DECLARATION_DECIPHER_RE)) {
            val match = regex.find(playerJs) ?: continue
            val name = match.groupValues[1]
            val start = match.range.first
            val joinIdx = playerJs.indexOf(JOIN_EMPTY, start)
            if (joinIdx < 0) continue
            val closeIdx = playerJs.indexOf('}', joinIdx)
            if (closeIdx < 0) continue
            val source = playerJs.substring(start, closeIdx + 1)
            if (source.length <= MAX_FUNCTION_CHARS) return name to source
        }
        return null
    }

    /**
     * The n-transform is the anonymous split/join function containing
     * `charCodeAt` — exactly one in any player build.
     */
    private fun extractNTransformFunction(playerJs: String): String? =
        SPLIT_JOIN_FUNCTION_RE.findAll(playerJs)
            .map { it.value.trim() }
            .firstOrNull { it.contains("charCodeAt") && it.length in 40..MAX_FUNCTION_CHARS }

    // ------------------------------------------------------------- fast path

    /**
     * Fast path: when the decipher body is only inline array operations, run
     * them directly in Kotlin (no WebView round-trip). Returns null when any
     * unknown statement appears (helper-function style bodies).
     */
    private fun inlineOps(functionSource: String): List<SigKind>? {
        val body = functionSource
            .substringAfter(".split(\"\")", "")
            .substringBefore("return", "")
        if (body.isEmpty()) return null
        val ops = mutableListOf<SigKind>()
        var cursor = 0
        while (cursor < body.length) {
            val rest = body.substring(cursor)
            when {
                rest.startsWith(".reverse()") -> {
                    ops += SigKind.Reverse
                    cursor += ".reverse()".length
                }
                rest.startsWith(".splice(0,") -> {
                    val digits = rest.substringAfter(".splice(0,", "").takeWhile { it.isDigit() }
                    if (digits.isEmpty()) return null
                    ops += SigKind.Splice(digits.toInt())
                    cursor += ".splice(0,$digits)".length + 1 // + closing paren
                }
                SWAP_RE.find(rest)?.range?.first == 0 -> {
                    val match = SWAP_RE.find(rest)!!
                    ops += SigKind.Swap(match.groupValues[1].toInt())
                    cursor += match.value.length
                }
                rest.startsWith(";") -> cursor += 1
                else -> return null
            }
        }
        return ops.takeIf { it.isNotEmpty() }
    }

    private fun applyOps(input: String, ops: List<SigKind>): String {
        val chars = input.toMutableList()
        for (op in ops) {
            when (op) {
                SigKind.Reverse -> chars.reverse()
                is SigKind.Splice -> repeat(op.count.coerceAtMost(chars.size)) { chars.removeAt(0) }
                is SigKind.Swap -> {
                    val index = op.position % chars.size
                    val tmp = chars[0]
                    chars[0] = chars[index]
                    chars[index] = tmp
                }
            }
        }
        return chars.joinToString("")
    }

    private sealed interface SigKind {
        data object Reverse : SigKind
        data class Splice(val count: Int) : SigKind
        data class Swap(val position: Int) : SigKind
    }

    private companion object {
        const val JOIN_EMPTY = "join(\"\")"
        const val MAX_FUNCTION_CHARS = 24_000

        /** `var <name>=function(a){a=a.split("");` */
        val ASSIGNMENT_DECIPHER_RE = Regex(
            """(\w+)\s*=\s*function\(\s*(\w+)\s*\)\s*\{\s*\2\s*=\s*\2\.split\(""\)""",
        )

        /** `function <name>(a){a=a.split("");` */
        val DECLARATION_DECIPHER_RE = Regex(
            """function\s+(\w+)\s*\(\s*(\w+)\s*\)\s*\{\s*\2\s*=\s*\2\.split\(""\)""",
        )

        /** Any anonymous `function(a){var x=a.split("")…return x.join("")}` body. */
        val SPLIT_JOIN_FUNCTION_RE = Regex(
            """(?s)function\s*\(\s*\w+\s*\)\s*\{\s*var\s+\w+\s*=\s*\w+\.split\(""\)[\s\S]{10,24000}?return\s+\w+\.join\(""\)\s*;?\s*\}""",
        )

        /** Inline swap op: `var c=a[0];a[0]=a[b%a.length];a[b%a.length]=c;` */
        val SWAP_RE = Regex(
            """var \w+=\w+\[0\];\w+\[0\]=\w+\[(\d+)%\w+\.length\];\w+\[(\d+)%\w+\.length\]=\w+;?""",
        )

        fun escapeJs(value: String): String = buildString {
            for (ch in value) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '\'' -> append("\\'")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    else -> append(ch)
                }
            }
        }
    }
}
