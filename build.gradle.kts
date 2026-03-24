
plugins {
    java
    application
}

group = "com.pikuma"
version = "1.0-SNAPSHOT"


// ── LWJGL version ────────────────────────────────────────────────────────────
val lwjglVersion = "3.4.1"


// ── All target platforms — build fat-jar for all from any OS ─────────────────
val lwjglNativesList = listOf(
    "natives-linux",
    "natives-linux-arm64",
    "natives-macos",
    "natives-macos-arm64",
    "natives-windows",
    "natives-windows-x86",       // 32-bit Windows
    "natives-windows-arm64"
)

// ── Detect natives classifier from current OS / arch ─────────────────────────
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

// ── Separate configuration to hold ALL natives ────────────────────────────────
val allNatives: Configuration by configurations.creating

//val lwjglNatives: String = when {
//    osName.contains("windows") -> when {
//        osArch.contains("aarch64") -> "natives-windows-arm64"
//        osArch.contains("x86")     -> "natives-windows-x86"
//        else                       -> "natives-windows"
//    }
//    osName.contains("mac") -> when {
//        osArch.contains("aarch64") -> "natives-macos-arm64"
//        else                       -> "natives-macos"
//    }
//    else -> when {                                          // Linux fallback
//        osArch.contains("aarch64") -> "natives-linux-arm64"
//        else                       -> "natives-linux"
//    }
//}
//
//println(">> Detected LWJGL natives: $lwjglNatives")

repositories {
    mavenCentral()
    // LWJGL's own repo — required for SDL3 and latest builds
    maven {
        name = "LWJGL Releases"
        url  = uri("https://build.lwjgl.org/release/latest/maven")
    }
}

dependencies {

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    // ── Java API jars (platform-independent) ─────────────────────────────────
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-sdl")
    implementation("org.lwjgl:lwjgl-opengl")

    // ── Native jars for EVERY platform ───────────────────────────────────────
    lwjglNativesList.forEach { natives ->
        allNatives("org.lwjgl:lwjgl::$natives")
        allNatives("org.lwjgl:lwjgl-sdl::$natives")
        allNatives("org.lwjgl:lwjgl-opengl::$natives")
    }

// ── OpenGL (optional – for rendering into the SDL window) ────────────────
//    implementation("org.lwjgl:lwjgl-opengl")
//    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
}

// ── Application entry point ───────────────────────────────────────────────────
application {
    mainClass = "com.pikuma.Main"
}

// ── Java toolchain ────────────────────────────────────────────────────────────
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// ── Fat-jar bundling ALL platforms ───────────────────────────────────────────
tasks.register<Jar>("fatJarAllPlatforms") {
    group             = "build"
    description       = "Fat-jar containing natives for ALL platforms."
    archiveClassifier = "all-platforms"

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Your compiled classes
    from(sourceSets.main.get().output)

    // Platform-independent LWJGL jars
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })

    // ALL native jars for every platform
    from({
        allNatives
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
}

// ── Per-platform slim jars (optional) ────────────────────────────────────────
// Creates one jar per platform — smaller than the all-platforms jar
lwjglNativesList.forEach { natives ->
    val taskName = "fatJar-${natives}"

    val nativeConfig = configurations.create("natives-$natives")

    dependencies {
        nativeConfig(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
        nativeConfig("org.lwjgl:lwjgl::$natives")
        nativeConfig("org.lwjgl:lwjgl-sdl::$natives")
        nativeConfig("org.lwjgl:lwjgl-opengl::$natives")
    }

    tasks.register<Jar>(taskName) {
        group             = "build"
        description       = "Fat-jar for $natives only."
        archiveClassifier = natives

        manifest {
            attributes["Main-Class"] = application.mainClass.get()
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(sourceSets.main.get().output)

        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith(".jar") }
                .map { zipTree(it) }
        })

        from({
            nativeConfig
                .filter { it.name.endsWith(".jar") }
                .map { zipTree(it) }
        })
    }
}


// ── Convenience task — builds ALL per-platform jars at once ──────────────────
tasks.register("fatJarAllSeparate") {
    group       = "build"
    description = "Builds one fat-jar per platform."
    dependsOn(lwjglNativesList.map { "fatJar-$it" })
}


tasks.test {
    useJUnitPlatform()
}