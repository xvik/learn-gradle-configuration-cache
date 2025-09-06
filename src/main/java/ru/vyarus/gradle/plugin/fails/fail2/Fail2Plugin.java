package ru.vyarus.gradle.plugin.fails.fail2;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * The simplest fail - using project at runtime.
 *
 * @author Vyacheslav Rusakov
 * @since 04.09.2025
 */
public abstract class Fail2Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("[configuration] Project name: " + project.getName());
        
        project.getTasks().register("fail2Task", task ->  {
            task.doLast(task1 ->
                    System.out.println("[run] Project name: " + project.getName()));
        });
    }
}
