<div align="center">

# 🎮 Sura Engine

**A complete, professional 2D game engine and editor for Android — built from scratch, fully offline.**

[![Build](https://github.com/surafel5509-del/Sura/actions/workflows/build-apk.yml/badge.svg)](https://github.com/surafel5509-del/Sura/actions/workflows/build-apk.yml)
![Platform](https://img.shields.io/badge/platform-Android%2021%2B-3ddc84)
![Language](https://img.shields.io/badge/language-Kotlin-7f52ff)
![License](https://img.shields.io/badge/license-MIT-blue)
![Offline](https://img.shields.io/badge/network-not%20required-success)

</div>

---

## What is Sura Engine?

Sura Engine is a **from-scratch game engine and IDE that runs entirely on your phone or tablet**.
No PC, no internet, no cloud account, no external build server. Open the app, build your game,
press ▶ Play, and export a real Android APK.

It ships with everything you expect from a modern engine:

| Subsystem | What you get |
|-----------|--------------|
| **Editor** | Landscape IDE: menu bar, tool palette, Hierarchy, Inspector, Asset browser, Console, Profiler, Scene view with grid/gizmos |
| **Renderer** | Hardware OpenGL ES 2.0 renderer with sprite batching (up to 4096 quads/draw call) **and** a Canvas fallback renderer that runs on every device; per-project renderer choice |
| **Scenes** | Scene graph with parenting, tags, layers, depth sorting, prefabs, instantiation, serialization to `.scene` JSON |
| **Physics** | Custom 2D solver: rigid bodies (static/kinematic/dynamic), box & circle colliders, friction, restitution, triggers, collision events, raycasts, AABB queries |
| **Scripting** | **SuraScript** — an embedded scripting VM (lexer → parser → interpreter) with functions, loops, recursion, strings, and a full engine API |
| **Animation** | Keyframe clips, frame based sprite animation, sprite sheets, easing library, tween manager, coroutine-style timers |
| **Textures** | Offline atlas packer (grows to power-of-two atlases, reports unpacked sprites), runtime font atlas |
| **Particles** | GPU-friendly particle systems with emission shapes, gravity, colour/size interpolation |
| **Tilemaps** | Multi-layer tilemaps, tile painting in the editor, tile collision helper for platformers, tilemap serialization |
| **AI** | A* grid navigation with weighted cells and diagonal movement |
| **Lighting** | 2D point lights with flicker, additive/radial gradients |
| **Audio** | SoundPool-based SFX manager + streaming music player, volume buses |
| **Input** | Multi-touch manager, virtual joystick, gesture pan/zoom, key events |
| **UI** | Retained-mode UI toolkit: panels, buttons, labels, text fields, sliders, toggles, dropdowns, progress bars, layout groups |
| **Tools** | Profiler (FPS graph, frame ms, draw calls, memory), console with 20+ commands, undo/redo stack, scene manager, prefab tools, autosave, project manager, build manager |
| **Assets** | Offline asset pipeline: texture loading, bitmap font atlas generated at runtime, asset browser |
| **Saves** | Slot-based save system, project settings, localisation tables |

Everything is 100% offline: the engine never asks for the internet, an account or a licence key.

---

## Quick start (build the APK)

### Option A — install the ready-made APK (no PC needed)

1. Open **[Releases → v1.0.0](https://github.com/surafel5509-del/Sura/releases/latest)**.
2. Download **`app-debug.apk`** (installs on any Android 5.0+ device) or the release build.
3. Install it (allow "install unknown apps" if Android asks).
4. Launch **Sura Engine** — it opens in landscape with a demo platformer already loaded.

Every push also produces fresh APKs in the workflow run under **Artifacts**
(`sura-engine-debug-apk`, `sura-engine-release-apk`).

### Option B — Android Studio / command line

```bash
git clone https://github.com/surafel5509-del/Sura.git
cd Sura/android
gradle :app:assembleDebug      # requires JDK 17 + Android SDK 33
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release builds (unsigned) and a tag-driven GitHub Release with APKs are produced automatically:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

---

## The editor

The editor is designed for a **landscape** phone or tablet, exactly like Unity/Godot layouts:

```
┌────────────────────────────────────────────────────────────────────────────┐
│ SURA ENGINE   File Edit Create Play View Build Help           1.0.0        │
├────────────────────────────────────────────────────────────────────────────┤
│ TOOLS  Select Move Rotate Scale Paint Erase Pan │ ▶ ⏸ ⏭ ⏹ │ Grid Stats …   │
├──────────────┬────────────────────────────────────────────┬────────────────┤
│ HIERARCHY    │ Scene view (grid, gizmos, lights, colliders)│  INSPECTOR     │
│  ▾ Level     │                                            │  Player        │
│    ▸ Ground  │        drag  = move object                 │   Transform    │
│    Player ◉  │        pinch = zoom camera                  │   Sprite       │
│    Coin      │        two fingers = pan                    │   RigidBody2D  │
│  ASSETS      │                                            │   Script       │
├──────────────┴────────────────────────────────────────────┤  Duplicate …   │
│ Console ▸ Profiler                       type a command …  │                │
└────────────────────────────────────────────────────────────────────────────┘
```

* **Hierarchy** — tree view with search, expand/collapse, visibility toggle, long-press menu (rename, duplicate, create child, move to root, delete).
* **Inspector** — Unity-style component list with editable numeric fields (± buttons + keyboard), colour pickers, checkboxes, dropdowns, component add/remove/reset.
* **Asset browser** — browse the project folder, create scenes and scripts, instantiate prefabs, load demos.
* **Console** — log output plus a command line (`help`, `spawn`, `move`, `demo platformer`, `script x = 5`, …).
* **Profiler** — live FPS graph with 30/60 FPS guides, frame time, draw calls, object count, memory.
* **Play controls** — ▶ Play (with automatic scene snapshot), ⏸ Pause, ⏭ Step one frame, ⏹ Stop (restores the edit-time scene).
* **Undo/redo** — every editor mutation is a command; `Edit ▸ Undo/Redo`.

### Console commands

```
help | list | spawn <name> | delete | select <name> | move <x> <y>
grid [0/1] | snap [px] | stats | scene info|new <name>|save
script <code> | demo [platformer|shooter|pong|topdown|particles]
undo | redo | play | pause | stop | clear
```

---

## SuraScript — scripting without leaving the device

```js
// PlayerController.sura
var speed = 180
var jumpForce = 420
var velocityY = 0
var grounded = true

function start() {
    log("player ready")
}

function update(dt) {
    velocityY = velocityY - 900 * dt
    y = y + velocityY * dt
    if (y <= 0) {
        y = 0
        velocityY = 0
        grounded = true
    }
    if (touchActive()) {
        if (touchX() < screenWidth() / 2) {
            move(-speed * dt, 0)
        } else {
            move(speed * dt, 0)
            if (grounded) { velocityY = jumpForce; grounded = false }
        }
    }
}
```

Engine API exposed to scripts:

| Category | Functions |
|----------|-----------|
| Transform | `move(dx, dy)`, `setPosition(x, y)`, `x`, `y`, `rotation`, `scaleX`, `scaleY`, `active` |
| Physics | `setVelocity(x, y)`, `velocityX()`, `velocityY()`, `addForce(x, y)`, `isGrounded()` |
| Scene | `spawn(name, x, y)`, `destroySelf()`, `find(name)`, `objectX/Y(name)`, `distanceTo(name)`, `objectCount()` |
| Input | `touchActive()`, `touchX()`, `touchY()`, `screenWidth()`, `screenHeight()` |
| Time/Math | `engineTime()`, `deltaTime()`, `random(min, max)`, `clamp`, `lerp`, `sqrt`, `sin`, `cos`, `floor`, `ceil`, `abs`, `min`, `max` |
| Strings | `+`, `.length`, `.upper()`, `.lower()`, `.trim()`, `.split(sep)`, `.contains(s)`, `.substring(a, b)` |
| Debug | `log(message)`, `print(message)`, `type(value)` |

Lifecycle hooks: `start()`, `update(dt)`, `fixedUpdate(dt)`, `onCollision(name)`, `onTriggerEnter(name)`, `onDestroy()`.
Infinite loops are impossible — the VM enforces a step budget per call and reports the error in the inspector/console.

---

## Project layout

```
Sura/
├─ android/                           # Gradle project (the engine + editor app)
│  ├─ app/src/main/java/com/sura/engine/
│  │  ├─ core/         Engine, services, jobs, scheduler, time, logging
│  │  ├─ scene/        GameObject, Component, Transform, Scene, SceneManager
│  │  ├─ rendering/    Camera, Sprite, renderers, SceneRenderer, lights, GL backend
│  │  ├─ physics/      PhysicsWorld, RigidBody2D, colliders, raycasts, events
│  │  ├─ scripting/    Lexer, Parser, AST, Interpreter (SuraScript VM), ScriptBehaviour
│  │  ├─ animation/    Clips, Animator, Tween, TweenManager, Easing
│  │  ├─ particles/    ParticleSystem
│  │  ├─ tilemap/      TileSet, TileLayer, Tilemap, TilemapRenderer, collider helper
│  │  ├─ nav/          NavGrid + A* pathfinding
│  │  ├─ ui/           Panels, buttons, labels, sliders, layout, UIElementHolder
│  │  ├─ editor/       Full IDE: menus, toolbar, panels, gizmos, dialogs, console
│  │  ├─ runtime/      Standalone player activity
│  │  ├─ project/      Project + settings + manager (create/open/save)
│  │  ├─ serialization/Engine JSON codec + scene serializer
│  │  ├─ audio/ input/ assets/ save/ localization/ platform/ build/ debug/ utils/
│  │  └─ games/        Playable demos: platformer, shooter, pong, topdown, particles
│  └─ app/src/test/                  # JVM unit tests (VM, JSON, physics, A*, …)
├─ .github/workflows/build-apk.yml   # CI: tests → debug APK → release APK
└─ docs/                             # architecture, editor guide, scripting, build docs
```

Projects created inside the app live in the device file system:

```
/Android/data/com.sura.engine/files/Projects/<ProjectName>/
├─ Assets/     Scenes/     Scripts/     Prefabs/     Build/
├─ project.sura.json                  # app name, package, version, orientation…
└─ Scenes/Level1.scene                # serialized scene graph
```

---

## Engine in 20 lines

```kotlin
val scene = Scene("MainScene")

val player = scene.createGameObject("Player")
player.transform.position.set(120f, 240f)

player.addComponent(SpriteRenderer().apply {
    size.set(32f, 48f)
    color = Color(0.54f, 0.71f, 0.98f, 1f)
})
player.addComponent(RigidBody2D().apply { mass = 2f })
player.addComponent(BoxCollider2D().apply { size.set(32f, 48f) })
player.addComponent(ScriptBehaviour().apply {
    scriptName = "PlayerController"
    source = "function update(dt) { move(60 * dt, 0) }"
    compile()
})

sceneManager.loadScene(scene)
```

More: **[docs/architecture.md](docs/architecture.md)**, **[docs/editor.md](docs/editor.md)**,
**[docs/scripting.md](docs/scripting.md)**, **[docs/building.md](docs/building.md)**,
**[docs/api.md](docs/api.md)**, **[docs/tutorial.md](docs/tutorial.md)** and
ready-to-use scripts in **[samples/scripts](samples/scripts)**.

---

## Roadmap

* [x] Core ECS-lite scene graph, transforms, components
* [x] OpenGL ES 2.0 batching renderer + Canvas fallback
* [x] 2D physics with triggers, events and raycasts
* [x] SuraScript VM + engine API
* [x] Full landscape editor (hierarchy, inspector, assets, console, profiler)
* [x] Tilemaps, lights, particles, animation, A* navigation
* [x] Scene serialization, projects, prefabs, save slots
* [x] CI that publishes signed-ready APKs
* [x] Tilemap autotiling (16-entry neighbour masks)
* [x] Sprite atlas packer
* [ ] Audio mixer bus editor
* [ ] Multiplayer sandbox (LAN, offline-first)

---

## Contributing

See **[CONTRIBUTING.md](CONTRIBUTING.md)**. Bug reports and feature requests are welcome via GitHub Issues.

## Keyboard shortcuts (physical keyboard / Chromebook / DeX)

| Shortcut | Action |
|----------|--------|
| `Ctrl+Z` / `Ctrl+Shift+Z` | Undo / Redo |
| `Ctrl+S` | Save scene |
| `Ctrl+Shift+S` | Save project |
| `Ctrl+P` / `Space` | Play or Pause |
| `Ctrl+.` | Stop |
| `Ctrl+B` | Build dialog |
| `Ctrl+G` | Toggle grid |

## License

MIT © Sura Engine contributors — see **[LICENSE](LICENSE)**.
