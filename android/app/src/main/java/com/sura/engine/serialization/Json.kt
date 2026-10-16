package com.sura.engine.serialization

/**
 * Dependency free JSON model + parser + writer.
 * The engine uses this instead of org.json so projects, scenes and saves can be parsed on any
 * platform (unit tests, desktop tooling) and stay 100% offline.
 */
sealed class JsonValue {
    class JObject(val entries: LinkedHashMap<String, JsonValue> = LinkedHashMap()) : JsonValue() {
        operator fun get(key: String): JsonValue? = entries[key]
        operator fun set(key: String, value: JsonValue) { entries[key] = value }
        fun has(key: String): Boolean = entries.containsKey(key)
        fun keys(): Set<String> = entries.keys
        val size: Int get() = entries.size
    }

    class JArray(val items: MutableList<JsonValue> = mutableListOf()) : JsonValue() {
        fun add(value: JsonValue) { items.add(value) }
        operator fun get(index: Int): JsonValue? = items.getOrNull(index)
        val size: Int get() = items.size
    }

    class JString(val value: String) : JsonValue()
    class JNumber(val value: Double) : JsonValue()
    class JBool(val value: Boolean) : JsonValue()
    object JNull : JsonValue()

    // ------------------------------------------------------------ convenience accessors

    val isObject: Boolean get() = this is JObject
    val isArray: Boolean get() = this is JArray
    val isNull: Boolean get() = this is JNull

    fun asObject(): JObject = this as? JObject ?: JObject()
    fun asArray(): JArray = this as? JArray ?: JArray()

    fun asString(default: String = ""): String = when (this) {
        is JString -> value
        is JNumber -> if (value == kotlin.math.floor(value)) value.toLong().toString() else value.toString()
        is JBool -> value.toString()
        else -> default
    }

    fun asDouble(default: Double = 0.0): Double = when (this) {
        is JNumber -> value
        is JString -> value.toDoubleOrNull() ?: default
        is JBool -> if (value) 1.0 else 0.0
        else -> default
    }

    fun asFloat(default: Float = 0f): Float = asDouble(default.toDouble()).toFloat()
    fun asInt(default: Int = 0): Int = asDouble(default.toDouble()).toInt()
    fun asBool(default: Boolean = false): Boolean = when (this) {
        is JBool -> value
        is JNumber -> value != 0.0
        is JString -> value.equals("true", ignoreCase = true) || value == "1"
        else -> default
    }

    fun obj(key: String): JObject? = (this as? JObject)?.get(key)?.asObject()
    fun array(key: String): JArray? = (this as? JObject)?.get(key)?.asArray()
    fun str(key: String, default: String = ""): String = (this as? JObject)?.get(key)?.asString(default) ?: default
    fun num(key: String, default: Double = 0.0): Double = (this as? JObject)?.get(key)?.asDouble(default) ?: default
    fun flt(key: String, default: Float = 0f): Float = num(key, default.toDouble()).toFloat()
    fun int(key: String, default: Int = 0): Int = num(key, default.toDouble()).toInt()
    fun bool(key: String, default: Boolean = false): Boolean = (this as? JObject)?.get(key)?.asBool(default) ?: default
}

class JsonParseException(message: String, val position: Int) : Exception("$message at position $position")

object Json {
    // ------------------------------------------------------------ writing

    fun stringify(value: JsonValue, pretty: Boolean = true, indent: Int = 0): String {
        val sb = StringBuilder()
        write(value, sb, pretty, indent)
        return sb.toString()
    }

    private fun write(value: JsonValue, sb: StringBuilder, pretty: Boolean, indent: Int) {
        when (value) {
            is JsonValue.JObject -> {
                if (value.size == 0) { sb.append("{}"); return }
                sb.append("{")
                val keys = value.keys().toList()
                for ((index, key) in keys.withIndex()) {
                    if (pretty) { sb.append("\n"); repeat(indent + 1) { sb.append("  ") } }
                    sb.append(escape(key)).append(if (pretty) ": " else ":")
                    write(value[key] ?: JsonValue.JNull, sb, pretty, indent + 1)
                    if (index < keys.size - 1) sb.append(",")
                }
                if (pretty) { sb.append("\n"); repeat(indent) { sb.append("  ") } }
                sb.append("}")
            }
            is JsonValue.JArray -> {
                if (value.size == 0) { sb.append("[]"); return }
                sb.append("[")
                for (index in 0 until value.size) {
                    if (pretty) { sb.append("\n"); repeat(indent + 1) { sb.append("  ") } }
                    write(value[index] ?: JsonValue.JNull, sb, pretty, indent + 1)
                    if (index < value.size - 1) sb.append(",")
                }
                if (pretty) { sb.append("\n"); repeat(indent) { sb.append("  ") } }
                sb.append("]")
            }
            is JsonValue.JString -> sb.append(escape(value.value))
            is JsonValue.JNumber -> {
                val d = value.value
                if (d == kotlin.math.floor(d) && !d.isInfinite() && kotlin.math.abs(d) < 1e15) {
                    sb.append(d.toLong().toString())
                } else {
                    sb.append(d.toString())
                }
            }
            is JsonValue.JBool -> sb.append(if (value.value) "true" else "false")
            JsonValue.JNull -> sb.append("null")
        }
    }

