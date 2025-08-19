package ru.vyarus.gradle.plugin.sample9;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
public class Sample9PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'ru.vyarus.sample9' apply false
                }
                
                subprojects {
                    apply plugin: 'ru.vyarus.sample9'

                    repositories {
                        // required for testKit run
                        mavenCentral()
                    }
                }
                """);
        write(file("settings.gradle"), "include ':sub1', ':sub2'");
        // gradle would warn otherwise
        file("sub1").mkdirs();
        file("sub2").mkdirs();

        // WHEN run without cache
        BuildResult result = run("sample9Task", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("[sub1, sub2]");

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("sample9Task", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("[sub1, sub2]");
    }
}
