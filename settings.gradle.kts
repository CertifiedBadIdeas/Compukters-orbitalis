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

val compukters = file(providers.gradleProperty("compuktersWorkspace").get())
val orbitalis = file(providers.gradleProperty("orbitalisCheckout").get())
val sdk = compukters.resolve("build/addon-sdk")
val metadata = Properties().apply { sdk.resolve("sdk.properties").inputStream().use(::load) }
gradle.extra["compuktersSdkDirectory"] = sdk.absolutePath
settings.pluginManagement.apply {
    repositories {
        maven { url = uri(sdk.resolve("repository")) }
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.4.10"
        id("dev.architectury.loom") version "1.17.491"
        id("ru.lazyhat.compukters.addon") version metadata.getProperty("sdkVersion")
    }
}
includeBuild(compukters) { name = "Compukters" }
includeBuild(orbitalis) { name = "Orbitalis" }
rootProject.name = "compukters-orbitalis"
