package com.sura.engine.serialization

import com.sura.engine.animation.Animator
import com.sura.engine.particles.ParticleSystem
import com.sura.engine.physics.BoxCollider2D
import com.sura.engine.physics.BodyType
import com.sura.engine.physics.CircleCollider2D
import com.sura.engine.physics.RigidBody2D
import com.sura.engine.rendering.CameraComponent
import com.sura.engine.rendering.Light2D
import com.sura.engine.rendering.SpriteRenderer
import com.sura.engine.scene.Component
import com.sura.engine.scene.GameObject
import com.sura.engine.scene.Scene
import com.sura.engine.scripting.ScriptBehaviour
import com.sura.engine.tilemap.Tilemap
import com.sura.engine.tilemap.TilemapComponent
import com.sura.engine.utils.Color

/**
 * Converts scenes to and from JSON so projects can be saved and shared.
 * Every supported component has an explicit reader/writer pair (no reflection => fast + predictable).
 */
class SceneSerializer {
    companion object {
        const val FORMAT_VERSION = 1
        const val SCENE_EXTENSION = ".scene"
    }

    // ------------------------------------------------------------- serialize

    fun serialize(scene: Scene): String {
        val root = JsonValue.JObject()
        root["format"] = Json.of("sura.scene")
        root["version"] = Json.of(FORMAT_VERSION)
        root["name"] = Json.of(scene.name)
        val objects = JsonValue.JArray()
        for (go in scene.allObjects) objects.add(serializeObject(go))
        root["objects"] = objects
        return Json.stringify(root, true)
    }

    fun serializeObject(go: GameObject): JsonValue.JObject {
        val o = JsonValue.JObject()
        o["name"] = Json.of(go.name)
        o["tag"] = Json.of(go.tag)
        o["layer"] = Json.of(go.layer)
        o["active"] = Json.of(go.active)

        val t = JsonValue.JObject()
        t["x"] = Json.of(go.transform.position.x)
        t["y"] = Json.of(go.transform.position.y)
        t["rotation"] = Json.of(go.transform.rotation)
        t["scaleX"] = Json.of(go.transform.scale.x)
        t["scaleY"] = Json.of(go.transform.scale.y)
        t["zOrder"] = Json.of(go.transform.zOrder)
        o["transform"] = t

        val components = JsonValue.JArray()
        for (component in go.components) {
            val data = serializeComponent(component) ?: continue
            components.add(data)
        }
        o["components"] = components
        return o
    }

