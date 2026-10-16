# Tutorial: your first game in 10 minutes (no PC needed)

> Everything below happens on the phone/tablet, offline.

## 1. Open the editor

Launch **Sura Engine**. The editor opens in landscape with a demo platformer already loaded
so you immediately see a working scene.

## 2. Create your project

`File ▸ New project` → name it `MyGame`.
The engine creates a project folder with `Assets`, `Scenes`, `Scripts`, `Prefabs` and `Build`.

## 3. Build a level with tiles

1. `Create ▸ Tilemap` — a 16×16 tilemap appears and is selected.
2. In the **Inspector** find the `TilemapComponent` section and tick **solid** on the `Ground` layer.
3. Choose the **Paint** tool in the tool palette, then drag on the scene view to draw tiles.
4. Use **Erase** to remove tiles, and `View ▸ Toggle grid` to line things up.

## 4. Add a player

1. `Create ▸ Physics box` → rename it to `Player` in the Inspector.
2. `Add + ▸ Script` in the Inspector.
3. Tap **Edit script** and paste the `PlayerController.sura` from this repository
   (`samples/scripts/PlayerController.sura`), then **Check** and **Apply**.
4. Drag the player with the **Move** tool to the start position.

## 5. Add a camera

`Create ▸ Camera`. Select it, then set **Follow target** to `Player` in the Inspector.
Play mode now follows the player smoothly.

## 6. Add life to the scene

* `Create ▸ Particle system` for dust, sparks or fireflies (Inspector exposes emission, lifetime,
  speed and colour ramps).
* `Create ▸ Light 2D` for torches — enable **Flicker** for a living glow.
* `Create ▸ Sprite` + `Add + ▸ Sprite Animator` for frame animation (set the sheet columns/rows
  and add a clip in code or through the inspector).

## 7. Play it

Press ▶ **Play** in the tool palette. The editor hides its overlays, physics and scripts run.
Use ⏸ **Pause** / ⏭ **Step** to debug frame by frame, then ⏹ **Stop** — the scene is restored
exactly as it was.

## 8. Save and export

* `File ▸ Save scene` writes `Scenes/<name>.scene`.
* `Build ▸ Build settings` collects app name, package id, version and orientation and writes
  `Build/build-spec.json`.
* Everything is stored inside the project folder — copy it out, or build an APK with the bundled
  Gradle project (`docs/building.md`).

## 9. Cheat sheet

| Action | How |
|--------|-----|
| Undo / redo | `Edit ▸ Undo` or the Undo button in the inspector |
| Console | bottom panel, type `help` |
| Spawn object | `spawn Enemy` in the console |
| Load a demo | `File ▸ Load demo` |
| Run script live | console: `script x = 100` |
