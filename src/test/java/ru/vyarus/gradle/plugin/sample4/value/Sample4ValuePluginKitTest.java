package ru.vyarus.gradle.plugin.sample4.value;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2025
 */
public class Sample4ValuePluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample4.value'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("task1", "-Dfoo=1", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("NonCacheableValue: 1");

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("task1", "-Dfoo=1", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("NonCacheableValue: 1");

        // WHEN property changed, but no invalidation!
        System.out.println("\n\n------------------- NO CACHE INVALIDATION ----------------------------------------");
        result = run("task1", "-Dfoo=2", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("NonCacheableValue: 2");
    }

}
