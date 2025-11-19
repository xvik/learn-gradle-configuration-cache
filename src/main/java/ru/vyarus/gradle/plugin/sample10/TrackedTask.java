package ru.vyarus.gradle.plugin.sample10;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * Dummy task - just needed to attach to it.
 *
 * @author Vyacheslav Rusakov
 * @since 19.11.2025
 */
public abstract class TrackedTask extends DefaultTask {

    @TaskAction
    public void run() {

    }
}
