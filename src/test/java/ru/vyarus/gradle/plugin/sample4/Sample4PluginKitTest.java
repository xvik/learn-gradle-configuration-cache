package ru.vyarus.gradle.plugin.sample4;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public class Sample4PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample4'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN simple run
        BuildResult result = run("task1", "-PstartTime=1");

        // THEN provider called at runtime
        String out = result.getOutput();
        Assertions.assertThat(out).contains("""
                        > Task :task1
                        called computeMessage('static')
                        Task exec / static value: Computed message: static
                        Provider called: 1
                        called computeMessage('provider 1')
                        Task exec / provider value: Computed message: provider 1
                        """);

        // WHEN run without cache
        System.out.println("\n\n------------------- CACHE ENABLED ----------------------------------------");
        result = run("task1", "-PstartTime=1", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("Provider called: 1");

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("task1", "-PstartTime=1", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("called computeMessage('provider 1')");
        Assertions.assertThat(out).doesNotContain("Provider called: 1");

        // WHEN property changed
        System.out.println("\n\n------------------- CACHE INVALIDATION ----------------------------------------");
        result = run("task1", "-PstartTime=2", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Calculating task graph as configuration cache cannot be reused because the set of Gradle properties has changed: the value of 'startTime' was changed.");
        Assertions.assertThat(out).contains("Provider called: 2");
    }


    // same as the test above, but property declared directly in the build script
    @Test
    void testConfigurationCacheWithBuildProperty() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample4'
                }
                
                ext.startTime = 1
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("task1", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("Provider called: 1");

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("task1", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("called computeMessage('provider 1')");
        Assertions.assertThat(out).doesNotContain("Provider called: 1");

        // WHEN property changed
        System.out.println("\n\n------------------- CACHE INVALIDATION ----------------------------------------");
        // change property in the script
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample4'
                }
                
                ext.startTime = 2
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);
        result = run("task1", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Calculating task graph as configuration cache cannot be reused because file 'build.gradle' has changed.");
        Assertions.assertThat(out).contains("Provider called: 2");
    }
}
