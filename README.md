<div align="center">

# 🎨 FlopText RGB

*Create it. Color it. Save it. A Birdflop-style gradient tool, natively in-game.*

[Website](https://mattsmods.vercel.app/floptextrgb) · [Download](https://mattsmods.vercel.app/floptextrgb/download) · [Report a bug](https://mattsmods.vercel.app/floptextrgb/report-bug) · [Suggestions](https://mattsmods.vercel.app/floptextrgb/suggestions) · [Support](https://mattsmods.vercel.app/floptextrgb/support) · [Threads](https://mattsmods.vercel.app/floptextrgb/threads) · [Community](https://mattsmods.vercel.app/floptextrgb/community) · [Collections](https://mattsmods.vercel.app/floptextrgb/collections)

</div>

FlopText RGB is a **client-side Fabric mod** that lets you build multi-stop RGB text gradients inside Minecraft, so you don't have to tab out to a browser. It is built for anything you'd normally use Birdflop's RGB tools for, like mob name tags, MOTDs and chat formatting.

## Features

- **In-game gradient editor**: open it with the hotkey (**V** by default, rebindable in Controls) or from Mod Menu.
- **Multi-stop gradients**: drag color stops along a rail and see the result live.
- **HSV color selector**: click the selected stop's swatch for a hue/saturation/brightness picker with a live HEX field.
- **Polished gradient bar**: thin stop markers with color knobs sit under the bar so the whole gradient stays visible, and the selected stop shows its position in percent.
- **One consistent look**: every screen (editor, Library, Community Presets, Settings, dialogs) shares the same flat button style and colors, with confirm actions in blue, destructive ones in red, and active style toggles ([B] [I] [S] [U]) lit up.
- **Multiple output formats**: MiniMessage, JSON, plain hex strings and traditional section codes, plus a custom template.
- **Better Presets**: Favorites, custom folders, a preset library, and JSON import/export with a built-in file browser.
- **Community Presets**: a built-in catalogue of 431 gradient presets across 16 categories, including 135 mob gradients and 23 seasonal/holiday ones.

## Supported versions

| Minecraft | Folder | Status |
|---|---|---|
| **26.3** | [`versions/26.3`](versions/26.3) | Current. 1.8.1 build confirmed working in-game |
| **26.2** | [`versions/26.2`](versions/26.2) | **New, untested.** First 26.2 build |
| **26.1.2** | [`versions/26.1.2`](versions/26.1.2) | Current. 1.8.1 build confirmed working in-game |
| 1.21.11 | [`versions/1.21.11`](versions/1.21.11) | Legacy (1.7.0). **No more updates** |

The 26.x folders share the same Java source. Only `compat/Compat.java` differs, covering the screen API, input and key differences between Minecraft versions. 1.21.11 is separate legacy code.

## Installing

Download the mod from [Modrinth](https://modrinth.com) or the [website](https://mattsmods.vercel.app/floptextrgb/download). You also need:

1. **Fabric Loader** and **Fabric API**
2. **Cloth Config API** (matching your Minecraft version)
3. **Mod Menu**, recommended

## Building from source

| Folder | Java | Gradle |
|---|---|---|
| `versions/26.1.2`, `versions/26.2`, `versions/26.3` | 25 | 9.7.1 |
| `versions/1.21.11` | 21 | 9.2.1 |

```sh
cd versions/26.1.2
gradle build
```

The jar appears in `build/libs/`. No Gradle wrapper is included, so use an installed Gradle of the version above. GitHub Actions builds all three folders on every push (`.github/workflows/build.yml`).

## ⚠️ Source status (please read)

The Java code in this repository began as source **reconstructed from compiled jars**. It is not the original authoring source, and early versions were produced by patching compiled classes. Since then the reconstructed source has been built by GitHub Actions: the 1.8.1 builds for 26.1.2 and 26.3 compile and were confirmed working in-game. 26.2 is new and untested, and 1.21.11 is frozen.

The jars on Modrinth and the website are the authoritative releases.

## Docs

- [`CHANGELOG.md`](CHANGELOG.md): version history
- [`docs/`](docs): per-version changelogs, bug notes and port status reports
- [`docs/OUTPUT_FORMAT_COMPATIBILITY.md`](docs/OUTPUT_FORMAT_COMPATIBILITY.md): output format details

## Credits & transparency

- The in-game tool is an adaptation of the open-source text formatting tools from the RGBirdFlop / Birdflop community.
- This mod was built with a lot of AI assistance, along with a lot of manual testing, bug hunting and direction by the developer. It exists partly to inspire other mod makers. Fork it, expand it, or remake it better.

## License

[MIT](LICENSE) © MattThatsWho / FlopText RGB project
