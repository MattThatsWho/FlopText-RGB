# Contributing

Thanks for helping out!

- **Bugs, suggestions and help:** use the website, not GitHub issues: https://mattsmods.vercel.app/floptextrgb/report-bug
- **Layout:** each Minecraft version is its own Gradle project under `versions/<mc>` (`26.1.2`, `26.2`, `26.3`, `1.21.11`). The 26.x folders share identical Java source except `compat/Compat.java` (screen API, input and key differences per version), so a fix usually needs to be copied into all three. `1.21.11` is separate legacy code.
- **Build:** see the README. 26.x needs JDK 25 + Gradle 9.7.1; 1.21.11 needs JDK 21.
- **Status:** 1.21.11 is legacy and won't get updates. 26.2 is new and untested.
- **Please be honest in PRs** about whether you compiled and launched the mod in-game.
