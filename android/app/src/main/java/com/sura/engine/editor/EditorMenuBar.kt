package com.sura.engine.editor

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import com.sura.engine.core.Engine
import com.sura.engine.games.DemoScenes
import com.sura.engine.platform.Platform
import com.sura.engine.project.ProjectManager

/** Top menu bar (File / Edit / Create / Component / Play / Build / Help) with real dropdown menus. */
class EditorMenuBar(context: Context, private val state: EditorState, private val callbacks: Callbacks) : LinearLayout(context) {

    interface Callbacks {
        fun onPlay()
        fun onPause()
        fun onStop()
        fun onStep()
        fun onSaveScene()
        fun onSaveProject()
        fun onBuild()
        fun onToggleGrid()
        fun onToggleStats()
        fun onToggleColliders()
        fun onFocusSelection()
        fun onOpenProject(name: String)
        fun onNewProject(name: String)
        fun onExit()
        fun onSavePrefab()
        fun onScenes()
        fun onProjectSettings()
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(EditorTheme.panelAlt)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, EditorTheme.dp(context, EditorTheme.MENU_HEIGHT_DP))
        setPadding(EditorTheme.dp(context, 6), 0, EditorTheme.dp(context, 6), 0)

        addView(brand())
        addView(menu("File") { menu ->
            menu.menu.add("New project").setOnMenuItemClickListener { onCreateProject(); true }
            menu.menu.add("Open project").setOnMenuItemClickListener { onOpenProject(); true }
            menu.menu.add("Save project").setOnMenuItemClickListener { callbacks.onSaveProject(); true }
            menu.menu.add("Save scene").setOnMenuItemClickListener { callbacks.onSaveScene(); true }
            menu.menu.add("Scenes...").setOnMenuItemClickListener { callbacks.onScenes(); true }
            menu.menu.add("Project settings").setOnMenuItemClickListener { callbacks.onProjectSettings(); true }
            menu.menu.add("Load demo").setOnMenuItemClickListener { onLoadDemo(); true }
            menu.menu.add("Exit editor").setOnMenuItemClickListener { callbacks.onExit(); true }
        })
        addView(menu("Edit") { menu ->
            menu.menu.add("Undo").setOnMenuItemClickListener {
                val ok = state.undoStack.undo()
                state.log(if (ok) "undo" else "nothing to undo", if (ok) EditorState.ConsoleLine.Level.INFO else EditorState.ConsoleLine.Level.WARN)
                true
            }
            menu.menu.add("Redo").setOnMenuItemClickListener { state.undoStack.redo(); state.notifyChanged(); true }
            menu.menu.add("Duplicate selection").setOnMenuItemClickListener { duplicate(); true }
            menu.menu.add("Delete selection").setOnMenuItemClickListener { state.deleteSelected(); true }
            menu.menu.add("Select none").setOnMenuItemClickListener { state.select(null); true }
        })
        addView(menu("Create") { menu ->
            menu.menu.add("Empty object").setOnMenuItemClickListener { state.createObject("GameObject"); true }
            menu.menu.add("Sprite").setOnMenuItemClickListener { createWith("Sprite") { go ->
                go.addComponent(com.sura.engine.rendering.SpriteRenderer())
            }; true }
            menu.menu.add("Camera").setOnMenuItemClickListener { createWith("Camera") { go ->
                go.addComponent(com.sura.engine.rendering.CameraComponent())
            }; true }
            menu.menu.add("Light 2D").setOnMenuItemClickListener { createWith("Light") { go ->
                go.addComponent(com.sura.engine.rendering.Light2D())
            }; true }
            menu.menu.add("Particle system").setOnMenuItemClickListener { createWith("Particles") { go ->
                go.addComponent(com.sura.engine.particles.ParticleSystem())
            }; true }
            menu.menu.add("Physics box").setOnMenuItemClickListener { createWith("PhysicsBox") { go ->
                go.addComponent(com.sura.engine.rendering.SpriteRenderer())
                go.addComponent(com.sura.engine.physics.BoxCollider2D().apply { size.set(48f, 48f) })
                go.addComponent(com.sura.engine.physics.RigidBody2D())
            }; true }
            menu.menu.add("Tilemap").setOnMenuItemClickListener { createWith("Tilemap") { go ->
                val map = com.sura.engine.tilemap.Tilemap(16, 16, 32f)
                val layer = map.addLayer("Ground")
                layer.solid = true
                layer.color = com.sura.engine.utils.Color(0.3f, 0.42f, 0.32f, 1f)
                go.addComponent(com.sura.engine.tilemap.TilemapComponent().apply { tilemap = map })
            }; true }
            menu.menu.add("Script object").setOnMenuItemClickListener { createWith("ScriptedObject") { go ->
                go.addComponent(com.sura.engine.scripting.ScriptBehaviour().apply {
                    scriptName = "NewScript"
                    source = InspectorView.DEFAULT_SCRIPT
                })
            }; true }
            menu.menu.add("UI Canvas").setOnMenuItemClickListener { createWith("UI Canvas") { go ->
                go.addComponent(com.sura.engine.ui.UIElementHolder())
            }; true }
            menu.menu.add("Save selection as prefab").setOnMenuItemClickListener { callbacks.onSavePrefab(); true }
        })
        addView(menu("Play") { menu ->
            menu.menu.add("Play").setOnMenuItemClickListener { callbacks.onPlay(); true }
            menu.menu.add("Pause / Resume").setOnMenuItemClickListener { callbacks.onPause(); true }
            menu.menu.add("Step frame").setOnMenuItemClickListener { callbacks.onStep(); true }
            menu.menu.add("Stop").setOnMenuItemClickListener { callbacks.onStop(); true }
        })
        addView(menu("View") { menu ->
            menu.menu.add("Toggle grid").setOnMenuItemClickListener { callbacks.onToggleGrid(); true }
            menu.menu.add("Toggle stats").setOnMenuItemClickListener { callbacks.onToggleStats(); true }
            menu.menu.add("Toggle colliders").setOnMenuItemClickListener { callbacks.onToggleColliders(); true }
            menu.menu.add("Focus selection").setOnMenuItemClickListener { callbacks.onFocusSelection(); true }
            menu.menu.add("Reset camera").setOnMenuItemClickListener {
                state.log("camera reset")
                callbacks.onFocusSelection()
                true
            }
        })
        addView(menu("Build") { menu ->
            menu.menu.add("Build debug APK settings").setOnMenuItemClickListener { callbacks.onBuild(); true }
            menu.menu.add("Export project bundle").setOnMenuItemClickListener {
                val project = state.project
                if (project == null) state.log("no project open", EditorState.ConsoleLine.Level.WARN)
                else {
                    val manager = ProjectManager(Platform(Engine.get().context))
                    manager.saveProject(project)
                    state.log("Project bundle exported to ${project.rootDir.absolutePath}")
                }
                true
            }
        })
        addView(menu("Help") { menu ->
            menu.menu.add("Console commands").setOnMenuItemClickListener {
                state.log(EditorCommands.HELP)
                true
            }
            menu.menu.add("About Sura Engine").setOnMenuItemClickListener {
                Dialogs.alert(context, "Sura Engine 1.0.0",
                    "A complete offline 2D game engine and editor for Android.\n\n" +
                        "Renderer: Canvas + OpenGL ES 2.0\n" +
                        "Scripting: SuraScript VM\n" +
                        "Physics: custom 2D solver\n" +
                        "Editor: hierarchy, inspector, console, asset browser, profiler")
                true
            }
        })
    }

    private fun brand(): TextView = EditorTheme.label(context, "  SURA ENGINE ", 14f, EditorTheme.accent, true)

    private fun menu(title: String, build: (PopupMenu) -> Unit): TextView {
        val view = EditorTheme.label(context, "$title \u25BE", 12.5f, EditorTheme.text)
        view.setBackgroundColor(EditorTheme.panelAlt)
        view.setOnClickListener {
            val popup = PopupMenu(context, it)
            build(popup)
            popup.show()
        }
        return view
    }

    private fun createWith(name: String, build: (com.sura.engine.scene.GameObject) -> Unit) {
        val go = state.createObject(name) ?: return
        build(go)
        state.log("Created $name")
        state.notifyChanged()
    }

    private fun duplicate() {
        val go = state.selection ?: return
        val scene = state.scene ?: return
        val serializer = com.sura.engine.serialization.SceneSerializer()
        val copy = serializer.deserializeObject(serializer.serializeObject(go), scene)
        copy.name = state.uniqueName(go.name)
        copy.transform.position.x += state.snapSize
        scene.adopt(copy)
        state.select(copy)
        state.isDirty = true
        state.log("Duplicated ${go.name}")
    }

    private fun onCreateProject() {
        Dialogs.prompt(context, "New project name", "MyGame") { name ->
            if (name.isBlank()) return@prompt
            val manager = ProjectManager(Platform(Engine.get().context))
            val project = manager.createProject(name)
            val scene = DemoScenes.build("empty")
            scene.name = "MainScene"
            manager.saveScene(project, scene)
            callbacks.onNewProject(project.name)
            state.log("Project '${project.name}' created at ${project.rootDir.absolutePath}")
        }
    }

    private fun onOpenProject() {
        val manager = ProjectManager(Platform(Engine.get().context))
        val projects = manager.listProjects()
        if (projects.isEmpty()) {
            Dialogs.alert(context, "No projects", "Create a project first (File > New project).")
            return
        }
        Dialogs.choose(context, "Open project", projects) { index ->
            val project = manager.openProject(projects[index]) ?: return@choose
            val scenes = manager.listScenes(project)
            if (scenes.isEmpty()) {
                val scene = DemoScenes.build("empty")
                manager.saveScene(project, scene)
                state.scene = scene
            } else {
                state.scene = manager.loadScene(project, scenes.first())
            }
            callbacks.onOpenProject(project.name)
            state.log("Opened project '${project.name}'")
        }
    }

    private fun onLoadDemo() {
        val demos = DemoScenes.names()
        Dialogs.choose(context, "Load demo scene", demos) { index ->
            val scene = DemoScenes.build(demos[index])
            state.scene = scene
            state.select(null)
            state.log("Demo '${scene.name}' loaded (${scene.allObjects.size} objects)")
        }
    }
}
