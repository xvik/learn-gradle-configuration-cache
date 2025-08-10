package ru.vyarus.gradle.plugin.sample8;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.vyarus.gradle.plugin.AbstractKitTest;

import java.io.File;

/**
 * @author Vyacheslav Rusakov
 * @since 10.08.2025
 */
public class Sample8PluginKitTest extends AbstractKitTest {
    @TempDir
    File cacheDir;

    @Test
    void testConfigurationCache() throws Exception {

        // build cache must not survive after the test (so use test-specific temp dir)
        write(file("settings.gradle"), String.format("""
                rootProject.name='sample8'
                buildCache {
                    local {
                        directory = new File("%s")
                    }
                }
                """, cacheDir.getCanonicalPath()));

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample8'
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("sample8Task", "--build-cache", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("""
                Task executed
                doLast for sample8Task
                Finish event: :sample8Task
                """);

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("sample8Task", "--build-cache", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        // task NOT EXECUED!
        Assertions.assertThat(result.task(":sample8Task").getOutcome()).isSameAs(TaskOutcome.UP_TO_DATE);
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("""
                > Task :sample8Task UP-TO-DATE
                Finish event: :sample8Task
                
                """);
    }
}
