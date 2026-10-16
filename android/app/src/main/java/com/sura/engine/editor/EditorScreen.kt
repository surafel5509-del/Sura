package com.sura.engine.editor

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.sura.engine.assets.AssetManager
import com.sura.engine.audio.AudioManager
import com.sura.engine.build.BuildManager
import com.sura.engine.core.Engine
import com.sura.engine.core.JobSystem
import com.sura.engine.core.Scheduler
import com.sura.engine.games.DemoScenes
import com.sura.engine.input.InputManager
import com.sura.engine.physics.PhysicsWorld
import com.sura.engine.platform.Platform
import com.sura.engine.project.Project
import com.sura.engine.project.ProjectManager
import com.sura.engine.scene.Scene
import com.sura.engine.scene.SceneManager
import com.sura.engine.scripting.ScriptEngine
import com.sura.engine.serialization.SceneSerializer

/**
 * The complete Sura Engine IDE screen: menu bar, tool palette, hierarchy, scene view,
 * inspector, asset browser, console and profiler - laid out for landscape tablets and phones.
 */
class EditorScreen(context: Context) : LinearLayout(context) {

    val state = EditorState()
    private val engineView = EngineView(context)
    private val hierarchy = HierarchyView(context, state)
    private val inspector = InspectorView(context, state)
    private val assets = AssetBrowserView(context, state)
    private val console = ConsoleView(context, state)
    private val stats = StatsView(context, state)
    private val leftContainer = FrameLayout(context)
    private val bottomContainer = FrameLayout(context)
    private val tabStrip = LinearLayout(context)
    private val bottomTabs = LinearLayout(context)
    private var leftTab = 0
    private var bottomTab = 0
    private var playSnapshot: String? = null

    private val projectManager = ProjectManager(Platform(Engine.get().context))
    private val sceneManager = SceneManager()
    private val physics = PhysicsWorld()
    private val scripts = ScriptEngine()

    private val callbacks = object : EditorMenuBar.Callbacks {
        override fun onPlay() = enterPlayMode()
        override fun onPause() { state.isPaused = !state.isPaused; state.log(if (state.isPaused) "Paused" else "Resumed"); state.notifyChanged() }
        override fun onStop() = exitPlayMode()
        override fun onStep() { engineView.stepFrame(); state.log("Stepped one frame") }
        override fun onSaveScene() = saveScene()
        override fun onSaveProject() { projectManager.saveProject(state.project ?: return); state.log("Project saved") }
        override fun onBuild() = showBuildDialog()
        override fun onToggleGrid() { state.showGrid = !state.showGrid; engineView.showGrid = state.showGrid; state.notifyChanged() }
        override fun onToggleStats() { state.showStats = !state.showStats; engineView.showStats = state.showStats; state.notifyChanged() }
        override fun onToggleColliders() { state.showColliders = !state.showColliders; engineView.showColliders = state.showColliders; state.notifyChanged() }
        override fun onFocusSelection() = engineView.focusSelection()
        override fun onOpenProject(name: String) = loadProject(name)
        override fun onNewProject(name: String) = loadProject(name)
        override fun onExit() = Dialogs.alert(context, "Sura Engine", "Press the back key or the recent apps button to leave the editor.\nYour project is saved automatically.")
        override fun onSavePrefab() = saveSelectionAsPrefab()
        override fun onScenes() = showSceneManager()
        override fun onProjectSettings() = showProjectSettings()
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(EditorTheme.background)
        setupEngine()
        buildLayout()
        state.subscribe { post { refreshPanels() } }
        bootstrapProject()
    }

    // ------------------------------------------------------------------ engine

    private fun setupEngine() {
        val engine = Engine.get()
        engine.services.register(SceneManager::class.java, sceneManager)
        engine.services.register(InputManager::class.java, InputManager())
        engine.services.register(PhysicsWorld::class.java, physics)
        engine.services.register(AudioManager::class.java, AudioManager(context))
        engine.services.register(AssetManager::class.java, AssetManager(context))
        engine.services.register(JobSystem::class.java, JobSystem())
        engine.services.register(Scheduler::class.java, Scheduler())
        engine.start()
        engineView.inputManager = engine.services.get(InputManager::class.java)
        engineView.editorState = state
        engineView.sceneManager = sceneManager
        engineView.showGrid = state.showGrid
        engineView.gridSize = state.snapSize
        EngineView.physicsWorldRef = physics
        console.log("Sura Engine 1.0.0 ready - offline mode")
    }

