# FlopText RGB 1.8.1 — Minecraft 26.3 port status

**Status:** Experimental compatibility candidate packaged; **not yet verified in a live 26.3 client**.

## Candidate and baseline

- Candidate: `jar Folder/floptext-1.8.1-26.3.jar`
- Baseline: `/FlopText RGB/Mods/26.1.2/jar Folder/floptextrgb-1.8.1-26.1.2.jar`
- Candidate SHA-256: `e4597ba8ac486ac1d6c5341c0a856565953d41839b014919953ece9bc5fe255c`

At the owner's direction, the first 26.3 test candidate was created by updating only `fabric.mod.json` in the working 26.1.2 JAR. This allows a focused compatibility test, but it is not equivalent to recompiling against Minecraft 26.3 APIs. The 26.1.2 baseline remains unchanged.

## 26.3 target settings

- Minecraft `26.3`
- Java runtime `>=25`
- Fabric Loader `>=0.19.5`
- Fabric API `0.165.0+26.3` (provided by the instance; mod metadata accepts any version)
- Cloth Config `>=26.3.159`
- Mod Menu `>=21.0.0` recommended
- Source build config: Fabric Loom `1.19`, Gradle `9.7.1`
- Candidate filename `floptext-1.8.1-26.3.jar`

Minecraft 26.3 uses Java 25+. The candidate's class files are Java class-file major version 65 (Java 21 bytecode), which can run on Java 25. This describes the bytecode baseline, not the Minecraft compatibility level.

## Packaging checks passed

- ZIP/JAR integrity: passed (39 entries).
- 35 packaged `.class` files parse; all have class-file major 65.
- Metadata targets version `1.8.1-26.3`, Minecraft `26.3`, Java `>=25`, Fabric Loader `>=0.19.5`, Cloth Config `>=26.3.159`, and recommends Mod Menu `>=21.0.0`.
- Only changed archive entry versus the baseline: `fabric.mod.json`.
- All 35 class files and every other resource are byte-identical to the 26.1.2 JAR, including `assets/floptext_rgb/community_presets.json`.

## Unverified / blocking items

The current host has OpenJDK 21.0.12, not JDK 25; Gradle is not installed; network DNS is unavailable to fetch the official toolchain or dependencies. Therefore no build was run against official Minecraft 26.3/Fabric/Cloth Config/Mod Menu dependencies, and no 26.3 Prism launch was possible here. The candidate might work if relevant APIs remain binary compatible, but compatibility cannot be inferred from metadata and archive checks alone.

The source workspace uses reconstructed Java sources from the 26.1.2 candidate rather than the original source repository. Source/config checks don't establish exact source-to-class parity.

## Required Prism runtime checks

- Launch without crash; entrypoint and Mod Menu registration; open via hotkey and Mod Menu.
- Main editor rendering/layout, text input, gradient/output formatting, buttons, mouse/keyboard input.
- Color swatch tooltip (`Click to edit`), HSV modal, square/hue dragging, HEX input, Apply and Cancel.
- Screenshot picker, clipboard, external PC file picker.
- Library previews/scrolling/sticky Folders/Favorites heading; folder actions and Favorites JSON export.
- Community Presets filters/pagination/previews/clicks; 431 entries across 16 categories.
- JSON and legacy ZIP import/export, config persistence, inspect `latest.log` and crash reports.

A genuine source rebuild with JDK 25 and official 26.3 dependencies should follow if the candidate fails any test. Don't overwrite the baseline or mark the port stable until the owner confirms runtime behavior.
