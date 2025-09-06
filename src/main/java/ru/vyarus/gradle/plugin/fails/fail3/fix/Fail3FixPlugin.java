package ru.vyarus.gradle.plugin.fails.fail3.fix;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import ru.vyarus.gradle.plugin.fails.fail3.Fail3Extension;

/**
 * @author Vyacheslav Rusakov
 * @since 06.09.2025
 */
public class Fail3FixPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Fail3Extension ext = project.getExtensions().create("fail3", Fail3Extension.class, project);

        project.getTasks().register("fail3Task", task ->  {
            String message = ext.message;
            task.doLast(task1 ->
                    System.out.println("[run] Message: " + message));
        });
    }
}
