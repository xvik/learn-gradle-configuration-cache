package ru.vyarus.gradle.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 02.08.2025
 */
public abstract class AbstractKitTest {

    @TempDir
    File projectDir;
    File buildFile;
    boolean debug;

    @BeforeEach
    void setUp() {
        buildFile = file("build.gradle");
    }

    public String projectName() {
        return projectDir.getName();
    }

    public File file(String path) {
        return new File(projectDir, path);
    }

    public void build(String content) {
        write(buildFile, content);
    }

    public void write(File file, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.append(content);
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to write into file: " + file.getPath(), e);
        }
    }

    /**
     * Enable it and run test with debugger (no manual attach required). Not always enabled to speed up tests during
     * normal execution.
     */
    public void debug() {
        debug = true;
    }

    public GradleRunner gradle(File root, String... commands) {
        List<String> cmdLine = new ArrayList<>(List.of(commands));
        // add stacktrace to all commands to better see errors
        cmdLine.add("--stacktrace");

        return GradleRunner.create()
                .withProjectDir(root)
                .withArguments(cmdLine)
                .withPluginClasspath()
                .withDebug(debug)
                .forwardOutput();
    }

    public GradleRunner gradle(String... commands) {
        return gradle(projectDir, commands);
    }

    public BuildResult run(String... commands) {
        return gradle(commands).build();
    }

    public BuildResult runFailed(String... commands) {
        return gradle(commands).buildAndFail();
    }

    public BuildResult runVer(String gradleVersion, String... commands) {
        return gradle(commands).withGradleVersion(gradleVersion).build();
    }

    public BuildResult runFailedVer(String gradleVersion, String... commands) {
        return gradle(commands).withGradleVersion(gradleVersion).buildAndFail();
    }
}
