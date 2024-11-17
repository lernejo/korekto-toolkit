package com.github.lernejo.korekto.toolkit.thirdparty.git

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.nio.file.Paths

internal class ExerciseClonerTest {
    @Test
    @EnabledIfSystemProperty(named = "github_token", matches = ".+")
    internal fun sample_clone() {
        System.setProperty("github_user", "ledoyen")
        val ex = ExerciseCloner(Paths.get("target/repositories")).gitClone(
            "https://github.com/ledoyen/spring-todo-list"
        )
        println(ex)
    }
}
