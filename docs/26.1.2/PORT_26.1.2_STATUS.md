# Port status — FlopText RGB v1.7.0 → Minecraft 26.1.2

**Artifact:** `floptext-1.7.0-26.1.2.jar`  
**Status:** UI-restoration candidate for user testing; not a verified release.

## Reference and data preservation
- Uses the exact stored v1.7.0 JAR as its comparison baseline; the original reference archive remains untouched under `reference/`.
- The Community catalogue is byte-for-byte unchanged: 431 presets across 16 categories.
- The original icon and English language resource are byte-for-byte unchanged.
- This source was reconstructed from compiled bytecode, not recovered from an original authoring repository. Visual and behavioral parity is approximate and needs live testing.

## Minecraft 26.1.2 compatibility repairs
- Corrected Fabric `ClientTickEvents.END_CLIENT_TICK`, erased `Event.register(Object)`, callback `onEndTick`, and the stale inner-class metadata.
- Corrected linkage for `Component.literal`, `MutableComponent.withStyle`, `Font.width(FormattedText)`, and style formatting methods.
- Corrected `Style.withColor(int)` primitive descriptor; removed invalid `Style.withColor(Integer)` calls.
- Replaced all stale `Screen.addRenderableWidget(AbstractWidget):AbstractWidget` descriptors with the correct erased `GuiEventListener` signature, preserving each screen/subclass method owner.
- Removed duplicate registration in the main editor button helper.

## UI parity pass
- Main editor: original default blue/pink endpoint colors, selected first stop, charcoal panels, neutral borders, title/subtitle, Preview/Input/Output sections, B/I/S/U, gradient bar, editable stop handles, hex controls, format/copy/custom actions, favorites cards, and footer geometry.
- Main editor scroll: independent main/sidebar scroll state, the original compact two-pixel scrollbar track/thumb ratio, wheel routing to the relevant panel, and clipping of scrolled content.
- Icons: gear, book, and Community person are pixel-pattern drawings, not Unicode substitutions. The Settings gear no longer has an extra tile behind it; hover tint and labels are retained.
- Settings: original two picker options with Back; native PC file picker is the fresh-install default, and selecting it never silently falls back to the in-game browser. The in-game browser is the explicit secondary option.
- Library: folders before Favorites; original toolbar actions; individual favorite JSON actions; Open Preset Folder; modal click blocking; scrolled labels/buttons/dividers share the same offset and viewport clipping; Add Folder dialog extends below both lower buttons.
- Community Presets: compact header/search row, all 17 filter chips, responsive card grid, category/tag chips, Load controls, Back/Previous/Next, page/result count, original palette, and layered hover-bevel drawing. Rendering and click tests use the same recorded hitboxes.
- Previews: shared RGB renderer is used for folder cards and save-destination selection, with a live sample added to Add Preset to Folder; main editor, Favorites, Library, and Community paths remain present.
- Gradient stops: interior stops can cross while dragging; endpoints stay locked.
- Custom Output: full-screen dim overlay, original instruction text, template field disabled and dimmed when Custom output is inactive, activation checkbox, live preview, Save Template and Back actions.
- Import/export: native PC file dialog is the default, covers JSON and legacy ZIP workflows, enforces supported extensions after file selection, and does not switch silently to the in-game picker if system mode is selected.
- Hex picker: reconstructed screenshot-sampling flow using Minecraft 26.1.x APIs.

## Static verification
- `scripts/verify_port.py`: 12 checks passed.
- `scripts/verify_ui_parity.py`: 43 checks passed.
- `scripts/verify_crash_fix.py`: metadata, archive, preset catalogue, and known linkage checks passed.
- `scripts/verify_widget_registration.py`: 6 references use the correct erased `GuiEventListener` descriptor; no old `AbstractWidget` descriptor remains.
- `scripts/ParseSources.java`: 26 Java sources, zero syntax errors.
- `scripts/run_core_smoke_tests.sh`: 7 gradient assertions passed.
- Final candidate JAR: 39 unique archive entries, no CRC errors or duplicate paths; 35 class files parse successfully.

## Important limitations
- The full project has not been compiled by Gradle against the actual Minecraft 26.1.2, Fabric API, Cloth Config, and Mod Menu artifacts in this environment.
- The current JAR has not been launched in the actual 26.1.2 client after the most recent UI changes.
- Pixel-level layout, scroll handling, button/icon dispatch, screenshot color sampling, configuration persistence, Community interactions, folder operations, and JSON/ZIP round trips need in-game regression tests.
- This build must remain labelled In Testing until the user confirms launch, appearance, and interactions.

If another issue appears, keep the same version identifier during this test cycle, add the report to the bug notes, and patch the same candidate until the user confirms it works.
