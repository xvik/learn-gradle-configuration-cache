package ru.vyarus.gradle.plugin.sample5;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2025
 */
public class Sample5PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample5'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("sample5Task", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("Task executed: " + projectName());

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("sample5Task", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("Task executed: " + projectName());
    }
}
