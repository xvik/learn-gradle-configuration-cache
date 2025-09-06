package ru.vyarus.gradle.plugin.fails.fail1;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 05.09.2025
 */
public class Fail1PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.fail1'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("fail1Task", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput().replace(projectName(), "test-project");
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks: fail1Task",
                "Configuration cache entry stored with 1 problem.");
        Assertions.assertThat(out).contains("""
                > Task :fail1Task
                [run] Task executed for project: test-project

                1 problem was found storing the configuration cache.
                - Task `:fail1Task` of type `ru.vyarus.gradle.plugin.fails.fail1.Fail1Task`: invocation of 'Task.project' at execution time is unsupported.
                  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution
                """);
    }
}
