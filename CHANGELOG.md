# Changelog

Full per-version notes live in [`docs/`](docs). Versions are listed newest first.

## 1.9.0 (Minecraft 26.1.2, 26.2, 26.3)
- New gradient bar and stop markers: framed full-height bar, thin stems with color knobs below it instead of squares covering the gradient, larger accent-ringed selected stop, hover highlight, and the selected stop's position shown in percent.
- Larger, easier-to-grab stop hit area. Bordered preview box and evenly framed color swatch.
- Added Minecraft 26.2 support (new, untested).
- One shared source for 26.1.2 / 26.2 / 26.3 with a small per-version `Compat` adapter.
- Jar files are now named `floptext-rgb-<version>-<minecraft>.jar`.

## 1.8.1 (Minecraft 26.3)
- Rebuilt from source for 26.3 with a small compatibility layer for 26.3's screen API (`gui.setScreen` / `gui.screen()`), SDL input (left click is 1, SDL key codes), the hotkey `KeyMapping`, and the screenshot color picker.
- Confirmed working in a 26.3 client. Supersedes the earlier metadata-only candidate.

## 1.8.1 (Minecraft 26.1.2)
- Added a small "Click to edit" tooltip when hovering the selected stop's color swatch.

## 1.8.0 (Minecraft 26.1.2)
- Added an in-mod HSV color selector, opened by clicking the selected stop's swatch, with a live preview and editable HEX field.
- Added Apply Color and Cancel actions. The screenshot-based picker is unchanged.

## 1.7.1 (Minecraft 26.1.2)
- Fixed the Copy / Copy To Clipboard actions and added a visible "copied" confirmation.

## 1.7.0 (Minecraft 26.1.2)
- Port of 1.7.0 to Minecraft 26.1.2.
- Fixed missing gradient previews, the Windows PC file picker, Format selector behavior, preview spacing for bold text, and the folder page layout.

## 1.7.0 (Minecraft 1.21.11): legacy
- "Better Presets" drop: Favorites, folders, preset library, JSON import/export, in-game file browser.
- Built-in Community Presets: 431 gradients across 16 categories.
- No further updates are planned for 1.21.11.
