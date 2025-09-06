package ru.vyarus.gradle.plugin.fails.fail4;

import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Vyacheslav Rusakov
 * @since 06.09.2025
 */
public abstract class Fail4Task extends DefaultTask {

    private SourceSet sourceSet;
    private String name;

    public Fail4Task() {
        sourceSet = getProject().getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main");
        name = sourceSet.getName();
    }

    @TaskAction
    public void run() {
        System.out.println("[run] Task source set: " + name);
    }
}
