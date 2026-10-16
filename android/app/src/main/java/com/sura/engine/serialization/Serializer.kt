package com.sura.engine.serialization

/**
 * Generic value serializer used by save games and tooling.
 * Accepts Kotlin primitives, lists and maps and converts them to engine [JsonValue] trees.
 */
object Serializer {

    fun toJson(value: Any?): JsonValue = when (value) {
        null -> JsonValue.JNull
        is JsonValue -> value
        is String -> JsonValue.JString(value)
        is Boolean -> JsonValue.JBool(value)
        is Int -> JsonValue.JNumber(value.toDouble())
        is Long -> JsonValue.JNumber(value.toDouble())
        is Float -> JsonValue.JNumber(value.toDouble())
        is Double -> JsonValue.JNumber(value)
        is Number -> JsonValue.JNumber(value.toDouble())
        is Map<*, *> -> {
            val o = JsonValue.JObject()
            for ((k, v) in value) o[k.toString()] = toJson(v)
            o
        }
        is List<*> -> {
            val a = JsonValue.JArray()
            for (item in value) a.add(toJson(item))
            a
        }
        is Array<*> -> {
            val a = JsonValue.JArray()
            for (item in value) a.add(toJson(item))
            a
        }
        else -> JsonValue.JString(value.toString())
    }

    fun stringify(value: Any?, pretty: Boolean = true): String = Json.stringify(toJson(value), pretty)
    fun parse(text: String): JsonValue = Json.parse(text)

    /** Flattens a json tree back into plain Kotlin values. */
    fun toPlain(value: JsonValue): Any? = when (value) {
        is JsonValue.JObject -> {
            val map = LinkedHashMap<String, Any?>()
            for (key in value.keys()) map[key] = toPlain(value[key] ?: JsonValue.JNull)
            map
        }
        is JsonValue.JArray -> value.items.map { toPlain(it) }
        is JsonValue.JString -> value.value
        is JsonValue.JNumber -> value.value
        is JsonValue.JBool -> value.value
        JsonValue.JNull -> null
    }
}
