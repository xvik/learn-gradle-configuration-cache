package ru.vyarus.gradle.plugin.fails.fail4.fix;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

/**
 * @author Vyacheslav Rusakov
 * @since 06.09.2025
 */
public class Fail4FixPlugin implements Plugin<Project> {

    private SourceSet sourceSet;

    @Override
    public void apply(Project project) {
        sourceSet = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("test");

        project.getTasks().register("fail4Fix", Fail4FixTask.class, task ->  {
            String set = sourceSet.getName();
            task.doLast(task1 ->
                    System.out.println("[run] Project source set: " + set));
        });
    }
}
