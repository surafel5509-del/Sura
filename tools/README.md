# Engine tools

## `make_icons.py`

Generates the launcher icons (all densities) plus documentation copies:

```bash
python3 tools/make_icons.py
```

* Renders a 1024×1024 master in pure Python (no Pillow, no ImageMagick) with 1× sampling of
  rounded shapes and gradients, then box-filters it down to every `mipmap-*` density.
* Outputs `ic_launcher.png` and `ic_launcher_round.png` into
  `android/app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/`.
* Also writes `docs/icon-512.png` for the README/release notes.

Run it again whenever the brand colours change (palette constants are at the top of the file).
