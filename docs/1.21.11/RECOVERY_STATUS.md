# Source Recovery Status — FlopText RGB 1.7.0

## Source of truth

Exact baseline: `reference/FlopText-RGB-1.7.0.jar` (SHA-256: `e27829bf9a760315f87039791ceb69ec5d9ea204ca9c636ce85e89cffdf86cfb`).

The original JAR declares mod id `floptext_rgb`, version `1.7.0`, Minecraft `1.21.11`, Java `>=21`, Fabric Loader `>=0.18.0`, Fabric API, and Cloth Config. Mod Menu is a recommended integration.

## Recovered editable source coverage

There are 25 Java source files, representing the 25 top-level classes in the archive. Their implementations are **reconstructions**, not original repository sources. They cover:

- Client entrypoint, config registration, hotkey, and Mod Menu integration.
- Config and preset data model, stop interpolation/hex helpers, and formatted previews.
- Main RGB editor, multi-stop editing, formatting, output generation, copying, custom template, and color-selection palette.
- Community catalogue load/sanitize/search, category and card interaction.
- Favorites and named folder library, destination selection, load/save/delete actions, import/export dialogs, JSON transfer, legacy ZIP transfer, file browsing, file-picker settings, and custom pixel-art icon renderer.

The recovered sources aim to supply an editable baseline that can be built and iteratively corrected. They are not claimed to match original source line-for-line or to reproduce all original behavior perfectly. In particular:

- The reconstructed Color Picker currently offers a color palette; exact screen-color sampling has not been re-established from the source JAR.
- AWT/Swing file dialogs are not used; the in-game file browser is the default because the prior Java environment experienced platform-dialog failures.
- UI geometry and interaction have been reconstructed from class signatures, binary inspection, and retained requirements. They require actual testing at the user's GUI scale.
- Some helper methods intentionally delegate to UI setup performed directly in a screen's `init()` method to prevent duplicate widgets.

## Retained original evidence

- All 35 compiled `.class` files (including nested classes) are preserved byte-for-byte under `recovery/class-files/`.
- `recovery/bytecode/` contains disassembly and signature listings for the exact compiled classes.
- The source JAR and extracted packaged resources are preserved under `reference/` and `src/main/resources/` respectively.
- The exact source archive includes 431 Community Presets in 16 categories. Ignore older notes that report other totals when working with this exact JAR.

## Validation status

### Passed static checks

- Reference JAR SHA-256 captured and rechecked.
- Reference JAR archive is readable and has 40 unique archive entries.
- All 35 extracted class files match the corresponding class bytes in the original JAR.
- `community_presets.json` parses, contains 431 entries, and has 16 categories.
- JDK syntax parser reports zero Java syntax errors in the reconstructed source files (see `recovery/SOURCE_SYNTAX_CHECK.txt` after running the validation script).
- ZIP output passes `unzip -t` after packaging.

### Not verified

- Full Gradle compilation against Minecraft/Fabric dependencies.
- Game startup or Mod Menu launch.
- Actual click dispatch, drag behavior, modal blocking, GUI-scale layout, color-picker interaction, JSON/ZIP round trips, or filesystem behavior inside Minecraft.

The current workspace has JDK 21 but no installed Gradle/Maven and no dependency cache or external download access. Therefore this report intentionally does not claim a successful Gradle build or a tested mod JAR.

## Readiness

This project is now a structured **source-recovery baseline**, not a validated release. Do not replace the v1.7.0 reference JAR with a JAR built from this source until a compatible environment can compile it and the full in-game test pass is completed.
