package ru.vyarus.gradle.plugin.sample2;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public class Sample2PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample2'
                }
                
                sample2 {
                    message = "Configured!"
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache enabled!
        BuildResult result = run("task1", "task2");

        // THEN same object instance everywhere
        String out = result.getOutput();
        Assertions.assertThat(out).contains("[Task 1]", "[Task 1, Task 2]");

        // WHEN run with cache enabled
        System.out.println("\n\n------------------- CACHE ENABLED ----------------------------------------");
        result = run("task1", "task2", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used, but objects are different!
        out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("[Task 1]", "[Task 2]");

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("task1", "task2", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("[Task 1]", "[Task 2]");
    }
}
