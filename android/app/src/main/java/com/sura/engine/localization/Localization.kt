package com.sura.engine.localization

/** Offline string table used by games and the editor UI. */
class Localization(val defaultLanguage: String = "en") {
    private val tables = mutableMapOf<String, MutableMap<String, String>>()
    var language: String = defaultLanguage

    init {
        tables["en"] = mutableMapOf()
    }

    fun addLanguage(code: String) { tables.getOrPut(code) { mutableMapOf() } }

    fun set(key: String, value: String, language: String = defaultLanguage) {
        tables.getOrPut(language) { mutableMapOf() }[key] = value
    }

    fun setAll(language: String, values: Map<String, String>) {
        tables.getOrPut(language) { mutableMapOf() }.putAll(values)
    }

    fun translate(key: String, fallback: String = key): String {
        tables[language]?.get(key)?.let { return it }
        tables[defaultLanguage]?.get(key)?.let { return it }
        return fallback
    }

    fun has(key: String): Boolean = tables[language]?.containsKey(key) == true
    fun languages(): List<String> = tables.keys.toList()
    fun count(language: String = this.language): Int = tables[language]?.size ?: 0
}