    private fun serializeComponent(component: Component): JsonValue.JObject? {
        val o = JsonValue.JObject()
        when (component) {
            is SpriteRenderer -> {
                o["type"] = Json.of("SpriteRenderer")
                o["enabled"] = Json.of(component.enabled)
                o["color"] = Json.of(colorToHex(component.color))
                o["sizeX"] = Json.of(component.size.x)
                o["sizeY"] = Json.of(component.size.y)
                o["sortingLayer"] = Json.of(component.sortingLayer)
                o["orderInLayer"] = Json.of(component.orderInLayer)
            }
            is RigidBody2D -> {
                o["type"] = Json.of("RigidBody2D")
                o["enabled"] = Json.of(component.enabled)
                o["bodyType"] = Json.of(component.bodyType.name)
                o["mass"] = Json.of(component.mass)
                o["gravityScale"] = Json.of(component.gravityScale)
                o["linearDrag"] = Json.of(component.linearDrag)
                o["useGravity"] = Json.of(component.useGravity)
                o["velocityX"] = Json.of(component.velocity.x)
                o["velocityY"] = Json.of(component.velocity.y)
            }
            is BoxCollider2D -> {
                o["type"] = Json.of("BoxCollider2D")
                o["enabled"] = Json.of(component.enabled)
                o["sizeX"] = Json.of(component.size.x)
                o["sizeY"] = Json.of(component.size.y)
                o["isTrigger"] = Json.of(component.isTrigger)
                o["friction"] = Json.of(component.friction)
                o["restitution"] = Json.of(component.restitution)
            }
            is CircleCollider2D -> {
                o["type"] = Json.of("CircleCollider2D")
                o["enabled"] = Json.of(component.enabled)
                o["radius"] = Json.of(component.radius)
                o["isTrigger"] = Json.of(component.isTrigger)
                o["friction"] = Json.of(component.friction)
            }
            is Light2D -> {
                o["type"] = Json.of("Light2D")
                o["enabled"] = Json.of(component.enabled)
                o["color"] = Json.of(colorToHex(component.color))
                o["radius"] = Json.of(component.radius)
                o["intensity"] = Json.of(component.intensity)
                o["flicker"] = Json.of(component.flicker)
            }
            is CameraComponent -> {
                o["type"] = Json.of("CameraComponent")
                o["enabled"] = Json.of(component.enabled)
                o["isMain"] = Json.of(component.isMain)
                o["zoom"] = Json.of(component.zoom)
                o["followTarget"] = Json.of(component.followTargetName)
            }
            is ParticleSystem -> {
                o["type"] = Json.of("ParticleSystem")
                o["enabled"] = Json.of(component.enabled)
                o["maxParticles"] = Json.of(component.maxParticles)
                o["emissionRate"] = Json.of(component.emissionRate)
                o["lifetime"] = Json.of(component.lifetime)
                o["speed"] = Json.of(component.speed)
                o["sizeStart"] = Json.of(component.sizeStart)
                o["sizeEnd"] = Json.of(component.sizeEnd)
                o["colorStart"] = Json.of(colorToHex(component.colorStart))
                o["colorEnd"] = Json.of(colorToHex(component.colorEnd))
                o["gravityX"] = Json.of(component.gravity.x)
                o["gravityY"] = Json.of(component.gravity.y)
                o["playing"] = Json.of(component.isPlaying)
                o["loop"] = Json.of(component.loop)
            }
            is Animator -> {
                o["type"] = Json.of("Animator")
                o["enabled"] = Json.of(component.enabled)
                o["speed"] = Json.of(component.speed)
                o["currentClip"] = Json.of(component.currentClip ?: "")
            }
            is TilemapComponent -> {
                o["type"] = Json.of("TilemapComponent")
                o["enabled"] = Json.of(component.enabled)
                o["tileSize"] = Json.of(component.tileSize)
                val map = component.tilemap
                if (map != null) {
                    o["width"] = Json.of(map.width)
                    o["height"] = Json.of(map.height)
                    val layers = JsonValue.JArray()
                    for (layer in map.layers) {
                        val layerJson = JsonValue.JObject()
                        layerJson["name"] = Json.of(layer.name)
                        layerJson["visible"] = Json.of(layer.visible)
                        layerJson["solid"] = Json.of(layer.solid)
                        layerJson["color"] = Json.of(colorToHex(layer.color))
                        val tiles = JsonValue.JArray()
                        for (tile in layer.tiles) tiles.add(Json.of(tile))
                        layerJson["tiles"] = tiles
                        layers.add(layerJson)
                    }
                    o["layers"] = layers
                }
            }
            is ScriptBehaviour -> {
                o["type"] = Json.of("ScriptBehaviour")
                o["enabled"] = Json.of(component.enabled)
                o["scriptName"] = Json.of(component.scriptName)
                o["source"] = Json.of(component.source)
            }
            else -> return null
        }
        return o
    }

    // ------------------------------------------------------------- deserialize

    fun deserialize(json: String, fallbackName: String = "Scene"): Scene {
        val root = try { Json.parse(json).asObject() } catch (e: JsonParseException) {
            throw SceneFormatException("Scene JSON is invalid: ${e.message}")
        }
        val scene = Scene(root.str("name", fallbackName))
        val objects = root.array("objects") ?: return scene
        for (index in 0 until objects.size) {
            val objectJson = objects[index]?.asObject() ?: continue
            scene.adopt(deserializeObject(objectJson, scene))
        }
        return scene
    }

    fun deserializeObject(json: JsonValue.JObject, scene: Scene? = null): GameObject {
        val go = GameObject(json.str("name", "GameObject"))
        go.tag = json.str("tag", "Untagged")
        go.layer = json.int("layer", 0)
        go.active = json.bool("active", true)
        go.scene = scene

        json.obj("transform")?.let { t ->
            go.transform.position.set(t.flt("x"), t.flt("y"))
            go.transform.rotation = t.flt("rotation")
            go.transform.scale.set(t.flt("scaleX", 1f), t.flt("scaleY", 1f))
            go.transform.zOrder = t.int("zOrder")
        }

        val components = json.array("components") ?: return go
        for (index in 0 until components.size) {
            val componentJson = components[index]?.asObject() ?: continue
            val component = deserializeComponent(componentJson) ?: continue
            component.enabled = componentJson.bool("enabled", true)
            go.addComponent(component)
        }
        return go
    }

