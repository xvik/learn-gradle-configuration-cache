package ru.vyarus.gradle.plugin.sample1;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import ru.vyarus.gradle.plugin.AbstractKitTest;

/**
 * @author Vyacheslav Rusakov
 * @since 02.08.2025
 */
public class Sample1PluginKitTest extends AbstractKitTest {

    @Test
    void testConfigurationCache() {

        // SETUP
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample1'
                }
                
                sample1 {
                    message = "Configured!"
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);

        // WHEN run without cache
        BuildResult result = run("sample1Task", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        String out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as no cached configuration is available for tasks:",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("""
                > Configure project :
                [configuration] Plugin created
                [configuration] Plugin applied
                [configuration] Extension created
                [configuration] Task registered. Ext message: Default
                [configuration] Project evaluated. Ext message: Configured!
                [configuration] Task created
                [configuration] Task configured. Ext message: Configured!
                [configuration] Task delayed configuration. Ext message: Configured!

                > Task :sample1Task
                Extension get message: Configured!
                [run] Before task: Configured!, plugin field: assigned value
                [run] Task executed: message=Configured!, message2=Custom, public field=assigned value, private field=set
                """);

        // WHEN run with populated cache
        System.out.println("\n\n------------------- FROM CACHE ----------------------------------------");
        result = run("sample1Task", "--configuration-cache", "--configuration-cache-problems=warn");
        out = result.getOutput();

        // THEN cache used
        Assertions.assertThat(out).contains(
                "Reusing configuration cache.");
        Assertions.assertThat(out).contains("""
                > Task :sample1Task
                Extension get message: Configured!
                [run] Before task: Configured!, plugin field: assigned value
                [run] Task executed: message=Configured!, message2=Custom, public field=assigned value, private field=set
                """);

        // WHEN configuration changed
        System.out.println("\n\n------------------- INVALIDATE CACHE ----------------------------------------");
        build("""
                plugins {
                    id 'java'
                    id 'ru.vyarus.sample1'
                }
                
                sample1 {
                    message = "Changed!"
                }
                
                repositories {
                    // required for testKit run
                    mavenCentral()
                }
                """);
        result = run("sample1Task", "--configuration-cache", "--configuration-cache-problems=warn");

        // THEN cache not used
        out = result.getOutput();
        Assertions.assertThat(out).contains(
                "Calculating task graph as configuration cache cannot be reused because file 'build.gradle' has changed.",
                "Configuration cache entry stored.");
        Assertions.assertThat(out).contains("""
                > Configure project :
                [configuration] Plugin created
                [configuration] Plugin applied
                [configuration] Extension created
                [configuration] Task registered. Ext message: Default
                [configuration] Project evaluated. Ext message: Changed!
                [configuration] Task created
                [configuration] Task configured. Ext message: Changed!
                [configuration] Task delayed configuration. Ext message: Changed!

                > Task :sample1Task
                Extension get message: Changed!
                [run] Before task: Changed!, plugin field: assigned value
                [run] Task executed: message=Changed!, message2=Custom, public field=assigned value, private field=set
                """);

    }
}
