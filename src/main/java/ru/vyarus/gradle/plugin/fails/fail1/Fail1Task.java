package ru.vyarus.gradle.plugin.fails.fail1;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Vyacheslav Rusakov
 * @since 04.09.2025
 */
public abstract class Fail1Task extends DefaultTask {

    @TaskAction
    public void run() {
        System.out.println("[run] Task executed for project: " + getProject().getName());
    }
}
