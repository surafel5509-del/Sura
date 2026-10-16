package com.sura.engine.editor

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
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
import com.sura.engine.scripting.ScriptBehaviour
import com.sura.engine.serialization.SceneSerializer
import com.sura.engine.tilemap.TilemapComponent
import com.sura.engine.utils.Color as SuraColor

/** Unity style inspector: transforms, components, editable values with undo support. */
class InspectorView(context: Context, private val state: EditorState) : LinearLayout(context) {

    private val scroll = ScrollView(context)
    private val content = LinearLayout(context).apply { orientation = VERTICAL }

    init {
        orientation = VERTICAL
        setBackgroundColor(EditorTheme.panel)
        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(EditorTheme.panelAlt)
        }
        headerRow.addView(EditorTheme.label(context, "  INSPECTOR", 12f, EditorTheme.accent, true), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        headerRow.addView(EditorTheme.button(context, "Add +") { showAddComponentMenu() })
        addView(headerRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(EditorTheme.divider(context))
        scroll.addView(content)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        state.subscribe { post { refresh() } }
        refresh()
    }

    fun refresh() {
        content.removeAllViews()
        val go = state.selection
        if (go == null) {
            content.addView(EditorTheme.label(context, "Nothing selected.\n\nSelect an object in the Hierarchy panel, or tap + there to create one.", 11.5f, EditorTheme.textFaint))
            return
        }

        // ---- object header
        val nameField = EditText(context).apply {
            setText(go.name)
            setTextColor(EditorTheme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setBackgroundColor(EditorTheme.panelAlt)
            setPadding(EditorTheme.dp(context, 8), EditorTheme.dp(context, 6), EditorTheme.dp(context, 8), EditorTheme.dp(context, 6))
            setSingleLine()
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && text.toString() != go.name) {
                    val previous = go.name
                    val updated = text.toString().ifBlank { previous }
                    go.name = updated
                    state.isDirty = true
                    state.undoStack.push("Rename ${previous}", { go.name = previous }, { go.name = updated })
                    state.notifyChanged()
                }
            }
        }
        content.addView(nameField, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val identity = EditorTheme.label(context, "Tag: ${go.tag}   Layer: ${go.layer}   Children: ${go.children.size}", 10.5f, EditorTheme.textDim)
        content.addView(identity)

        val activeRow = EditorTheme.row(context, 30)
        activeRow.addView(EditorTheme.label(context, "Active", 11.5f), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val activeBox = CheckBox(context).apply {
            isChecked = go.active
            setOnCheckedChangeListener { _, checked -> go.active = checked; state.notifyChanged() }
        }
        activeRow.addView(activeBox)
        content.addView(activeRow)

        // ---- transform
        section("Transform")
        numberRow("Position X", go.transform.position.x) { value ->
            val previous = go.transform.position.x
            go.transform.position.x = value
            pushChange("Move X", { go.transform.position.x = previous }, { go.transform.position.x = value })
        }
        numberRow("Position Y", go.transform.position.y) { value ->
            val previous = go.transform.position.y
            go.transform.position.y = value
            pushChange("Move Y", { go.transform.position.y = previous }, { go.transform.position.y = value })
        }
        numberRow("Rotation", go.transform.rotation, step = 15f) { value ->
            val previous = go.transform.rotation
            go.transform.rotation = value
            pushChange("Rotate", { go.transform.rotation = previous }, { go.transform.rotation = value })
        }
        numberRow("Scale X", go.transform.scale.x, step = 0.1f) { value ->
            val previous = go.transform.scale.x
            go.transform.scale.x = value
            pushChange("Scale X", { go.transform.scale.x = previous }, { go.transform.scale.x = value })
        }
        numberRow("Scale Y", go.transform.scale.y, step = 0.1f) { value ->
            val previous = go.transform.scale.y
            go.transform.scale.y = value
            pushChange("Scale Y", { go.transform.scale.y = previous }, { go.transform.scale.y = value })
        }
        numberRow("Depth", go.transform.zOrder.toFloat(), step = 1f) { value ->
            go.transform.zOrder = value.toInt()
            state.notifyChanged()
        }

        // ---- components
        for (component in go.components) componentSection(component)

        content.addView(EditorTheme.label(context, "  ${go.components.size} component(s)", 10.5f, EditorTheme.textFaint))
    }

    private fun pushChange(label: String, onUndo: () -> Unit, onRedo: () -> Unit) {
        state.isDirty = true
        state.undoStack.push(label, onUndo, onRedo)
        state.notifyChanged()
    }

    private fun section(title: String) {
        val tv = EditorTheme.label(context, "  $title", 11.5f, EditorTheme.accentAlt, true)
        tv.setBackgroundColor(EditorTheme.panelAlt)
        content.addView(tv)
    }

    private fun componentSection(component: Component) {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(EditorTheme.panelLight)
            setPadding(EditorTheme.dp(context, 6), EditorTheme.dp(context, 5), EditorTheme.dp(context, 4), EditorTheme.dp(context, 5))
        }
        header.addView(EditorTheme.label(context, component::class.java.simpleName, 11.5f, EditorTheme.text, true), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val enabledBox = CheckBox(context).apply {
            isChecked = component.enabled
            setOnCheckedChangeListener { _, checked -> component.enabled = checked; state.notifyChanged() }
        }
        header.addView(enabledBox)
        val more = EditorTheme.label(context, " \u22EE ", 14f, EditorTheme.textDim)
        more.setOnClickListener { anchor ->
            val menu = PopupMenu(context, anchor)
            menu.menu.add("Remove component").setOnMenuItemClickListener {
                state.selection?.removeComponent(component)
                state.isDirty = true
                state.notifyChanged()
                true
            }
            menu.menu.add("Reset values").setOnMenuItemClickListener {
                resetComponent(component)
                true
            }
            menu.show()
        }
        header.addView(more)
        content.addView(header)

        when (component) {
            is SpriteRenderer -> {
                numberRow("Size X", component.size.x, step = 1f) { component.size.x = it; state.notifyChanged() }
                numberRow("Size Y", component.size.y, step = 1f) { component.size.y = it; state.notifyChanged() }
                numberRow("Sort layer", component.sortingLayer.toFloat(), step = 1f) { component.sortingLayer = it.toInt(); state.notifyChanged() }
                colorRow("Tint", component.color) { component.color = it; state.notifyChanged() }
                textRow("Sprite path", component.texturePath) { path -> component.texturePath = path; state.notifyChanged() }
            }
            is RigidBody2D -> {
                choiceRow("Body type", listOf("STATIC", "KINEMATIC", "DYNAMIC"), component.bodyType.name) { choice ->
                    component.bodyType = BodyType.valueOf(choice)
                    state.notifyChanged()
                }
                numberRow("Mass", component.mass, step = 0.5f) { component.mass = it.coerceAtLeast(0.01f); state.notifyChanged() }
                numberRow("Gravity scale", component.gravityScale, step = 0.5f) { component.gravityScale = it; state.notifyChanged() }
                numberRow("Linear drag", component.linearDrag, step = 0.1f) { component.linearDrag = it.coerceAtLeast(0f); state.notifyChanged() }
                boolRow("Use gravity", component.useGravity) { component.useGravity = it; state.notifyChanged() }
            }
            is BoxCollider2D -> {
                numberRow("Size X", component.size.x, step = 1f) { component.size.x = it; state.notifyChanged() }
                numberRow("Size Y", component.size.y, step = 1f) { component.size.y = it; state.notifyChanged() }
                boolRow("Is trigger", component.isTrigger) { component.isTrigger = it; state.notifyChanged() }
                numberRow("Friction", component.friction, step = 0.1f) { component.friction = it; state.notifyChanged() }
            }
            is CircleCollider2D -> {
                numberRow("Radius", component.radius, step = 0.5f) { component.radius = it.coerceAtLeast(0.01f); state.notifyChanged() }
                boolRow("Is trigger", component.isTrigger) { component.isTrigger = it; state.notifyChanged() }
            }
            is Light2D -> {
                numberRow("Radius", component.radius, step = 10f) { component.radius = it; state.notifyChanged() }
                numberRow("Intensity", component.intensity, step = 0.1f) { component.intensity = it; state.notifyChanged() }
                boolRow("Flicker", component.flicker) { component.flicker = it; state.notifyChanged() }
                colorRow("Color", component.color) { component.color = it; state.notifyChanged() }
            }
            is CameraComponent -> {
                numberRow("Zoom", component.zoom, step = 0.1f) { component.zoom = it.coerceAtLeast(0.05f); state.notifyChanged() }
                boolRow("Main camera", component.isMain) { component.isMain = it; state.notifyChanged() }
                textRow("Follow target", component.followTargetName) { component.followTargetName = it; state.notifyChanged() }
            }
            is ParticleSystem -> {
                numberRow("Burst count", component.maxParticles.toFloat(), step = 25f) { component.maxParticles = it.toInt(); state.notifyChanged() }
                numberRow("Emission/sec", component.emissionRate, step = 5f) { component.emissionRate = it; state.notifyChanged() }
                numberRow("Lifetime", component.lifetime, step = 0.25f) { component.lifetime = it.coerceAtLeast(0.05f); state.notifyChanged() }
                numberRow("Speed", component.speed, step = 10f) { component.speed = it; state.notifyChanged() }
                boolRow("Playing", component.isPlaying) { component.isPlaying = it; state.notifyChanged() }
                val burst = EditorTheme.button(context, "Emit burst") { component.emit(component.maxParticles / 4) }
                content.addView(burst)
            }
            is Animator -> {
                numberRow("Speed", component.speed, step = 0.1f) { component.speed = it; state.notifyChanged() }
            }
            is TilemapComponent -> {
                val map = component.tilemap
                if (map == null) {
                    val create = EditorTheme.button(context, "Create ${16}x16 tilemap") {
                        val newMap = com.sura.engine.tilemap.Tilemap(16, 16, component.tileSize)
                        val ground = newMap.addLayer("Ground")
                        ground.solid = true
                        ground.color = SuraColor(0.30f, 0.42f, 0.32f, 1f)
                        component.tilemap = newMap
                        state.isDirty = true
                        state.notifyChanged()
                    }
                    content.addView(create)
                } else {
                    val info = EditorTheme.label(context, "  ${map.width} x ${map.height} tiles (${map.layers.size} layers)", 11f, EditorTheme.textDim)
                    content.addView(info)
                    numberRow("Tile size", component.tileSize, step = 4f) { component.tileSize = it.coerceAtLeast(4f); state.notifyChanged() }
                    for ((index, layer) in map.layers.withIndex()) {
                        val row = EditorTheme.row(context, 28)
                        row.addView(EditorTheme.label(context, "  Layer ${layer.name}", 11f), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                        val solidBox = CheckBox(context).apply {
                            isChecked = layer.solid
                            setOnCheckedChangeListener { _, checked -> layer.solid = checked; state.notifyChanged() }
                        }
                        row.addView(solidBox)
                        content.addView(row)
                        colorRow("Layer ${index} color", layer.color) { layer.color = it; state.notifyChanged() }
                    }
                    val clear = EditorTheme.button(context, "Clear tiles") {
                        map.layers.forEach { it.clear() }
                        state.notifyChanged()
                    }
                    content.addView(clear)
                }
            }
            is ScriptBehaviour -> {
                textRow("Script name", component.scriptName) { component.scriptName = it; state.notifyChanged() }
                val button = EditorTheme.button(context, "Edit script (${component.source.length} chars)") {
                    ScriptEditorDialog.show(context, component, state)
                }
                content.addView(button)
                val error = component.lastError
                if (error != null) {
                    content.addView(EditorTheme.label(context, "  Error: $error", 10.5f, EditorTheme.error))
                }
            }
            else -> content.addView(EditorTheme.label(context, "  (no editable properties)", 10.5f, EditorTheme.textFaint))
        }
        content.addView(EditorTheme.divider(context))
    }

    private fun resetComponent(component: Component) {
        val go = state.selection ?: return
        val json = SceneSerializer().serializeComponent(component)
        if (json == null) return
        val fresh = SceneSerializer().deserializeComponent(json) ?: return
        go.removeComponent(component)
        go.addComponent(fresh)
        state.isDirty = true
        state.notifyChanged()
    }

    // ------------------------------------------------------------ field widgets

    private fun numberRow(label: String, value: Float, step: Float = 1f, onChanged: (Float) -> Unit) {
        val row = EditorTheme.row(context, 32)
        row.addView(EditorTheme.label(context, label, 11f, EditorTheme.textDim), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        val minus = EditorTheme.button(context, "\u2212") { onChanged(value - step) }
        val plus = EditorTheme.button(context, "+") { onChanged(value + step) }
        val field = EditText(context).apply {
            setText(formatFloat(value))
            setTextColor(EditorTheme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setBackgroundColor(EditorTheme.panelAlt)
            setSingleLine()
            gravity = Gravity.END
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            setPadding(EditorTheme.dp(context, 6), 0, EditorTheme.dp(context, 6), 0)
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val parsed = text.toString().toFloatOrNull()
                    if (parsed != null && parsed != value) onChanged(parsed) else setText(formatFloat(value))
                }
            }
        }
        val width = EditorTheme.dp(context, 74)
        row.addView(field, LayoutParams(width, LayoutParams.WRAP_CONTENT))
        row.addView(minus, LayoutParams(EditorTheme.dp(context, 34), LayoutParams.WRAP_CONTENT))
        row.addView(plus, LayoutParams(EditorTheme.dp(context, 34), LayoutParams.WRAP_CONTENT))
        content.addView(row)
    }

    private fun boolRow(label: String, value: Boolean, onChanged: (Boolean) -> Unit) {
        val row = EditorTheme.row(context, 30)
        row.addView(EditorTheme.label(context, label, 11f, EditorTheme.textDim), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val box = CheckBox(context).apply {
            isChecked = value
            setOnCheckedChangeListener { _, checked -> onChanged(checked) }
        }
        row.addView(box)
        content.addView(row)
    }

    private fun textRow(label: String, value: String, onChanged: (String) -> Unit) {
        val row = EditorTheme.row(context, 32)
        row.addView(EditorTheme.label(context, label, 11f, EditorTheme.textDim), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val field = EditText(context).apply {
            setText(value)
            setTextColor(EditorTheme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setBackgroundColor(EditorTheme.panelAlt)
            setSingleLine()
            setPadding(EditorTheme.dp(context, 6), 0, EditorTheme.dp(context, 6), 0)
            setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onChanged(text.toString()) }
        }
        row.addView(field, LayoutParams(EditorTheme.dp(context, 140), LayoutParams.WRAP_CONTENT))
        content.addView(row)
    }

    private fun choiceRow(label: String, options: List<String>, current: String, onChanged: (String) -> Unit) {
        val row = EditorTheme.row(context, 32)
        row.addView(EditorTheme.label(context, label, 11f, EditorTheme.textDim), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val button = EditorTheme.button(context, current) {
            Dialogs.choose(context, label, options) { index -> onChanged(options[index]) }
        }
        row.addView(button, LayoutParams(EditorTheme.dp(context, 140), LayoutParams.WRAP_CONTENT))
        content.addView(row)
    }

    private fun colorRow(label: String, value: SuraColor, onChanged: (SuraColor) -> Unit) {
        val row = EditorTheme.row(context, 32)
        row.addView(EditorTheme.label(context, label, 11f, EditorTheme.textDim), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val swatch = View(context).apply {
            setBackgroundColor(value.toInt())
            setOnClickListener {
                val serializer = SceneSerializer()
                Dialogs.prompt(context, "Color hex (RRGGBBAA)", serializer.colorToHex(value)) { hex ->
                    onChanged(serializer.hexToColor(hex, value))
                }
            }
        }
        val palette = EditorTheme.button(context, "\u25CF") {
            Dialogs.choose(context, "Palette", PALETTE_NAMES) { index -> onChanged(PALETTE_COLORS[index]) }
        }
        row.addView(swatch, LayoutParams(EditorTheme.dp(context, 34), EditorTheme.dp(context, 20)))
        row.addView(palette, LayoutParams(EditorTheme.dp(context, 40), LayoutParams.WRAP_CONTENT))
        content.addView(row)
    }

    private fun showAddComponentMenu() {
        val go = state.selection ?: return
        val options = listOf(
            "Sprite Renderer", "Rigid Body 2D", "Box Collider 2D", "Circle Collider 2D",
            "Light 2D", "Camera", "Particle System", "Animator", "Tilemap", "Script"
        )
        Dialogs.choose(context, "Add component to ${go.name}", options) { index ->
            val component: Component? = when (index) {
                0 -> SpriteRenderer()
                1 -> RigidBody2D()
                2 -> BoxCollider2D()
                3 -> CircleCollider2D()
                4 -> Light2D()
                5 -> CameraComponent()
                6 -> ParticleSystem()
                7 -> Animator()
                8 -> TilemapComponent().apply {
                    val map = com.sura.engine.tilemap.Tilemap(16, 16, 32f)
                    val ground = map.addLayer("Ground")
                    ground.solid = true
                    ground.color = SuraColor(0.30f, 0.42f, 0.32f, 1f)
                    tilemap = map
                }
                else -> ScriptBehaviour().apply {
                    scriptName = "NewScript"
                    source = DEFAULT_SCRIPT
                }
            }
            if (component != null) {
                go.addComponent(component)
                state.isDirty = true
                state.log("Added ${component::class.java.simpleName} to ${go.name}")
                state.notifyChanged()
            }
        }
    }

    private fun formatFloat(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString() else String.format("%.3f", value)

    companion object {
        val DEFAULT_SCRIPT = """
            // SuraScript - runs on the device, no internet required
            var speed = 100

            function start() {
                print("hello from " + "SuraScript")
            }

            function update(dt) {
                x = x + speed * dt
                if (x > 400) {
                    speed = 0 - speed
                }
                if (x < 0) {
                    speed = 0 - speed
                }
            }
        """.trimIndent()

        private val PALETTE_NAMES = listOf(
            "White", "Black", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "Orange", "Transparent"
        )
        private val PALETTE_COLORS = listOf(
            SuraColor(1f, 1f, 1f, 1f), SuraColor(0f, 0f, 0f, 1f), SuraColor(0.95f, 0.35f, 0.35f, 1f),
            SuraColor(0.45f, 0.85f, 0.45f, 1f), SuraColor(0.4f, 0.6f, 0.98f, 1f), SuraColor(0.98f, 0.9f, 0.45f, 1f),
            SuraColor(0.4f, 0.85f, 0.9f, 1f), SuraColor(0.85f, 0.5f, 0.9f, 1f), SuraColor(0.95f, 0.65f, 0.3f, 1f),
            SuraColor(0f, 0f, 0f, 0f)
        )
    }
}
