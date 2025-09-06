package ru.vyarus.gradle.plugin.fails.fail2.fix;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

/**
 * @author Vyacheslav Rusakov
 * @since 05.09.2025
 */
public abstract class Fail2FixPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        final String projectName = project.getName();
        project.getTasks().register("fail2Fix", task ->  {
            task.doLast(task1 ->
                    System.out.println("[run] Project name: " + projectName));
        });

        final Provider<String> nameProvider = project.provider(() -> {
            System.out.println("[configuration] Provider called");
            return project.getName();
        });
        project.getTasks().register("fail2Fix2", task ->  {
            task.doLast(task1 ->
                    System.out.println("[run] Project name: " + nameProvider.get()));
        });
    }
}
