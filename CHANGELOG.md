# Changelog

## 1.0.0 — Sura Engine first release

### Engine
* Scene graph with components, parenting, tags, layers and depth sorting.
* OpenGL ES 2.0 batched sprite renderer plus a Canvas fallback renderer.
* 2D physics: rigid bodies, box/circle colliders, friction, restitution, triggers,
  collision/trigger callbacks, raycasts and AABB queries.
* SuraScript VM (lexer, parser, tree-walking interpreter) with step-budget sandboxing
  and a full engine API (`move`, `spawn`, `setVelocity`, `touchX`, `log`, …).
* Particle systems, 2D lights, tilemaps with tile collision, A* navigation, virtual joystick.
* Animation: keyframe clips, animators, tweens, easing library.
* Retained-mode UI toolkit (panels, buttons, labels, sliders, layout groups, HUD support).
* Profiler (FPS graph, frame time, draw calls, memory), job system, scheduler, save slots,
  localisation tables, dependency-free JSON codec and scene serializer.
* Playable demos: platformer, space shooter, pong, top-down, particle showcase.

### Editor
* Full landscape IDE: menu bar, tool palette, hierarchy tree, inspector, asset browser,
  console with commands, profiler panel.
* Scene view with grid, axes, gizmos, collider overlay, lights, pinch zoom and pan.
* Play/Pause/Step/Stop with automatic scene snapshots and instant restoration.
* Undo/redo for every editing operation, project manager, prefabs, build settings dialog.

### Build & CI
* Gradle 8.7 + AGP 8.1 + Kotlin 1.9 project, unit test suite.
* GitHub Actions: tests → debug APK → release APK → artifacts, tag-driven releases,
  compiler diagnostics posted as commit comments on failure.