    private fun bootstrapProject() {
        val last = projectManager.lastProjectName()
        val projects = projectManager.listProjects()
        val project: Project = when {
            last.isNotEmpty() && projects.contains(last) -> projectManager.openProject(last) ?: projectManager.createProject("MyGame")
            projects.isNotEmpty() -> projectManager.openProject(projects.first()) ?: projectManager.createProject("MyGame")
            else -> projectManager.createProject("MyGame")
        }
        state.project = project
        val sceneNames = projectManager.listScenes(project)
        val scene: Scene = if (sceneNames.isEmpty()) {
            val created = DemoScenes.build("platformer")
            created.name = "Level1"
            projectManager.saveScene(project, created)
            created
        } else {
            projectManager.loadScene(project, sceneNames.first()) ?: DemoScenes.build("platformer")
        }
        state.scene = scene
        engineView.scene = scene
        sceneManager.loadScene(scene)
        state.log("Project '${project.name}' ready - scene '${scene.name}' (${scene.allObjects.size} objects)")
        console.refresh()
        state.notifyChanged()
    }

    private fun loadProject(name: String) {
        val project = projectManager.openProject(name) ?: projectManager.createProject(name)
        state.project = project
        val scenes = projectManager.listScenes(project)
        val scene = if (scenes.isEmpty()) {
            val created = DemoScenes.build("empty")
            projectManager.saveScene(project, created)
            created
        } else projectManager.loadScene(project, scenes.first()) ?: DemoScenes.build("empty")
        state.scene = scene
        engineView.scene = scene
        sceneManager.loadScene(scene)
        sceneManager.scenes[scene.name] = scene
        state.select(null)
        state.log("Loaded project '${project.name}' - scene '${scene.name}'")
        state.notifyChanged()
    }

    private fun saveScene() {
        val project = state.project ?: return run { state.log("No project open", EditorState.ConsoleLine.Level.WARN) }
        val scene = state.scene ?: return
        val ok = projectManager.saveScene(project, scene)
        state.isDirty = !ok
        state.log(if (ok) "Scene '${scene.name}' saved (${scene.allObjects.size} objects)" else "Save failed", if (ok) EditorState.ConsoleLine.Level.INFO else EditorState.ConsoleLine.Level.ERROR)
    }

    private fun enterPlayMode() {
        val scene = state.scene ?: return
        playSnapshot = try { SceneSerializer().serialize(scene) } catch (t: Throwable) { null }
        state.isPlaying = true
        state.isPaused = false
        engineView.isEditorMode = false
        sceneManager.loadScene(scene)
        state.log("Play mode - ${scene.allObjects.size} objects running")
        state.notifyChanged()
    }

    private fun exitPlayMode() {
        state.isPlaying = false
        state.isPaused = false
        engineView.isEditorMode = true
        val snapshot = playSnapshot
        if (snapshot != null) {
            try {
                val restored = SceneSerializer().deserialize(snapshot, state.scene?.name ?: "Scene")
                state.scene = restored
                engineView.scene = restored
                sceneManager.loadScene(restored)
                state.log("Stopped - scene state restored")
            } catch (t: Throwable) {
                state.log("Could not restore scene: ${t.message}", EditorState.ConsoleLine.Level.WARN)
            }
        }
        playSnapshot = null
        state.notifyChanged()
    }

    private fun showBuildDialog() {
        val project = state.project ?: return
        val settings = project.settings
        val manager = BuildManager()
        Dialogs.prompt(context, "Build - app name", settings.appName) { appName ->
            settings.appName = appName.ifBlank { project.name }
            Dialogs.prompt(context, "Build - package id", settings.packageName) { packageName ->
                settings.packageName = packageName.ifBlank { "com.sura.${project.name.lowercase()}" }
                Dialogs.choose(context, "Orientation", listOf("landscape", "portrait")) { index ->
                    settings.orientation = if (index == 0) "landscape" else "portrait"
                    saveScene()
                    val spec = JsonBuildSpec.build(settings, project)
                    project.writeText("Build/build-spec.json", spec)
                    manager.buildDebug { ok, message ->
                        post {
                            state.log(if (ok) "$message -> Build/build-spec.json written" else "Build failed: $message",
                                if (ok) EditorState.ConsoleLine.Level.INFO else EditorState.ConsoleLine.Level.ERROR)
                            Dialogs.alert(context, "Build", buildSummary(settings, ok, message))
                        }
                    }
                }
            }
        }
    }

