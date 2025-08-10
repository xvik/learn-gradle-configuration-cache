package ru.vyarus.gradle.plugin.sample6;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 08.08.2025
 */
public class Sample6PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample6'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("sample6Task", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains(
                "Added value 'val1': state=[val1]",
                "Added value 'val2': state=[val1, val2]",
                "Service 2 called Service 1 with state: state=[val1, val2]");
        Assertions.assertThat(out).contains("""
                Service 2 created
                Service 2 called Service 1 with state: state=[val1, val2]
                Direct state: [val1, val2]
                Direct state var: [val1, val2]
                Provider: [val1, val2]
                Service 1 closed
                Service 2 closed
                """);

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("sample6Task", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("""
                Service 2 created
                Service 1 created: state=[]
                Service 2 called Service 1 with state: state=[]
                Direct state: []
                Direct state var: [val1, val2]
                Provider: [val1, val2]
                Service 1 closed
                Service 2 closed
                """);
    }
}
