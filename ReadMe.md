### One jar containing every platform
```
./gradlew fatJarAllPlatforms
```

### Seven slim jars, one per platform
```
./gradlew fatJarAllSeparate
```
### Single platform jar
```
./gradlew fatJar-natives-windows-x86
```

Output in `build/libs/`:
```
lwjgl-sdl3-1.0-SNAPSHOT-all-platforms.jar     ← ~80MB, runs anywhere
lwjgl-sdl3-1.0-SNAPSHOT-natives-windows.jar   ← ~15MB, Windows x64 only
lwjgl-sdl3-1.0-SNAPSHOT-natives-windows-x86.jar  ← Windows 32-bit
lwjgl-sdl3-1.0-SNAPSHOT-natives-linux.jar     ← Linux x64 only

```



| Limitation                             | Detail                                                                                                                  |
|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| Java bytecode is always cross-platform | No issue here — .class files run anywhere                                                                               |                                  |
| 32-bit Windows jar                     | Requires 32-bit JRE on target machine — the JVM itself must be 32-bit, you cannot run a 32-bit native from a 64-bit JVM |
| macOS notarization                     | If distributing .app on macOS, Apple requires code signing — a fat-jar bypasses this                                    |                  
| Fat-jar                                | sizeAll-platforms jar can be 80–100MB — use per-platform slim jars for distribution                                     |
| No cross-compilation needed            | LWJGL natives are pre-built by the LWJGL team — you just download and repackage                                         |                                        