    fun deserializeComponent(json: JsonValue.JObject): Component? {
        return when (json.str("type")) {
            "SpriteRenderer" -> SpriteRenderer().apply {
                color = hexToColor(json.str("color", "#FFFFFFFF"))
                size.set(json.flt("sizeX", 1f), json.flt("sizeY", 1f))
                sortingLayer = json.int("sortingLayer")
                orderInLayer = json.int("orderInLayer")
            }
            "RigidBody2D" -> RigidBody2D().apply {
                bodyType = try { BodyType.valueOf(json.str("bodyType", "DYNAMIC")) } catch (e: Throwable) { BodyType.DYNAMIC }
                mass = json.flt("mass", 1f)
                gravityScale = json.flt("gravityScale", 1f)
                linearDrag = json.flt("linearDrag")
                useGravity = json.bool("useGravity", true)
                velocity.set(json.flt("velocityX"), json.flt("velocityY"))
            }
            "BoxCollider2D" -> BoxCollider2D().apply {
                size.set(json.flt("sizeX", 1f), json.flt("sizeY", 1f))
                isTrigger = json.bool("isTrigger")
                friction = json.flt("friction", 0.5f)
                restitution = json.flt("restitution")
            }
            "CircleCollider2D" -> CircleCollider2D().apply {
                radius = json.flt("radius", 0.5f)
                isTrigger = json.bool("isTrigger")
                friction = json.flt("friction", 0.5f)
            }
            "Light2D" -> Light2D().apply {
                color = hexToColor(json.str("color", "#FFF2CCFF"))
                radius = json.flt("radius", 240f)
                intensity = json.flt("intensity", 1f)
                flicker = json.bool("flicker")
            }
            "CameraComponent" -> CameraComponent().apply {
                isMain = json.bool("isMain", true)
                zoom = json.flt("zoom", 1f)
                followTargetName = json.str("followTarget")
            }
            "ParticleSystem" -> ParticleSystem().apply {
                maxParticles = json.int("maxParticles", 200)
                emissionRate = json.flt("emissionRate", 20f)
                lifetime = json.flt("lifetime", 1f)
                speed = json.flt("speed", 100f)
                sizeStart = json.flt("sizeStart", 8f)
                sizeEnd = json.flt("sizeEnd")
                colorStart = hexToColor(json.str("colorStart", "#FFFFFFFF"))
                colorEnd = hexToColor(json.str("colorEnd", "#FFFFFF00"))
                gravity.set(json.flt("gravityX"), json.flt("gravityY"))
                isPlaying = json.bool("playing", true)
                loop = json.bool("loop", true)
            }
            "Animator" -> Animator().apply {
                speed = json.flt("speed", 1f)
                val clip = json.str("currentClip")
                if (clip.isNotEmpty()) play(clip)
            }
            "TilemapComponent" -> TilemapComponent().apply {
                tileSize = json.flt("tileSize", 32f)
                val width = json.int("width")
                val height = json.int("height")
                if (width > 0 && height > 0) {
                    val map = Tilemap(width, height, tileSize)
                    val layers = json.array("layers")
                    if (layers != null) {
                        for (index in 0 until layers.size) {
                            val layerJson = layers[index]?.asObject() ?: continue
                            val layer = map.addLayer(layerJson.str("name", "Layer$index"))
                            layer.visible = layerJson.bool("visible", true)
                            layer.solid = layerJson.bool("solid")
                            layer.color = hexToColor(layerJson.str("color", "#FFFFFFFF"))
                            val tiles = layerJson.array("tiles")
                            if (tiles != null) {
                                for (t in 0 until minOf(tiles.size, layer.tiles.size)) {
                                    layer.tiles[t] = tiles[t]?.asInt(-1) ?: -1
                                }
                            }
                        }
                    }
                    tilemap = map
                }
            }
            "ScriptBehaviour" -> ScriptBehaviour().apply {
                scriptName = json.str("scriptName", "Script")
                source = json.str("source")
            }
            else -> null
        }
    }

    // ------------------------------------------------------------- helpers

    fun colorToHex(color: Color): String {
        val r = (color.r.coerceIn(0f, 1f) * 255).toInt()
        val g = (color.g.coerceIn(0f, 1f) * 255).toInt()
        val b = (color.b.coerceIn(0f, 1f) * 255).toInt()
        val a = (color.a.coerceIn(0f, 1f) * 255).toInt()
        return String.format("#%02X%02X%02X%02X", r, g, b, a)
    }

    fun hexToColor(hex: String, fallback: Color = Color.WHITE): Color {
        val text = hex.trim().removePrefix("#")
        return try {
            when (text.length) {
                8 -> Color(
                    text.substring(0, 2).toInt(16) / 255f,
                    text.substring(2, 4).toInt(16) / 255f,
                    text.substring(4, 6).toInt(16) / 255f,
                    text.substring(6, 8).toInt(16) / 255f
                )
                6 -> Color(
                    text.substring(0, 2).toInt(16) / 255f,
                    text.substring(2, 4).toInt(16) / 255f,
                    text.substring(4, 6).toInt(16) / 255f,
                    1f
                )
                else -> fallback
            }
        } catch (e: Throwable) { fallback }
    }

    fun isSceneJson(text: String): Boolean = text.trimStart().startsWith("{") && text.contains("\"objects\"")
}

class SceneFormatException(message: String) : Exception(message)
