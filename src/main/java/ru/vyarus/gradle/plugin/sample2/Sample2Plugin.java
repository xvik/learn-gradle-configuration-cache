package ru.vyarus.gradle.plugin.sample2;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import ru.vyarus.gradle.plugin.sample1.Sample1Extension;

/**
 * Sample shows that you can't rely on objects uniqueness as the configuration cache would deserialize initially
 * the same object as different instances.
 *
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public abstract class Sample2Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final Sample1Extension ext = project.getExtensions().create("sample2", Sample1Extension.class);
        // some object, common for two tasks
        final SharedState state = new SharedState();
        state.direct = "Custom";
        System.out.println("[configuration] Initial shared object: " + state);

        // delayed configuration from extension
        project.afterEvaluate(p -> state.configTime = ext.message);

        project.getTasks().register("task1").configure(task ->
                task.doLast(task1 -> {
                    state.list.add("Task 1");
                    System.out.println("Task 1 shared object: " + state);
                }));

        project.getTasks().register("task2").configure(task ->
                task.doLast(task1 -> {
                    state.list.add("Task 2");
                    System.out.println("Task 2 shared object: " + state);
                }));
    }
}
