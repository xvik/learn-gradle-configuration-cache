package ru.vyarus.gradle.plugin.fails.fail4.fix;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 06.09.2025
 */
public class Fail4PluginFixKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.fail4fix'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("fail4Fix", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput().replace(projectName(), "test-project");
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks: fail4Fix",
                """
                        > Task :fail4Fix
                        [run] Task source set: main
                        [run] Project source set: test
                        """,
                "Configuration cache entry stored.");
        Assertions.assertThat(out).doesNotContain(
                "problem was found storing the configuration cache");
    }
}
