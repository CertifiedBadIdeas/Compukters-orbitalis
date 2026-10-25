# Compukters: Orbitalis

Minecraft 1.21.1 / NeoForge addon connecting Compukters Guest programs to Orbitalis celestial
observations, bounded engines-off forecasts and native ship-transfer lifecycle APIs.
The Guest program owns guidance and engine commands; this addon provides no autopilot.

The mod ID is `compukters_orbitalis`; the existing Orbitalis Guest API remains version 1.2.
Required mods: Compukters, Orbitalis and Sable 2.0.6. Propulsion is needed only by Guest programs
that use its separate thruster API. This repository does not depend on a consumer or development stand.

## Source development

The normal checkout is `Compukters/addons/orbitalis`, with an Orbitalis source checkout beside Compukters:

```text
workspace/
├── Orbitalis/
└── Compukters/
    └── addons/orbitalis/
```

Run Gradle with JDK 25 and install a Java 21 toolchain. Prepare the SDK in the selected Compukters
checkout first, then import this repository into the IDE or build it without additional properties:

```sh
../../gradlew stageAddonDevelopmentDependencies
./gradlew check developmentModJar
```

`gradle.properties` supplies `compuktersWorkspace=../..` and
`orbitalisCheckout=../../../Orbitalis`. Override these paths for another layout:

```sh
./gradlew check -PcompuktersWorkspace=/path/to/Compukters -PorbitalisCheckout=/path/to/Orbitalis
```

The build compiles against those local sources and the prepared local SDK. It does not launch
Minecraft as part of `check`. The stable development archive is
`build/devlibs/compukters-orbitalis-development-mod.jar`; independent client compositions may consume it.

This repository preserves the original addon's extracted history from Orbitalis-Compukters-Dev.
