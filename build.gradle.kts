import net.darkhax.curseforgegradle.TaskPublishCurseForge

plugins {
    idea
    `maven-publish`
    kotlin("jvm") version "2.2.10"
    id("net.minecraftforge.gradle") version "6+"
    id("org.spongepowered.mixin") version "0.7.38"
    id("com.modrinth.minotaur") version "2.+"
    id("net.darkhax.curseforgegradle") version "1.+"
}

val minecraftVersion: String by ext
val forgeVersion: String by ext
val modId: String by ext
val modName: String by ext
val modVersion: String by ext
val jeiVersion: String by ext

group = properties["group"].toString()
version = "$minecraftVersion-$modVersion"

java {
    withSourcesJar()
    base.archivesName = modName
}

kotlin {
    jvmToolchain(8)

    sourceSets.all {
        languageSettings {
            enableLanguageFeature("ContextParameters")
        }
    }
}

mixin {
    add(sourceSets.main.get(), "$modId.mixins.refmap.json")
    config("$modId.mixins.json")
}

minecraft {
    mappings("official", minecraftVersion)

    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        create("client") {
            workingDirectory(project.file("run/client"))

            property("forge.logging.console.level", "debug")

            property("mixin.env.remapRefMap", "true")

            accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

            mods {
                create(modId) {
                    source(sourceSets["main"])
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run/server"))

            property("forge.logging.console.level", "debug")

            property("mixin.env.remapRefMap", "true")

            accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

            mods {
                create(modId) {
                    source(sourceSets["main"])
                }
            }
        }
    }
}

repositories {
    mavenCentral()
    maven("https://files.minecraftforge.net/maven/")
    maven("https://api.modrinth.com/maven")
    maven("https://cursemaven.com")
}

dependencies {
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    implementation(fg.deobf("maven.modrinth:jei:$jeiVersion"))
    implementation(fg.deobf("maven.modrinth:architectury-api:1.32.68+forge"))
    implementation(fg.deobf("curse.maven:kubejs-238086:3647098"))
    implementation(fg.deobf("curse.maven:rhino-416294:3525704"))

    runtimeOnly(fg.deobf("maven.modrinth:enablemultiplayermode:1.0.0+Forge1.16.X"))
    runtimeOnly(fg.deobf("curse.maven:the-one-probe-245211:3752096"))
    runtimeOnly(fg.deobf("curse.maven:cyclops-core-232758:3900678"))
    runtimeOnly(fg.deobf("curse.maven:iconexporter-327048:3346632"))
    runtimeOnly(fg.deobf("maven.modrinth:mekanism:10.1.2.457"))
    runtimeOnly(fg.deobf("curse.maven:scalable-cats-force-320926:4059962"))
    runtimeOnly(fg.deobf("curse.maven:largefluidtank-291006:3838627"))
    runtimeOnly(fg.deobf("curse.maven:titanium-287342:5174863"))
    runtimeOnly(fg.deobf("curse.maven:industrial-foregoing-266515:5334823"))

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

tasks {
    withType<ProcessResources> {
        val remap = mapOf(
            "version" to project.version,
            "modId" to modId,
            "modName" to modName,
            "jeiVersion" to jeiVersion
        )

        inputs.properties(remap)

        filesMatching(setOf("META-INF/mods.toml", "pack.mcmeta")) {
            expand(remap)
        }
    }

    afterEvaluate {
        named("addMixinsToJar") {
            setDependsOn(listOf("compileJava", "compileKotlin"))
        }
    }

    jar {
        finalizedBy("reobfJar")
    }

    register<TaskPublishCurseForge>("curseforge") {
        group = "publishing"
        apiToken = project.properties["curseforge.token"].toString()

        upload(648536, jar) {
            releaseType = "release"
            changelog = readCurrentVersionChangelog()
            changelogType = "markdown"
            addGameVersion(minecraftVersion)
            addModLoader("forge")
            addOptional("kubejs", "jei")
        }
    }
}

modrinth {
    token = project.properties["modrinth.token"].toString()
    projectId = "fluid-cells"
    versionNumber = version.toString()
    versionType = "release"
    changelog = readCurrentVersionChangelog()
    uploadFile.set(tasks.jar)
    gameVersions.add(minecraftVersion)
    loaders.add("forge")
}

fun readCurrentVersionChangelog(): String {
    val changelogText = file("CHANGELOG.md").readText()

    val versionStartIndex = changelogText.indexOf("\n## [$version]")

    if (versionStartIndex == -1) {
        error("CHANGELOG.md does not contain version [$version]")
    }

    val versionEndIndex = run {
        val index = changelogText.indexOf("\n## ", startIndex = versionStartIndex+2)
        if (index != -1) index else changelogText.length
    }

    val resultVersionChangelog = changelogText.substring(versionStartIndex, versionEndIndex)

    return resultVersionChangelog
}