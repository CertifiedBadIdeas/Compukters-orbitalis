/*
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.architectury.loom")
    id("ru.lazyhat.compukters.addon")
}
group = "org.cbi.development"
version = "1.2"
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
kotlin { jvmToolchain(21) }
val compukters = file(gradle.extra["compuktersCheckout"].toString())
val orbitalis = file(gradle.extra["orbitalisCheckout"].toString())
val sdk = File(gradle.extra["compuktersSdkDirectory"].toString())
val sdkVersion = Properties().apply { sdk.resolve("sdk.properties").inputStream().use(::load) }.getProperty("sdkVersion")
repositories {
    maven { url = uri(sdk.resolve("repository")) }
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
    maven("https://api.modrinth.com/maven")
}
apply(from = "../../gradle/sable-libraries.gradle.kts")
compuktersAddon { register("orbitalis", "1.2") }
configurations.named("compuktersAddonTooling") {
    withDependencies {
        clear()
        add(project.dependencies.create(files(compukters.resolve(
            "modules/common/compiler-k2-engine/build/devlibs/compukters-addon-tooling-development.jar",
        )).builtBy(gradle.includedBuild("Compukters").task(":compiler-k2-engine:developmentAddonToolingJar"))))
    }
}
configurations.named("compuktersAddonPlatform") {
    withDependencies {
        clear()
        add(project.dependencies.create(files(compukters.resolve("modules/common/guest-platform/build/platform/compukters-platform.cpb"))
            .builtBy(gradle.includedBuild("Compukters").task(":guest-platform:assemblePlatformBundle"))))
    }
}
dependencies {
    minecraft("net.minecraft:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    neoForge("net.neoforged:neoforge:21.1.252")
    modCompileOnly("maven.modrinth:sable:fg9dTRz9") { isTransitive = false }
    compileOnly(files(configurations.named("sableCompanion")))
    compileOnly("ru.lazyhat.compukters:compukters-addon-api:$sdkVersion")
    compileOnly("ru.lazyhat.compukters:compukters-addon-neoforge-1.21.1:$sdkVersion")
    compileOnly(files(orbitalis.resolve("modules/minecraft/v1_21_1/v1_21_1-neoforge/build/devlibs/orbitalis-development-mod.jar"))
        .builtBy(gradle.includedBuild("Orbitalis").task(":v1_21_1-neoforge:developmentModJar")))
}
tasks.register<Jar>("developmentModJar") {
    manifest.attributes["Fabric-Loom-Remap"] = "false"
    archiveFileName.set("compukters-orbitalis-development-mod.jar")
    destinationDirectory.set(layout.buildDirectory.dir("devlibs"))
    from(sourceSets.main.get().output)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(files(orbitalis.resolve("modules/minecraft/v1_21_1/v1_21_1-neoforge/build/devlibs/orbitalis-development-mod.jar")))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform() }
