package com.punjabibeats.app.core.util

import android.os.Build
import android.text.Html

/** Strips HTML entities that JioSaavn titles/artists frequently contain ("&amp;"). */
fun unescapeHtml(raw: String): String {
    if (!raw.contains('&')) return raw
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(raw)
        }.toString()
    }.getOrDefault(raw)
}

private val consonants: Map<Char, String> = mapOf(
    'ਕ' to "k", 'ਖ' to "kh", 'ਗ' to "g", 'ਘ' to "gh", 'ਙ' to "n",
    'ਚ' to "ch", 'ਛ' to "chh", 'ਜ' to "j", 'ਝ' to "jh", 'ਞ' to "n",
    'ਟ' to "t", 'ਠ' to "th", 'ਡ' to "d", 'ਢ' to "dh", 'ਣ' to "n",
    'ਤ' to "t", 'ਥ' to "th", 'ਦ' to "d", 'ਧ' to "dh", 'ਨ' to "n",
    'ਪ' to "p", 'ਫ' to "ph", 'ਬ' to "b", 'ਭ' to "bh", 'ਮ' to "m",
    'ਯ' to "y", 'ਰ' to "r", 'ਲ' to "l", 'ਵ' to "v", 'ੜ' to "r",
    'ਸ' to "s", 'ਹ' to "h", 'ਲ਼' to "l", 'ਸ਼' to "sh", 'ਜ਼' to "z",
    'ਫ਼' to "f", 'ਖ਼' to "kh", 'ਗ਼' to "gh", '਼' to ""
)

private val matras: Map<Char, String> = mapOf(
    'ਾ' to "aa", 'ਿ' to "i", 'ੀ' to "ee", 'ੁ' to "u", 'ੂ' to "oo",
    'ੇ' to "e", 'ੈ' to "ai", 'ੋ' to "o", 'ੌ' to "au"
)

private val vowels: Map<Char, String> = mapOf(
    'ਅ' to "a", 'ਆ' to "aa", 'ਇ' to "i", 'ਈ' to "ee", 'ਉ' to "u",
    'ਊ' to "oo", 'ਏ' to "e", 'ਐ' to "ai", 'ਓ' to "o", 'ਔ' to "au", 'ੳ' to "o"
)

private val digits: Map<Char, String> = mapOf(
    '੦' to "0", '੧' to "1", '੨' to "2", '੩' to "3", '੪' to "4",
    '੫' to "5", '੬' to "6", '੭' to "7", '੮' to "8", '੯' to "9"
)

fun hasGurmukhi(text: String): Boolean = text.any { it in '੦'..'੯' || it in 'ਅ'..'ੴ' }

/**
 * Roman phonetic transliteration of Gurmukhi (Punjabi) lyrics. Consonants take
 * an inherent "a" unless followed by a matra, halant, nukta or addhak. Text
 * without Gurmukhi glyphs is returned untouched so the toggle degrades safely.
 */
fun transliterateGurmukhi(text: String): String {
    if (!hasGurmukhi(text)) return text
    val chars = text.toCharArray()
    val sb = StringBuilder(text.length + 8)
    var index = 0
    while (index < chars.size) {
        val current = chars[index]
        val next = chars.getOrNull(index + 1)
        when {
            consonants.containsKey(current) -> {
                sb.append(consonants.getValue(current))
                val suppressInherent = next == null || next == '੍' || next == '਼' ||
                    next == 'ੰ' || next == 'ਂ' || next == 'ੱ' || matras.containsKey(next)
                if (next == '੍') index++ // halant consumed below via index++
                if (!suppressInherent) sb.append('a')
            }
            matras.containsKey(current) -> sb.append(matras.getValue(current))
            vowels.containsKey(current) -> sb.append(vowels.getValue(current))
            digits.containsKey(current) -> sb.append(digits.getValue(current))
            current == 'ੱ' -> { /* addhak: gemination not reconstructed, skipped */ }
            else -> sb.append(current)
        }
        index++
    }
    return sb.toString().replace(Regex(" +"), " ").trim()
}
