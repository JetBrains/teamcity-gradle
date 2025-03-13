[![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

Gradle Runner
---

This repository was migrated from the svn http://svn.jetbrains.org/teamcity/plugins/gradle-runner
Branches before 2018.1.x are not migrated and can be found in the svn repository.

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
