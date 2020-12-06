# korekto-toolkit
Collection of utilities to grade IT exercises

![Build](https://github.com/lernejo/korekto-toolkit/workflows/Build/badge.svg)
[![JitPack](https://jitpack.io/v/lernejo/korekto-toolkit.svg)](https://jitpack.io/#lernejo/korekto-toolkit)
[![License](https://img.shields.io/github/license/lernejo/korekto-toolkit.svg)](https://opensource.org/licenses/Apache-2.0)

## Building a custom grading project

```java
public static void main(String[] args) {
    int exitCode = new GradingJob()
        .addCloneStep()
        .addStep("grading", (configuration, context) -> customGradingStep(configuration, context))
        .addSendStep()
        .run();
    System.exit(exitCode);
}
```

## Built-in steps
* `CloneStep` performs a Git clone of the **REPO_URL** parameter and provides `GradingContext#exercise`
* `SendStep` sends the `GradingContext#gradeDetails` to the **CALLBACK_URL** parameter, using **CALLBACK_PASSWORD** parameter if present

Parameters are read from the environment (`System#getenv`).

## Use it with maven
```xml
<dependencies>
    ...
    <dependency>
        <groupId>com.github.lernejo</groupId>
        <artifactId>korekto-toolkit</artifactId>
        <version>${korekto-toolkit.version}</version>
    </dependency>
    ...
</dependencies>

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
