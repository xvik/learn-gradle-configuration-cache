package ru.vyarus.gradle.plugin.fails.fail4;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 06.09.2025
 */
public class Fail4PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.fail4'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("fail4Task", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks: fail4Task",
                "Configuration cache entry stored with 2 problems.");
        Assertions.assertThat(out).contains("""
                > Task :fail4Task
                [run] Task source set: main
                [run] Project source set: test
                
                2 problems were found storing the configuration cache.
                - Task `:fail4Task` of type `ru.vyarus.gradle.plugin.fails.fail4.Fail4Task`: cannot deserialize object of type 'org.gradle.api.tasks.SourceSet' as these are not supported with the configuration cache.
                  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types
                - Task `:fail4Task` of type `ru.vyarus.gradle.plugin.fails.fail4.Fail4Task`: cannot serialize object of type 'org.gradle.api.internal.tasks.DefaultSourceSet', a subtype of 'org.gradle.api.tasks.SourceSet', as these are not supported with the configuration cache.
                  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types
                """);
    }
}
