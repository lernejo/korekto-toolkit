# korekto-toolkit

Collection of utilities to grade IT exercises

![Build](https://github.com/lernejo/korekto-toolkit/workflows/Build/badge.svg)
[![JitPack](https://jitpack.io/v/lernejo/korekto-toolkit.svg)](https://jitpack.io/#lernejo/korekto-toolkit)
[![License](https://img.shields.io/github/license/lernejo/korekto-toolkit.svg)](https://opensource.org/licenses/Apache-2.0)

## Building a custom grading project

The toolkit supplies a generic launcher `com.github.lernejo.korekto.toolkit.launcher.GradingJobLauncher` relying on the
existence of a [Service Provider](https://docs.oracle.com/javase/9/docs/api/java/util/ServiceLoader.html)
of `com.github.lernejo.korekto.toolkit.Grader`.

For the generic launcher to pick up the grading service, make it implement the `Grader` interface and declare it in  
`META-INF/services/com.github.lernejo.korekto.toolkit.Grader`.

The generic launcher has 3 modes

* **container** the default mode, sends the results to the given `CALLBACK_URL`
* **demo** by specifying a user slug (`-s=mySlug`)
* **group** by specifying the group mode (`-g`) and supplying a slugs file

## Built-in steps

* `CloneStep` performs a Git clone of the **REPO_URL** parameter and provides `GradingContext#exercise`
* `SendStep` sends the `GradingContext#gradeDetails` to the **CALLBACK_URL** parameter, using **CALLBACK_PASSWORD**
  parameter if present
* `UpsertGitHubGradingIssues` opens 2 kinds of issues on GitHub reflecting the state of ongoing exercise or its
  completion (requires a GitHub token, either personal or app installation)

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
