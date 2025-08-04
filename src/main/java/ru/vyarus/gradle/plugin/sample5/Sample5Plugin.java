package ru.vyarus.gradle.plugin.sample5;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Demonstrates that task could reference project in constructor.
 *
 * @author Vyacheslav Rusakov
 * @since 04.08.2025
 */
public abstract class Sample5Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
          project.getTasks().register("sample5Task", Sample5Task.class);
    }
}
