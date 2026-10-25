# FlopText RGB — Minecraft 26.1.2 port workspace

**Target artifact name:** `floptextrgb-1.8.0-26.1.2.jar`  
**Mod id:** `floptext_rgb`  
**Base:** exact `floptext-1.7.2-26.1.2.jar` downloaded from `public/mods/26.1.2/` in the connected GitLab project. The original v1.7.0 baseline remains preserved for reference.

This is an active port workspace, not yet a verified build. The Minecraft API was renamed and the GUI extraction/render-state architecture changed in 26.1; reconstructed sources have been migrated toward official Mojang names and the 26.1.2 API. No full Gradle compile or in-game launch was possible inside the current sandbox because it has Java 21 rather than Java 25, no Gradle installation, and no network DNS access for downloading the toolchain/dependencies.

## Build environment

- JDK 25
- Gradle 9.7.1 (wrapper distribution URL is recorded in `gradle/wrapper/gradle-wrapper.properties`)
- Minecraft 26.1.2
- Fabric Loader 0.19.5
- Fabric API 0.155.3+26.1.2
- Fabric Loom 1.18-SNAPSHOT
- Mod Menu 18.0.0
- Cloth Config 26.1.154

The wrapper scripts/JAR are not bundled in this workspace because they could not be obtained without network access. With Gradle 9.7.1 and JDK 25 installed, run `gradle build`. Output is configured to be `build/libs/floptextrgb-1.8.0-26.1.2.jar`.

## Port progress and known risks

- Updated `fabric.mod.json` and build dependencies for 26.1.2.
- Moved Java imports from Yarn/1.21.11 names toward official 26.1 names.
- Migrated screens to `extractRenderState` / `GuiGraphicsExtractor` render API names, current screen widget classes, and updated keymapping classes.
- Preserved the original v1.7.0 reference JAR in `reference/`.
- Existing preset catalogue is preserved as shipped resource data (431 entries, 16 categories).
- Preserved the screenshot-sampling picker and added a separate click-the-swatch HSV selector with live HEX preview, drag support, Apply Color and Cancel.
- Fixed all stale `Screen.addRenderableWidget(AbstractWidget):AbstractWidget` references to the correct erased `GuiEventListener` descriptor in the packaged candidate.
- This source was reconstructed from compiled bytecode, not restored from original authoring source. Functional parity is not guaranteed.
- The 26.1 rendering architecture changed substantially. Screen drawing/hover state, custom click and drag behavior, widget callbacks, Mod Menu integration, config/preset import/export, and all screens must be compiled and tested in the 26.1.2 client. Some renames below may need manual API fixes once an actual compile is available.
- Do not install or publish a JAR until it has successfully compiled, loaded, and passed in-game tests.

## Version-neutral JSON

Keep the preset JSON interchange shape unchanged unless a test reveals a real schema requirement. Minecraft 26.1.2's UI/API migration does not itself require changing the mod's data interchange JSON. Generated output is stored as a literal string plus format label; the website should retain that exact text.

## Reference

Official Fabric 26.1 port instructions: https://github.com/FabricMC/fabric-docs/blob/main/versions/26.1.2/develop/porting/index.md  
Official 26.1.2 Example Mod: https://github.com/FabricMC/fabric-example-mod/tree/26.1.2

## Run available static checks

```sh
python3 scripts/verify_port.py
java scripts/ParseSources.java src/main/java
scripts/run_core_smoke_tests.sh
python3 scripts/verify_ui_parity.py
```

These tests are intentionally labeled static/source-level checks. They do not replace compilation against the real Minecraft 26.1.2, Fabric Loader/API, Cloth Config and Mod Menu artifacts.

See `docs/PORT_26.1.2_STATUS.md` before attempting to build or install this source workspace.

## Latest regression-fix candidate (in testing)

The current candidate is named `floptextrgb-1.8.0-26.1.2.jar`, based on the exact `floptext-1.7.2-26.1.2.jar` downloaded from the project's GitLab repository. It adds an HSV saturation/brightness plane and hue slider in a modal opened by clicking the selected-stop color swatch. The hex field and preview update as the user drags or types; Apply Color commits the selected RGB value and Cancel discards edits. The screenshot/in-game picker button remains separate and unchanged. The website and all other Minecraft-version targets are intentionally untouched. This source reconstruction was compiled for static checks against local 26.1.2 API-signature stubs; it has not been Gradle-built against official Minecraft dependencies or launched in a real client in this environment.

Run the static validation suite with `scripts/run_all_static_checks.sh [path-to-jar]`. The current environment did not complete a full Gradle build against official Minecraft/Fabric dependencies or launch a real game client. This is reconstructed source, not the original authoring repository, and the Windows file dialog must be tested on Windows.
