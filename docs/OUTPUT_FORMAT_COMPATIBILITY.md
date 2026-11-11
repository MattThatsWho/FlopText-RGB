# FlopText RGB output and JSON compatibility note

## Decision: keep the current JSON schema unchanged

Do **not** create a different FlopText RGB preset JSON schema just for Minecraft 26.1.2. The JSON import/export used by the Better Presets/Library system is a FlopText RGB data format. It stores records such as a preset's name, input text, color stops and positions, style flags, output format, and custom template, as well as Favorites or folder containers. Its schema is owned by FlopText RGB, not by Minecraft's chat-component serialization.

For the website, store each community share as:

- `output_text`: the exact string copied from the mod;
- `output_format`: an explicit format label;
- optional `minecraft_version` / `mod_version` metadata if the creator chooses to provide it.

Render the submitted data as text by default and provide a Copy action that copies `output_text` byte-for-byte/string-for-string without reformatting it. Don't infer a format solely from the target Minecraft version.

## Important distinction: preset JSON vs. chat/command component syntax

Minecraft Java 1.21.5 changed command parsing for text components: commands such as `/tellraw` and `/title` moved from JSON text components to SNBT text components. Minecraft 1.21.11 is already after that change, and 26.1.2 is also after it. Therefore that particular JSON-to-SNBT transition does **not** create a new difference between these two target versions. It also does not mean that arbitrary JSON should be converted: the FlopText RGB library/preset JSON is a separate data format.

The v1.7.0 binary advertises these generated-output choices: `Legacy Hex`, `MiniMessage`, `Vanilla / Spigot`, and `Custom`. Its JSON import/export is a preset/library workflow, not a generated JSON chat-output format. Do not advertise a native JSON output mode for this exact v1.7.0 artifact unless a later inspected JAR actually contains one.

## Recommendation

Keep one version-neutral JSON format for website community creations and for FlopText RGB's preset exchange. If a future Minecraft-specific output format is added, treat it as a separate, explicitly named output-format feature with its own versioned implementation—not as a reason to mutate existing shared presets.
