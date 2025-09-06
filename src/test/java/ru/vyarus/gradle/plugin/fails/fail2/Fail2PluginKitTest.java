package ru.vyarus.gradle.plugin.fails.fail2;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 04.09.2025
 */
public class Fail2PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.fail2'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = runFailed("fail2Task", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput().replace(projectName(), "test-project");
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks: fail2Task");
        Assertions.assertThat(out).contains("""
                > Configure project :
                [configuration] Project name: test-project
                
                > Task :fail2Task FAILED

                2 problems were found storing the configuration cache.
                - Task `:fail2Task` of type `org.gradle.api.DefaultTask`: cannot deserialize object of type 'org.gradle.api.Project' as these are not supported with the configuration cache.
                  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types
                - Task `:fail2Task` of type `org.gradle.api.DefaultTask`: cannot serialize object of type 'org.gradle.api.internal.project.DefaultProject', a subtype of 'org.gradle.api.Project', as these are not supported with the configuration cache.
                  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types
                """);

        Assertions.assertThat(out).contains("""
                > Cannot invoke "org.gradle.api.Project.getName()" because "project" is null
                """);

    }
}
