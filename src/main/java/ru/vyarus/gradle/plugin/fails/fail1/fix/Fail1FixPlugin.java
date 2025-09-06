package ru.vyarus.gradle.plugin.fails.fail1.fix;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import ru.vyarus.gradle.plugin.fails.fail1.Fail1Task;

/**
 * @author Vyacheslav Rusakov
 * @since 05.09.2025
 */
public abstract class Fail1FixPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("fail1Fix", Fail1FixTask.class);
        project.getTasks().register("fail1Fix2", Fail1Fix2Task.class);
    }
}
