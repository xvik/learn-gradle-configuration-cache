package ru.vyarus.gradle.plugin.sample1;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Simple configuration cache demo (what code will not be executed again under configuration cache).
 *
 * @author Vyacheslav Rusakov
 * @since 02.08.2025
 */
public abstract class Sample1Plugin implements Plugin<Project> {

    private String pluginField;

    public Sample1Plugin() {
        System.out.println("[configuration] Plugin created");
    }

    @Override
    public void apply(Project project) {
        System.out.println("[configuration] Plugin applied");
        // register configuration extension
        final Sample1Extension ext = project.getExtensions().create("sample1", Sample1Extension.class);

        // register custom task
        project.getTasks().register("sample1Task", Sample1Task.class, task -> {
            task.getMessage().convention(ext.message);
            task.getMessage2().convention("Default");
            task.field = "assigned value";
            System.out.println("[configuration] Task configured. Ext message: " + ext.message);

            // the only line that works also under the configuration cache
            task.doFirst(task1 -> System.out.println("[run] Before task: " + ext.getMessage()
                    + ", plugin field: " + pluginField));
        });
        // task registered but not yet configured (user configuration also not yet applied)
        System.out.println("[configuration] Task registered. Ext message: " + ext.message);

        // afterEvaluate often used by plugins as the first point where user configuration applied
        project.afterEvaluate(p -> {
            pluginField = "assigned value";
            System.out.println("[configuration] Project evaluated. Ext message: " + ext.message);
        });

        // custom (lazy) task configuration
        project.getTasks().withType(Sample1Task.class).configureEach(task -> {
            System.out.println("[configuration] Task delayed configuration. Ext message: " + ext.message);
            task.getMessage2().set("Custom");
        });
    }
}
