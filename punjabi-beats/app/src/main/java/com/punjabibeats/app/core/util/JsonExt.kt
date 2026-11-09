package com.punjabibeats.app.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Tolerant accessors for loosely-typed backend JSON. Every helper returns null
 * instead of throwing so a single unexpected field can never break playback.
 */
fun JsonObject.firstString(vararg keys: String): String? {
    for (key in keys) {
        when (val value = this[key]) {
            is JsonNull, null -> continue
            is JsonPrimitive -> {
                val raw = value.content
                if (raw.isNotBlank() && raw != "null") return raw
            }
            else -> continue
        }
    }
    return null
}

fun JsonObject.firstLong(vararg keys: String): Long? =
    firstString(*keys)?.replace(Regex("[^0-9-]"), "")?.toLongOrNull()

fun JsonObject.firstInt(vararg keys: String): Int? =
    firstString(*keys)?.replace(Regex("[^0-9-]"), "")?.toIntOrNull()

fun JsonObject.firstDouble(vararg keys: String): Double? =
    firstString(*keys)?.toDoubleOrNull()

fun JsonObject.firstArray(vararg keys: String): JsonArray {
    for (key in keys) {
        (this[key] as? JsonArray)?.let { return it }
    }
    return JsonArray(emptyList())
}

fun JsonObject.firstObject(vararg keys: String): JsonObject? {
    for (key in keys) {
        when (val value = this[key]) {
            is JsonObject -> return value
            else -> continue
        }
    }
    return null
}

fun JsonObject.objectArray(vararg keys: String): List<JsonObject> {
    for (key in keys) {
        val list = (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
        if (list.isNotEmpty()) return list
    }
    return emptyList()
}

fun JsonElement.asStringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
