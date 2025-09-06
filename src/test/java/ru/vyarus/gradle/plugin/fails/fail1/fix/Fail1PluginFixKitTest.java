package ru.vyarus.gradle.plugin.fails.fail1.fix;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 05.09.2025
 */
public class Fail1PluginFixKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.fail1fix'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("fail1Fix", "fail1Fix2", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput().replace(projectName(), "test-project");
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks: fail1Fix fail1Fix2",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).doesNotContain(
                "problem was found storing the configuration cache");
    }
}
