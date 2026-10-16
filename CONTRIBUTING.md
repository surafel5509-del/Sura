# Contributing to Sura Engine

Thanks for helping make a fully offline game engine better!

## Workflow

1. Fork / branch (`feature/short-name`).
2. Keep the engine dependency-light: **no new runtime libraries** unless strictly necessary.
3. Make sure `gradle :app:testDebugUnitTest` passes and add tests for new logic.
4. Keep the Kotlin style consistent with the codebase (4-space indent, explicit types on public API,
   KDoc for public classes).
5. Open a pull request describing what changed and why, screenshots for editor changes.

## Rules of the engine

* **Offline first** — no network calls, no analytics, no accounts.
* **Mobile first** — target 60 FPS on a mid-range phone; avoid per-frame allocations in hot paths.
* **No reflection** — component serialization is explicit; keeps startup fast and ProGuard safe.
* **Every editor action is undoable** — push an `UndoStack` command when mutating the scene.

## Code map

See [docs/architecture.md](docs/architecture.md) for the full package layout.

## Reporting bugs

Please include: device + Android version, exact steps, expected vs actual behaviour, and the
console output from the editor (`Console` tab) if available.
