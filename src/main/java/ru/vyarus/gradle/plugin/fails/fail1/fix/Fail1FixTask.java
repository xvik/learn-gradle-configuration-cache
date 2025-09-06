package ru.vyarus.gradle.plugin.fails.fail1.fix;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Vyacheslav Rusakov
 * @since 05.09.2025
 */
public abstract class Fail1FixTask extends DefaultTask {

    private final String projectName;

    public Fail1FixTask() {
        projectName = getProject().getName();
    }

    @TaskAction
    public void run() {
        System.out.println("[run] Task executed for project: " + projectName);
    }
}
