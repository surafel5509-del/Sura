package com.sura.engine.editor

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sura.engine.core.Engine
import com.sura.engine.games.DemoScenes
import com.sura.engine.project.Project
import com.sura.engine.project.ProjectAsset
import com.sura.engine.project.ProjectManager
import com.sura.engine.serialization.SceneSerializer

/** Project browser: assets, scenes, scripts and prefabs with quick create actions. */
class AssetBrowserView(context: Context, private val state: EditorState) : LinearLayout(context) {

    private val breadcrumb = HorizontalScrollView(context)
    private val listContainer = LinearLayout(context).apply { orientation = VERTICAL }
    private var currentFolder = ""

    init {
        orientation = VERTICAL
        setBackgroundColor(EditorTheme.panel)
        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(EditorTheme.label(context, "  ASSETS", 12f, EditorTheme.accent, true), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        headerRow.addView(EditorTheme.button(context, "+ Script") { createScript() })
        addView(headerRow)
        breadcrumb.addView(LinearLayout(context).apply { orientation = HORIZONTAL })
        addView(breadcrumb, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(EditorTheme.divider(context))

        val actions = LinearLayout(context).apply { orientation = HORIZONTAL }
        actions.addView(EditorTheme.button(context, "New scene") { createScene() })
        actions.addView(EditorTheme.button(context, "Demos") { showDemos() })
        addView(actions)

        val scroll = ScrollView(context)
        scroll.addView(listContainer)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        state.subscribe { post { refresh() } }
        refresh()
    }

    fun refresh() {
        listContainer.removeAllViews()
        val project = state.project
        if (project == null) {
            listContainer.addView(EditorTheme.label(context, "No project open.\n\nOpen File > New project to start building a game.", 11.5f, EditorTheme.textFaint))
            renderBreadcrumb(null, "")
            return
        }
        renderBreadcrumb(project, currentFolder)
        val assets = project.listAssets(currentFolder)
        if (assets.isEmpty()) listContainer.addView(EditorTheme.label(context, "  (empty)", 11f, EditorTheme.textFaint))
        for (asset in assets) listContainer.addView(makeRow(asset))
    }

    private fun renderBreadcrumb(project: Project?, folder: String) {
        val row = (breadcrumb.getChildAt(0) as LinearLayout)
        row.removeAllViews()
        if (project == null) return
        val root = EditorTheme.label(context, "  ${project.name}  ", 11f, EditorTheme.accentAlt, true)
        root.setOnClickListener { currentFolder = ""; refresh() }
        row.addView(root)
        var path = ""
        for (part in folder.split("/").filter { it.isNotEmpty() }) {
            path = if (path.isEmpty()) part else "$path/$part"
            val segment = path
            val label = EditorTheme.label(context, "/ $part ", 11f, EditorTheme.textDim)
            label.setOnClickListener { currentFolder = segment; refresh() }
            row.addView(label)
        }
    }

    private fun makeRow(asset: ProjectAsset): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(EditorTheme.dp(context, 8), EditorTheme.dp(context, 6), EditorTheme.dp(context, 8), EditorTheme.dp(context, 6))
        }
        val iconColor = when (asset.type) {
            ProjectAsset.AssetType.SCENE -> EditorTheme.accent
            ProjectAsset.AssetType.SCRIPT -> EditorTheme.success
            ProjectAsset.AssetType.PREFAB -> EditorTheme.warning
            ProjectAsset.AssetType.TEXTURE -> EditorTheme.accentAlt
            ProjectAsset.AssetType.AUDIO -> Color.parseColor("#CBA6F7")
            ProjectAsset.AssetType.FOLDER -> EditorTheme.textDim
            else -> EditorTheme.textFaint
        }
        val icon = EditorTheme.label(context, if (asset.type == ProjectAsset.AssetType.FOLDER) "\u25B8 " else "\u25A0 ", 11f, iconColor)
        icon.setPadding(0, 0, 0, 0)
        row.addView(icon)
        row.addView(EditorTheme.label(context, asset.name, 11.5f), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val size = EditorTheme.label(context, asset.type.name.lowercase(), 9.5f, EditorTheme.textFaint)
        row.addView(size)
        row.setOnClickListener { openAsset(asset) }
        row.setOnLongClickListener { showAssetMenu(asset); true }
        return row
    }

    private fun openAsset(asset: ProjectAsset) {
        when (asset.type) {
            ProjectAsset.AssetType.FOLDER -> { currentFolder = asset.path; refresh() }
            ProjectAsset.AssetType.SCENE -> loadScene(asset)
            ProjectAsset.AssetType.SCRIPT -> editScript(asset)
            ProjectAsset.AssetType.PREFAB -> instantiatePrefab(asset)
            else -> state.log("${asset.name} is a ${asset.type.name.lowercase()} asset")
        }
    }

    private fun loadScene(asset: ProjectAsset) {
        val project = state.project ?: return
        val name = asset.name.removeSuffix(SceneSerializer.SCENE_EXTENSION)
        val scene = ProjectManager(com.sura.engine.platform.Platform(Engine.get().context)).loadScene(project, name)
        if (scene == null) { state.log("Could not load scene $name", EditorState.ConsoleLine.Level.ERROR); return }
        state.scene = scene
        state.select(null)
        state.log("Scene '$name' loaded (${scene.allObjects.size} objects)")
    }

    private fun editScript(asset: ProjectAsset) {
        val project = state.project ?: return
        val source = project.readText(asset.path) ?: return
        val go = state.createObject(asset.name.removeSuffix(".sura"))
        val behaviour = com.sura.engine.scripting.ScriptBehaviour().apply {
            scriptName = asset.name.removeSuffix(".sura")
            this.source = source
        }
        go?.addComponent(behaviour)
        state.log("Attached script ${behaviour.scriptName}")
    }

    private fun instantiatePrefab(asset: ProjectAsset) {
        val project = state.project ?: return
        val json = project.readText(asset.path) ?: return
        val scene = state.scene ?: return
        try {
            val parsed = com.sura.engine.serialization.Json.parseObject(json)
            val go = SceneSerializer().deserializeObject(parsed, scene)
            go.transform.position.set(0f, 0f)
            scene.adopt(go)
            state.select(go)
            state.log("Prefab ${asset.name} instantiated")
        } catch (t: Throwable) {
            state.log("Bad prefab: ${t.message}", EditorState.ConsoleLine.Level.ERROR)
        }
    }

    private fun showAssetMenu(asset: ProjectAsset) {
        val options = mutableListOf("Delete")
        if (asset.type == ProjectAsset.AssetType.SCENE) options.add(0, "Open scene")
        if (asset.type == ProjectAsset.AssetType.PREFAB) options.add(0, "Instantiate")
        Dialogs.choose(context, asset.name, options) { index ->
            val choice = options[index]
            when (choice) {
                "Delete" -> {
                    state.project?.delete(asset.path)
                    state.log("Deleted ${asset.name}")
                    refresh()
                }
                "Open scene" -> loadScene(asset)
                "Instantiate" -> instantiatePrefab(asset)
            }
        }
    }

    private fun createScript() {
        val project = state.project ?: return run { state.log("Open a project first", EditorState.ConsoleLine.Level.WARN) }
        Dialogs.prompt(context, "New script name", "PlayerController") { name ->
            if (name.isBlank()) return@prompt
            val ok = ProjectManager(com.sura.engine.platform.Platform(Engine.get().context)).saveScript(project, name, InspectorView.DEFAULT_SCRIPT)
            state.log(if (ok) "Script '$name.sura' created" else "Could not create script", if (ok) EditorState.ConsoleLine.Level.INFO else EditorState.ConsoleLine.Level.ERROR)
            refresh()
        }
    }

    private fun createScene() {
        val project = state.project ?: return run { state.log("Open a project first", EditorState.ConsoleLine.Level.WARN) }
        Dialogs.prompt(context, "New scene name", "Level1") { name ->
            if (name.isBlank()) return@prompt
            val scene = com.sura.engine.scene.Scene(name)
            val go = scene.createGameObject("Main Camera")
            go.addComponent(com.sura.engine.rendering.CameraComponent())
            ProjectManager(com.sura.engine.platform.Platform(Engine.get().context)).saveScene(project, scene)
            state.scene = scene
            state.log("Scene '$name' created and saved")
            refresh()
        }
    }

    private fun showDemos() {
        val options = DemoScenes.names()
        Dialogs.choose(context, "Load demo scene", options) { index ->
            val scene = DemoScenes.build(options[index])
            state.scene = scene
            state.select(null)
            state.log("Demo '${scene.name}' loaded with ${scene.allObjects.size} objects")
        }
    }
}
