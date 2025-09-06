package ru.vyarus.gradle.plugin.fails.fail4.fix;

import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Vyacheslav Rusakov
 * @since 06.09.2025
 */
public class Fail4FixTask extends DefaultTask {

    private String name;

    public Fail4FixTask() {
        SourceSet sourceSet = getProject().getExtensions()
                .getByType(JavaPluginExtension.class).getSourceSets().getByName("main");
        name = sourceSet.getName();
    }

    @TaskAction
    public void run() {
        System.out.println("[run] Task source set: " + name);
    }
}
