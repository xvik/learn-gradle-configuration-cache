package ru.vyarus.gradle.plugin.fails.fail1;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Demonstrates that task could reference project in constructor.
 *
 * @author Vyacheslav Rusakov
 * @since 04.09.2025
 */
public abstract class Fail1Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
          project.getTasks().register("fail1Task", Fail1Task.class);
    }
}
