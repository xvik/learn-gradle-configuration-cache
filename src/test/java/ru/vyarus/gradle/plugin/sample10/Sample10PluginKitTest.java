package ru.vyarus.gradle.plugin.sample10;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 19.11.2025
 */
public class Sample10PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample10'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("task1", "task2", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains(
                "Service configured: []",
                "Store task descriptor: task1",
                "Store task descriptor: task2",
                "Service created with state: [task1, task2]",
                "Task task1 doLast",
                "Task :task1 listened, but ignored",
                "Task task2 doLast",
                "Task :task2 listened, but ignored");

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("task1", "task2", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains(
                "Service created with state: [task1, task2]",
                "Task task1 doLast",
                "Task :task1 listened, but ignored",
                "Task task2 doLast",
                "Task :task2 listened, but ignored");
    }
}
