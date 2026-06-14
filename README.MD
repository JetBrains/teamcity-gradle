[![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

Gradle Runner
---

This repository was migrated from the svn http://svn.jetbrains.org/teamcity/plugins/gradle-runner
Branches before 2018.1.x are not migrated and can be found in the svn repository.

## Design overview

Most of the integration logic resides in the agent part: `gradle-runner-agent`.
The agent logic provides an init script to the Gradle execution. The script defines most of the behavior, including:

* Error handling and build problem service messages creation
* Test handling and test service messages creation
* Gradle project properties setup
* Incremental build setup
* Other runner-specific behavior

There are three init scripts. Each corresponds to a launch mode that can be set explicitly by a configuration property:

* `init.gradle`, corresponding to the `COMMAND_LINE` launch mode
* `init_since_8.gradle`, corresponding to the `TOOLING_API` launch mode
* `init_v2.gradle`, corresponding to the `COMMAND_LINE_V2` launch mode

The launch mode is selected as follows:

* If `teamcity.internal.gradle.runner.launch.mode` is set explicitly, its value is used:
  * `gradle` selects `COMMAND_LINE`
  * `gradle-tooling-api` selects `TOOLING_API`
  * `gradle_v2` selects `COMMAND_LINE_V2`
* Otherwise, `TOOLING_API` is selected automatically when Gradle 8.0+ is detected, configuration cache is enabled, configuration cache problems are not configured as `warn`, and no Tooling API-incompatible Gradle arguments are used.
* If none of the above applies, the runner falls back to `COMMAND_LINE`.

### Init scripts

`init.gradle` is the original init script, compatible with older Gradle versions.
As it is not compatible with Gradle Configuration Cache and could not be changed to support both older Gradle versions and Configuration Cache,
it was decided to introduce a new init script – `init_since_8.gradle`, compatible with Gradle versions 8+.
This init script heavily relies on the Gradle Tooling API for setup and execution of Gradle.
However, a number of issues surfaced with Tooling API:

* Worse build setup flow, portability, and transparency due to a different launch method that users do not normally use
* Incompatibility with some Gradle arguments
* More complicated usage and troubleshooting

Because of this, we decided to move back to non-Tooling API invocation of the Gradle process.
To support Configuration Cache and other newer Gradle features, a new init script was introduced – `init_v2.gradle`, compatible with Gradle versions 8.1+.

### `init_v2.gradle` init script

`init_v2.gradle` is the newest init script, compatible with Gradle 8.1+. Compared to `init.gradle`, it supports Configuration Cache and Isolated Projects.

Differences from `init.gradle`:

* Uses modern Gradle APIs instead of older build listener APIs which are not compatible with Configuration Cache. In particular, it uses build services and Gradle Flow APIs.
* Does not wrap every Gradle task in a single log block, as it is impossible to ensure these blocks are built correctly using CC-compatible APIs.
* Creates TeamCity build log blocks for test tasks. Test retry support messages are scoped to the test task log block.
* Supports Gradle isolated projects. Dependency-based incremental mode falls back to a full `build` because it needs to inspect all projects and source sets.

#### Gradle project properties
`init.gradle` has logic that adds properties read from the build properties file (`teamcity.build.parameters`) to the Gradle project's `ext` and `ext.teamcity`.
This is a poor design decision, as it modifies the Gradle project without the user's consent, and it is incompatible with modern Gradle practices, which encourage explicitly providing inputs.
However, we had to preserve this logic in `init_since_8.gradle` and `init_v2.gradle` for backwards compatibility.
This poses problems in Configuration Cache setups, as reading properties from the file during the configuration phase registers the file as a configuration phase input.
When the configuration cache is used, Gradle checks the file at the start of the build, and if the file metadata has changed since the previous build, the configuration cache is invalidated.
As `teamcity.build.parameters` is changed every build, this approach would always invalidate the configuration cache.

In the Tooling API-based logic (`init_since_8.gradle`), this was solved by creating a separate file named `teamcity.build.parameters.static` before Gradle was launched.
This file contains only properties deemed "static", meaning these properties do not change from build to build. Therefore, reading this file would not invalidate Configuration Cache.
However, this logic is brittle, as the heuristics that determine whether a property is "static" cannot be made precise.

In `init_v2.gradle`, this fragile logic is preserved for backwards compatibility reasons.
The creation of `teamcity.build.parameters.static` is skipped, as properties are read from `teamcity.build.parameters` in a way 
that does not register the file as a configuration cache input (see "Circumventing Gradle input detection logic").
The contract that `init_since_8.gradle` provided is preserved:

* All properties are added to the Gradle project when Configuration Cache is disabled.
* Only non-volatile ("static") properties are added to `project.ext` when Configuration Cache is enabled.
* A change to any of the non-volatile properties between builds will invalidate the Configuration Cache.

Additionally:

* All properties, including volatile ones ("dynamic" in `init_since_8.gradle` terminology), are available through `project.ext.teamcity`. Reading a changed property from `project.ext.teamcity` will invalidate the Configuration Cache.

#### Future plans for Gradle project properties
As this design is problematic and causes implementation issues, the best way to move forward would be to deprecate it.
A suggested deprecation path would be to stop adding properties to the Gradle project starting from a specific Gradle major version, 
as upgrading Gradle to a new major version often requires manual user intervention anyway.
Currently, it is possible to completely disable the population of the Gradle project with properties 
by setting the `teamcity.internal.gradle.runner.doNotPopulateGradleProperties` configuration parameter to `true`.

#### Circumventing Gradle input detection logic
Gradle uses "bytecode instrumentation" to track file access through common Java/Kotlin APIs.
This means that using most of the file access APIs will mark the file as a configuration input.
Gradle does not offer a public API to read a file without marking it as a configuration input.
However, non-instrumented APIs like RandomAccessFile can be used.
This relies on Gradle's internal implementation, but should be reasonably safe, as it is unlikely that Gradle will start tracking RandomAccessFile access in the future.

## Development

### Prerequisites

* JDK 8

Some integration tests use old versions of Gradle that are incompatible with Java 9 and later.
Therefore, to run and pass the tests successfully, it is necessary to use Java 8.
This can be done, for example, by explicitly specifying the version during the build:
```shell
JAVA_HOME=/Users/You/Library/Java/JavaVirtualMachines/corretto-1.8.0_372/Contents/Home ./gradlew clean build
```

### Building

1. Clone the repo
2. Build the project using Gradle
```shell
./gradlew clean build
```
Or, if you don’t want to run the tests:
```shell
./gradlew clean build -x test
```
The distribution will be located in the directory:
`<Path-to-the-project>/teamcity-gradle/gradle-runner-server/build/distributions/Gradle.zip`
