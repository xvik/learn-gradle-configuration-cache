package ru.vyarus.gradle.plugin.fails.fail3;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author Vyacheslav Rusakov
 * @since 05.09.2025
 */
public class Fail3Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Fail3Extension ext = project.getExtensions().create("fail3", Fail3Extension.class, project);

        project.getTasks().register("fail3Task", task ->  {
            task.doLast(task1 ->
                    System.out.println("[run] Message: " + ext.message));
        });
    }
}
