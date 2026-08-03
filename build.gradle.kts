plugins {
    java
    id("com.gradleup.shadow") version("8.3.5")
    id("com.github.ben-manes.versions") version("0.51.0")
}

// Change to true when releasing
val release = false
val majorVersion = "1.14.2"
val minorVersion = if (release) "Release" else "DEV-" + System.getenv("BUILD_NUMBER")

group = "com.extendedclip"
version = "$majorVersion-$minorVersion"

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.glaremasters.me/repository/public/")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.nexomc.com/releases/")
    maven("https://repo.oraxen.com/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.spigot)

    compileOnly(libs.vault)
    compileOnly(libs.authlib)

    compileOnly(libs.headdb)
    compileOnly(libs.craftengine.core)
    compileOnly(libs.craftengine.bukkit)
    compileOnly(libs.itemsadder)
    compileOnly(libs.nexo)
    compileOnly(libs.oraxen)
    compileOnly(libs.mythiclib)
    compileOnly(libs.mmoitems)
    compileOnly(libs.score)
    compileOnly(libs.sig)

    compileOnly(libs.papi)

    implementation(libs.nashorn)
    implementation(libs.adventure.platform)
    implementation(libs.adventure.minimessage)
    implementation(libs.bstats)

    compileOnly("org.jetbrains:annotations:23.0.0")
}

tasks {
    shadowJar {
        relocate("org.objectweb.asm", "com.extendedclip.deluxemenus.libs.asm")
        relocate("org.openjdk.nashorn", "com.extendedclip.deluxemenus.libs.nashorn")
        // net.kyori (Adventure) is intentionally NOT relocated: ItemMeta#displayName(Component)/
        // lore(List<Component>) (needed to render MiniMessage object tags like <sprite:...> in item
        // tooltips) require the exact same Component class Paper's own API expects. A relocated copy
        // would be a different class to the JVM and Paper's native item meta methods would reject it.
        relocate("org.bstats", "com.extendedclip.deluxemenus.libs.bstats")
        archiveFileName.set("DeluxeMenus-${rootProject.version}.jar")
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        disableAutoTargetJvm()
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to rootProject.version)
        }
    }

    // shadowJar and the plain jar task both write to build/libs/DeluxeMenus-<version>.jar; make sure
    // the shaded jar (which bundles bstats/Adventure/nashorn) is what's left behind after a build.
    assemble {
        dependsOn(shadowJar)
    }
}
