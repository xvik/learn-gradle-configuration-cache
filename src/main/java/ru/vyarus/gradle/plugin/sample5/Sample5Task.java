package ru.vyarus.gradle.plugin.sample5;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2025
 */
public abstract class Sample5Task extends DefaultTask {

    private final String projectName;

    public Sample5Task() {
        // this is configuration time! project access allowed!
        projectName = getProject().getName();
        System.out.println("[configuration] Task created");
    }

    @TaskAction
    public void run() {
        // project can't be accessed at runtime
        System.out.println("Task executed: " + projectName);
    }
}
