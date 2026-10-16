# Sura Engine — Architecture

Sura Engine is a compact, dependency-light 2D engine written in Kotlin for Android (API 21+).
It is organised as a set of small subsystems that only talk to each other through well-known
interfaces, which keeps the engine fast on mobile hardware and easy to extend.

```
                    ┌──────────────────────────────────────────┐
                    │              Editor (IDE)                │
                    │  menus · toolbar · hierarchy · inspector │
                    │  assets · console · profiler · gizmos    │
                    └───────────────┬──────────────────────────┘
                                    │ mutates
┌───────────────┐        ┌──────────▼──────────┐        ┌──────────────────┐
│  InputManager │───────▶│    Scene / SceneMgr │◀───────│  Serializer      │
│  multitouch   │        │ GameObject·Component│        │  projects·prefabs│
└───────────────┘        │      Transform      │        └──────────────────┘
                         └────┬────────┬───────┘
                              │        │
             ┌────────────────▼──┐  ┌──▼──────────────────┐
             │  PhysicsWorld     │  │  SuraScript VM      │
             │  bodies·colliders │  │  lexer→parser→eval  │
             │  events·raycasts  │  │  engine API bridge  │
             └───────────────────┘  └─────────────────────┘
                              │
                    ┌─────────▼──────────┐
                    │  Renderers         │
                    │  GLRenderer (GLES2)│
                    │  CanvasRenderer    │
                    │  SceneRenderer     │
                    └────────────────────┘
```

## Core loop

`EngineView` (editor) and `GLSurfaceView`-based players own the loop:

1. `Time.update()` computes delta time (clamped to 0.25 s to survive pauses).
2. `Engine.update()` ticks every registered `EngineService` (scene manager, physics, audio,
   assets, scheduler, jobs) and then runs the **fixed timestep** physics steps (1/60 s,
   max 5 catch-up steps).
3. Scene simulation: `Scene.update(dt)` → every component's `update`, `fixedUpdate`, `lateUpdate`.
4. `SceneRenderer` draws tilemaps → sprites (depth sorted) → particles; lights and gizmos are
   layered on top by the active view.

## Subsystems

| Package | Responsibility | Key types |
|---------|----------------|-----------|
| `core` | Engine singleton, service locator, timing, jobs, scheduler, logging | `Engine`, `ServiceLocator`, `Time`, `JobSystem`, `Scheduler`, `Logger` |
| `scene` | Scene graph, components, lifecycle | `Scene`, `SceneManager`, `GameObject`, `Component`, `Transform` |
| `rendering` | Camera, sprites, renderer backends, scene drawing, lights | `Camera`, `Sprite`, `GLRenderer`, `CanvasRenderer`, `SceneRenderer`, `Light2D` |
| `physics` | 2D rigid bodies, colliders, collision events, queries | `PhysicsWorld`, `RigidBody2D`, `BoxCollider2D`, `CircleCollider2D` |
| `scripting` | SuraScript language: tokenizer, parser, AST, interpreter | `Lexer`, `Parser`, `Interpreter`, `ScriptBehaviour` |
| `animation` | Keyframe clips, tweens, easing | `AnimationClip`, `Animator`, `Tween`, `Easing` |
| `particles` | Particle simulation (CPU) rendered by any backend | `ParticleSystem`, `Particle` |
| `tilemap` | Multi-layer tilemaps, painting, tile collision | `Tilemap`, `TileLayer`, `TileSet`, `TilemapComponent` |
| `nav` | A* pathfinding on uniform grids | `NavGrid`, `BinaryHeap` |
| `ui` | Retained-mode UI widgets and layout | `Canvas`, `Panel`, `Button`, `Label`, `Slider`, `UIElementHolder` |
| `editor` | The IDE itself (landscape layout, panels, dialogs, tools) | `EditorScreen`, `EditorState`, `EngineView`, `InspectorView`, … |
| `project` | Project folders, settings, prefabs, scenes on disk | `Project`, `ProjectSettings`, `ProjectManager` |
| `serialization` | Dependency-free JSON codec + scene serializer | `Json`, `JsonValue`, `SceneSerializer` |
| `debug` | Frame and memory profiling, FPS history | `Profiler` |
| `save` | Save slots, quick save | `SaveSystem` |
| `assets` | Offline asset access (textures, text, byte blobs) | `AssetManager` |
| `games` | Playable demo games | `DemoScenes` |

## Rendering

Two interchangeable backends implement the same `Renderer` interface:

* **`GLRenderer`** — OpenGL ES 2.0, one shader, sprite batching up to 4096 quads per draw call,
  `Matrix4` orthographic projection, runtime-generated bitmap font atlas for text.
* **`CanvasRenderer`** — pure `android.graphics` drawing; used by the editor view so previews
  work on every device/emulator, including software renderers.

`SceneRenderer` is backend agnostic: it walks the scene, sorts by `Transform.zOrder`, draws
sprite components with their sprite/colour/size, then particles, and finally leaves lights and
gizmos to the active view.

## Physics

`PhysicsWorld` performs iterative AABB/circle resolution with mass-weighted separation, restitution
and friction, dispatches `onCollision` / `onTriggerEnter` callbacks to `ScriptBehaviour`s, and
offers raycasts (`raycast`), point/rect queries (`queryPoint`, `queryRect`) and tilemap helpers
for platformers (`TilemapCollider`).

## Scripting

SuraScript is compiled in four stages:

```
source ──Lexer──▶ tokens ──Parser──▶ AST (Stmt/Expr) ──Interpreter──▶ values
```

* Numbers are doubles, booleans and strings are native, `null` is supported.
* Functions, recursion, `for i in a..b`, `while`, `if/else`, ternary `?:`, `and/or/not`.
* The VM is sandboxed: a step budget per call prevents infinite loops, and errors are reported
  with line numbers into the editor console.
* Engine values are bound as variables (`x`, `y`, `rotation`, `scaleX/Y`, `active`, `time`) and
  engine functions are registered as natives (`move`, `spawn`, `setVelocity`, `touchX`, …).

## Offline by design

* No network permissions, no analytics, no licence checks.
* Assets, projects, saves and settings live in the app's private storage.
* APK builds run on-device (build spec export) or in CI, never requiring a remote service.