    private fun buildSummary(settings: com.sura.engine.project.ProjectSettings, ok: Boolean, message: String): String =
        """
        Target     : Android
        App        : ${settings.appName}
        Package    : ${settings.packageName}
        Version    : ${settings.versionName} (${settings.versionCode})
        Orientation: ${settings.orientation}
        Min SDK    : ${settings.minSdk}   Target SDK: ${settings.targetSdk}
        Result     : ${if (ok) "OK" else "FAILED"} - $message

        The generated spec lives in Build/build-spec.json and can be compiled with Gradle
        (gradle :app:assembleRelease) using the bundled android/ project.
        """.trimIndent()

    // ------------------------------------------------------------------ layout

    private fun buildLayout() {
        addView(EditorMenuBar(context, state, callbacks))
        addView(ToolPaletteView(context, state, callbacks) {
            state.notifyChanged()
        })
        addView(EditorTheme.divider(context))

        val mainRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        addView(mainRow, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        // left panel
        val left = LinearLayout(context).apply { orientation = VERTICAL }
        left.layoutParams = LayoutParams(EditorTheme.dp(context, EditorTheme.LEFT_PANEL_WIDTH_DP), LayoutParams.MATCH_PARENT)
        val leftTabs = LinearLayout(context).apply { orientation = HORIZONTAL }
        leftTabs.addView(tabButton("Hierarchy") { leftTab = 0; refreshPanels() }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        leftTabs.addView(tabButton("Assets") { leftTab = 1; refreshPanels() }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        left.addView(leftTabs)
        leftContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        left.addView(leftContainer)
        left.addView(EditorTheme.button(context, "+ Create object") { state.createObject("GameObject") })
        mainRow.addView(left)

        // centre: scene view + console
        val centre = LinearLayout(context).apply { orientation = VERTICAL }
        centre.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        val centreTabs = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(EditorTheme.panelLight)
            gravity = Gravity.CENTER_VERTICAL
        }
        centreTabs.addView(EditorTheme.label(context, "  Scene", 11.5f, EditorTheme.textDim), LayoutParams(EditorTheme.dp(context, 70), LayoutParams.WRAP_CONTENT))
        centreTabs.addView(EditorTheme.label(context, "| Game", 11.5f, EditorTheme.textDim), LayoutParams(EditorTheme.dp(context, 70), LayoutParams.WRAP_CONTENT))
        centreTabs.addView(EditorTheme.label(context, "   pinch = zoom  -  two fingers = pan  -  drag = move  ", 10f, EditorTheme.textFaint), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        centre.addView(centreTabs)
        centre.addView(engineView, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        val bottomBar = LinearLayout(context).apply { orientation = HORIZONTAL; setBackgroundColor(EditorTheme.panelAlt) }
        bottomBar.addView(tabButton("Console") { bottomTab = 0; refreshPanels() }, LayoutParams(EditorTheme.dp(context, 90), LayoutParams.WRAP_CONTENT))
        bottomBar.addView(tabButton("Profiler") { bottomTab = 1; refreshPanels() }, LayoutParams(EditorTheme.dp(context, 90), LayoutParams.WRAP_CONTENT))
        bottomTabs.addView(bottomBar, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        centre.addView(bottomTabs)
        bottomContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, EditorTheme.dp(context, EditorTheme.CONSOLE_HEIGHT_DP))
        centre.addView(bottomContainer)
        mainRow.addView(centre)

        // right: inspector
        val right = LinearLayout(context).apply { orientation = VERTICAL }
        right.layoutParams = LayoutParams(EditorTheme.dp(context, EditorTheme.RIGHT_PANEL_WIDTH_DP), LayoutParams.MATCH_PARENT)
        right.addView(inspector, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        val actions = LinearLayout(context).apply { orientation = HORIZONTAL; setBackgroundColor(EditorTheme.panelAlt) }
        actions.addView(EditorTheme.button(context, "Duplicate") {
            val go = state.selection ?: return@button
            val scene = state.scene ?: return@button
            val serializer = SceneSerializer()
            val copy = serializer.deserializeObject(serializer.serializeObject(go), scene)
            copy.name = state.uniqueName(go.name)
            scene.adopt(copy)
            state.select(copy)
        })
        actions.addView(EditorTheme.button(context, "Delete") { state.deleteSelected() })
        actions.addView(EditorTheme.button(context, "Undo") { state.undoStack.undo(); state.notifyChanged() })
        actions.addView(EditorTheme.button(context, "Redo") { state.undoStack.redo(); state.notifyChanged() })
        right.addView(actions)
        mainRow.addView(right)

        refreshPanels()
    }

    private fun tabButton(title: String, onTap: () -> Unit): TextView {
        val view = EditorTheme.label(context, title, 11.5f, EditorTheme.text)
        view.gravity = Gravity.CENTER
        view.setBackgroundColor(EditorTheme.panelLight)
        view.setPadding(EditorTheme.dp(context, 8), EditorTheme.dp(context, 7), EditorTheme.dp(context, 8), EditorTheme.dp(context, 7))
        view.setOnClickListener { onTap() }
        return view
    }

    private fun refreshPanels() {
        leftContainer.removeAllViews()
        leftContainer.addView(if (leftTab == 0) hierarchy else assets)
        bottomContainer.removeAllViews()
        if (bottomTab == 0) console.refresh()
        bottomContainer.addView(if (bottomTab == 0) console else stats)
        engineView.showGrid = state.showGrid
        state.profiler.drawCalls = state.scene?.allObjects?.size ?: 0
    }

    private var autoSaveAccumulator = 0f

    /** Called by the activity on a timer to refresh live panels and trigger autosave. */
    fun onTick(deltaSeconds: Float = 0.25f) {
        if (bottomTab == 1) stats.update()
        val project = state.project ?: return
        val interval = project.settings.autosaveSeconds
        if (interval <= 0) return
        autoSaveAccumulator += deltaSeconds
        if (autoSaveAccumulator >= interval) {
            autoSaveAccumulator = 0f
            if (state.isDirty && !state.isPlaying) {
                val scene = state.scene
                if (scene != null) {
                    val json = SceneSerializer().serialize(scene)
                    project.writeText("Scenes/Autosave.scene", json)
                    state.log("Autosaved '${scene.name}' -> Scenes/Autosave.scene")
                    state.isDirty = false
                }
            }
        }
    }

    /** Stores the current selection as a reusable prefab in the project folder. */
    private fun saveSelectionAsPrefab() {
        val go = state.selection ?: return run { state.log("Select an object first", EditorState.ConsoleLine.Level.WARN) }
        val project = state.project ?: return run { state.log("No project open", EditorState.ConsoleLine.Level.WARN) }
        Dialogs.prompt(context, "Prefab name", go.name) { name ->
            if (name.isBlank()) return@prompt
            val json = com.sura.engine.serialization.Json.stringify(
                SceneSerializer().serializeObject(go), true
            )
            val ok = projectManager.savePrefab(project, name, json)
            state.log(if (ok) "Prefab '$name' saved (${go.components.size} components)" else "Could not save prefab",
                if (ok) EditorState.ConsoleLine.Level.INFO else EditorState.ConsoleLine.Level.ERROR)
        }
    }

    /** Scene browser: open, duplicate, create and delete project scenes. */
    private fun showSceneManager() {
        val project = state.project ?: return run { state.log("No project open", EditorState.ConsoleLine.Level.WARN) }
        val scenes = projectManager.listScenes(project).toMutableList()
        val options = mutableListOf<String>()
        options.addAll(scenes.map { "Open: $it" })
        options.add("Create new scene")
        scenes.forEach { options.add("Duplicate: $it") }
        scenes.forEach { options.add("Delete: $it") }
        Dialogs.choose(context, "Scenes", options) { index ->
            val choice = options[index]
            when {
                choice.startsWith("Open: ") -> {
                    val name = choice.removePrefix("Open: ")
                    val scene = projectManager.loadScene(project, name)
                    if (scene == null) {
                        state.log("Could not open '$name'", EditorState.ConsoleLine.Level.ERROR)
                    } else {
                        state.scene = scene
                        engineView.scene = scene
                        sceneManager.loadScene(scene)
                        state.select(null)
                        state.log("Scene '$name' loaded (${scene.allObjects.size} objects)")
                    }
                }
                choice.startsWith("Duplicate: ") -> {
                    val source = choice.removePrefix("Duplicate: ")
                    val scene = projectManager.loadScene(project, source)
                    if (scene != null) {
                        scene.name = uniqueSceneName(project, source)
                        projectManager.saveScene(project, scene)
                        state.log("Duplicated '$source' -> '${scene.name}'")
                    }
                }
                choice.startsWith("Delete: ") -> {
                    val name = choice.removePrefix("Delete: ")
                    Dialogs.confirm(context, "Delete scene", "Delete '$name' permanently?") {
                        project.delete("Scenes/$name.scene")
                        state.log("Deleted scene '$name'")
                    }
                }
                else -> {
                    Dialogs.prompt(context, "New scene name", "Level") { name ->
                        if (name.isBlank()) return@prompt
                        val scene = Scene(name)
                        val camera = scene.createGameObject("Main Camera")
                        camera.addComponent(com.sura.engine.rendering.CameraComponent())
                        projectManager.saveScene(project, scene)
                        state.scene = scene
                        engineView.scene = scene
                        sceneManager.loadScene(scene)
                        state.log("Scene '$name' created")
                    }
                }
            }
        }
    }

    /** Edits the exported game's identity and runtime settings. */
    private fun showProjectSettings() {
        val project = state.project ?: return run { state.log("No project open", EditorState.ConsoleLine.Level.WARN) }
        val settings = project.settings
        Dialogs.prompt(context, "App name", settings.appName) { appName ->
            settings.appName = appName.ifBlank { settings.appName }
            Dialogs.prompt(context, "Package id", settings.packageName) { packageName ->
                settings.packageName = packageName.ifBlank { settings.packageName }
                Dialogs.prompt(context, "Version name", settings.versionName) { version ->
                    settings.versionName = version.ifBlank { settings.versionName }
                    Dialogs.prompt(context, "Gravity Y (e.g. -9.81)", settings.physicsGravityY.toString(), numeric = true) { gravity ->
                        gravity.toFloatOrNull()?.let { settings.physicsGravityY = it }
                        Dialogs.choose(context, "Renderer backend", listOf("canvas", "opengl")) { index ->
                            settings.rendererBackend = if (index == 0) "canvas" else "opengl"
                            projectManager.saveProject(project)
                            state.log("Project settings saved (${settings.appName}, ${settings.packageName}, ${settings.rendererBackend})")
                            state.notifyChanged()
                        }
                    }
                }
            }
        }
    }

    fun shutdown() {
        projectManager.saveProject(state.project ?: return)
        saveScene()
    }

    private fun uniqueSceneName(project: Project, base: String): String {
        val existing = projectManager.listScenes(project).toSet()
        if (!existing.contains(base)) return base
        var index = 1
        while (existing.contains("$base $index")) index++
        return "$base $index"
    }

    fun consoleView(): ConsoleView = console
    fun engineSurface(): EngineView = engineView
    
    private object JsonBuildSpec {
        fun build(settings: com.sura.engine.project.ProjectSettings, project: Project): String {
            val map = linkedMapOf<String, Any?>(
                "appName" to settings.appName,
                "packageName" to settings.packageName,
                "versionName" to settings.versionName,
                "versionCode" to settings.versionCode,
                "orientation" to settings.orientation,
                "minSdk" to settings.minSdk,
                "targetSdk" to settings.targetSdk,
                "scenes" to project.scenes(),
                "scripts" to project.scripts(),
                "prefabs" to project.prefabs(),
                "builtAt" to System.currentTimeMillis()
            )
            return com.sura.engine.serialization.Serializer.stringify(map as Map<String, Any?>, true)
        }
    }
}
