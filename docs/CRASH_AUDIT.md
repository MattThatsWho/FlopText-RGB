# Crash Audit — `floptext-1.7.0-26.1.2`

## Known historical issues corrected
- `ClientTickEvents.END_CLIENT_TICK`: field descriptor corrected to Fabric's `Event` type; erased `Event.register(Object)` and callback name corrected.
- `Screen.addRenderableWidget`: stale `AbstractWidget` descriptor changed to the 26.1.2 `GuiEventListener` descriptor.
- Text APIs: `Component.literal`, `MutableComponent.withStyle`, `Font.width(FormattedText)`, boxed formatting style methods, and primitive-int `Style.withColor(int)` linkage checked.
- Rendering: GUI text render colors now carry opaque ARGB alpha; style-level gradient colors remain RGB. This addresses transparent/invisible previews.

## Validation status

The above signatures were checked in bytecode using `javap` and local API-signature tests. No complete Gradle build or Minecraft launch was possible here. More runtime errors may remain; inspect the user's next `latest.log` or crash report and keep this version in testing until the UI paths are confirmed.
