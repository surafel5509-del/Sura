package com.sura.engine.serialization

import org.json.JSONArray
import org.json.JSONObject

object Serializer {
    fun toJson(obj: Any?): JSONObject {
        val o = JSONObject()
        when (obj) {
            is Map<*, *> -> for ((k,v) in obj) o.put(k.toString(), toJsonValue(v))
            is List<*> -> { val a = JSONArray(); for (v in obj) a.put(toJsonValue(v)); o.put("list", a) }
            else -> o.put("value", obj)
        }
        return o
    }
    private fun toJsonValue(v: Any?): Any {
        return when (v) {
            null -> JSONObject.NULL
            is String, is Number, is Boolean -> v
            is Map<*, *> -> toJson(v)
            is List<*> -> { val a = JSONArray(); for (x in v) a.put(toJsonValue(x)); a }
            else -> v.toString()
        }
    }
    fun fromJson(s: String): JSONObject = JSONObject(s)
}
