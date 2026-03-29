package net.tautellini.backend.arenatactics.services

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

val firestoreJson = Json { ignoreUnknownKeys = true }

fun mapToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to mapToJsonElement(v) })
    is List<*> -> JsonArray(value.map { mapToJsonElement(it) })
    else -> JsonPrimitive(value.toString())
}

inline fun <reified T> fromFirestore(data: Map<String, Any>): T {
    val jsonElement = mapToJsonElement(data)
    return firestoreJson.decodeFromJsonElement(serializer(), jsonElement)
}

inline fun <reified T> toFirestore(value: T): Map<String, Any> {
    val jsonElement = firestoreJson.encodeToJsonElement(serializer(), value)
    return jsonElementToMap(jsonElement)
}

@Suppress("UNCHECKED_CAST")
fun jsonElementToMap(element: JsonElement): Map<String, Any> {
    val obj = element as JsonObject
    return obj.entries.associate { (key, value) -> key to jsonElementToAny(value) }
}

fun jsonElementToAny(element: JsonElement): Any = when (element) {
    is JsonNull -> "" // Firestore doesn't support null values well, use empty string
    is JsonPrimitive -> when {
        element.isString -> element.content
        element.content == "true" || element.content == "false" -> element.content.toBoolean()
        element.content.contains('.') -> element.content.toDouble()
        else -> element.content.toLongOrNull() ?: element.content
    }
    is JsonArray -> element.map { jsonElementToAny(it) }
    is JsonObject -> element.entries.associate { (k, v) -> k to jsonElementToAny(v) }
}
