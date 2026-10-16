# Editor guide

The editor opens in landscape and is split into five regions.

## 1. Menu bar

| Menu | Items |
|------|-------|
| **File** | New project, Open project, Save project, Save scene, Load demo, Exit editor |
| **Edit** | Undo, Redo, Duplicate selection, Delete selection, Select none |
| **Create** | Empty object, Sprite, Camera, Light 2D, Particle system, Physics box, Tilemap, Script object, UI Canvas |
| **Play** | Play, Pause/Resume, Step frame, Stop |
| **View** | Toggle grid, Toggle stats, Toggle colliders, Focus selection, Reset camera |
| **Build** | Build settings, Export project bundle |
| **Help** | Console commands, About |

## 2. Tool palette

`Select` `Move` `Rotate` `Scale` `Paint` `Erase` `Pan`, playback buttons, and fast toggles for
grid, stats, collider visualisation, snapping and Build.

### Scene view gestures

| Gesture | Action |
|---------|--------|
| Tap | Select the object under the touch (topmost first) |
| Drag object | Move (snapped to grid when Snap is on) |
| Two fingers drag | Pan camera |
| Pinch | Zoom camera |
| Paint/Erase tool + drag | Paint tiles into the selected tilemap layer |

## 3. Hierarchy

* Tree with expand/collapse arrows, component-type icons, active toggle.
* Search box filters across the whole scene.
* Long-press a row for: Rename, Duplicate, Create child, Move to root, Delete.
* `+ Create object` adds a new empty object at the camera centre.

## 4. Inspector

* Object name (editable), tag, layer, child count, active checkbox.
* `Transform`: position X/Y, rotation, scale X/Y, depth — with −/+ steppers that record undo steps.
* One section per component; the checkbox enables/disables it, `⋮` removes or resets it.
* `Add +` opens the component palette: sprite, physics, colliders, light, camera, particles,
  animator, tilemap, script.
* Script components open the **SuraScript editor** with `Check` (syntax), `Test run` (execute
  immediately) and `Insert template`.

## 5. Console & Profiler

* Console shows info/warn/error/command lines and accepts commands (`help` lists them).
* Profiler shows a live FPS graph with 30/60 FPS guide lines plus frame time, average, draw
  calls, object/component counts and memory usage.

## Playing and stopping

`Play` snapshots the scene as JSON, hides editor overlays and runs the real game loop with physics
and scripts. `Stop` restores the snapshot, so experimenting never destroys your level.

## Saving

* `File ▸ Save scene` writes `Scenes/<name>.scene` (JSON) inside the project folder.
* `File ▸ Save project` writes `project.sura.json` (app name, package, version, orientation…).
* The editor auto-opens the last used project on start.