    private fun escape(text: String): String {
        val sb = StringBuilder("\"")
        for (ch in text) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch.code < 32) sb.append("\\u").append(String.format("%04x", ch.code)) else sb.append(ch)
            }
        }
        return sb.append("\"").toString()
    }

    // ------------------------------------------------------------ parsing

    fun parse(text: String): JsonValue {
        val parser = JsonParser(text)
        val value = parser.parseValue()
        parser.skipWhitespace()
        return value
    }

    fun parseObject(text: String): JsonValue.JObject = parse(text).asObject()

    // ------------------------------------------------------------ builders

    fun obj(vararg pairs: Pair<String, JsonValue>): JsonValue.JObject {
        val o = JsonValue.JObject()
        for ((key, value) in pairs) o[key] = value
        return o
    }

    fun array(values: List<JsonValue>): JsonValue.JArray {
        val a = JsonValue.JArray()
        for (v in values) a.add(v)
        return a
    }

    fun of(value: String): JsonValue = JsonValue.JString(value)
    fun of(value: Double): JsonValue = JsonValue.JNumber(value)
    fun of(value: Int): JsonValue = JsonValue.JNumber(value.toDouble())
    fun of(value: Float): JsonValue = JsonValue.JNumber(value.toDouble())
    fun of(value: Boolean): JsonValue = JsonValue.JBool(value)
}

private class JsonParser(private val text: String) {
    private var pos = 0

    fun skipWhitespace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }

    fun parseValue(): JsonValue {
        skipWhitespace()
        if (pos >= text.length) throw JsonParseException("unexpected end of input", pos)
        return when (text[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.JString(parseString())
            't' -> { expect("true"); JsonValue.JBool(true) }
            'f' -> { expect("false"); JsonValue.JBool(false) }
            'n' -> { expect("null"); JsonValue.JNull }
            else -> parseNumber()
        }
    }

    private fun parseObject(): JsonValue.JObject {
        val result = JsonValue.JObject()
        pos++ // {
        skipWhitespace()
        if (pos < text.length && text[pos] == '}') { pos++; return result }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            if (pos >= text.length || text[pos] != ':') throw JsonParseException("expected ':'", pos)
            pos++
            result[key] = parseValue()
            skipWhitespace()
            if (pos >= text.length) throw JsonParseException("unterminated object", pos)
            when (text[pos]) {
                ',' -> pos++
                '}' -> { pos++; return result }
                else -> throw JsonParseException("expected ',' or '}'", pos)
            }
        }
    }

    private fun parseArray(): JsonValue.JArray {
        val result = JsonValue.JArray()
        pos++ // [
        skipWhitespace()
        if (pos < text.length && text[pos] == ']') { pos++; return result }
        while (true) {
            result.add(parseValue())
            skipWhitespace()
            if (pos >= text.length) throw JsonParseException("unterminated array", pos)
            when (text[pos]) {
                ',' -> pos++
                ']' -> { pos++; return result }
                else -> throw JsonParseException("expected ',' or ']'", pos)
            }
        }
    }

    private fun parseString(): String {
        if (pos >= text.length || text[pos] != '"') throw JsonParseException("expected string", pos)
        pos++
        val sb = StringBuilder()
        while (pos < text.length && text[pos] != '"') {
            val ch = text[pos]
            if (ch == '\\' && pos + 1 < text.length) {
                pos++
                when (val esc = text[pos]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    'b' -> sb.append('\b')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'u' -> {
                        val hex = text.substring(pos + 1, minOf(pos + 5, text.length))
                        sb.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                        pos += 4
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(ch)
            }
            pos++
        }
        if (pos >= text.length) throw JsonParseException("unterminated string", pos)
        pos++
        return sb.toString()
    }

    private fun parseNumber(): JsonValue.JNumber {
        val start = pos
        if (pos < text.length && (text[pos] == '-' || text[pos] == '+')) pos++
        while (pos < text.length && (text[pos].isDigit() || text[pos] == '.' || text[pos] == 'e' || text[pos] == 'E' || text[pos] == '-' || text[pos] == '+')) pos++
        val numberText = text.substring(start, pos)
        val value = numberText.toDoubleOrNull() ?: throw JsonParseException("invalid number '$numberText'", start)
        return JsonValue.JNumber(value)
    }

    private fun expect(literal: String) {
        if (!text.startsWith(literal, pos)) throw JsonParseException("expected '$literal'", pos)
        pos += literal.length
    }
}
