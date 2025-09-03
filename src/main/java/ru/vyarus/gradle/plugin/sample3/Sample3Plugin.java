package ru.vyarus.gradle.plugin.sample3;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

/**
 * Shows build service used for tasks communication.
 *
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public abstract class Sample3Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final Sample3Extension ext = project.getExtensions().create("sample3", Sample3Extension.class);
        // IMPORTANT: service not created at this moment! It's just a provider
        // It is also important to not resolve it too early because parameters might be initialized with defaults
        // (user-defined configuration might not be applied to extension yet)
        final Provider<SharedService> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", SharedService.class, spec -> {
                    // configuration value set with parameter
                    spec.getParameters().getExtParam().convention(project.provider(() -> ext.message));
                });

        // configuration value set DIRECTLY (to show difference)
        project.afterEvaluate(p -> {
            service.get().extParam = ext.message;
            System.out.println("[configuration] Project evaluated. Direct assigning: " + ext.message + " to service " + service.get() + ")");
        });

        project.getTasks().register("task1").configure(task ->
                task.doLast(task1 -> {
                    final SharedService sharedService = service.get();
                    sharedService.list.add("Task 1");
                    System.out.println("[run] Task 1 shared object: " + sharedService);
                }));

        project.getTasks().register("task2").configure(task -> {
            // For predictable execution sequence (simpler to validate in test).
            // Without it, tasks will run concurrently!
            task.mustRunAfter("task1");
            
            task.doLast(task1 -> {
                final SharedService sharedService = service.get();
                sharedService.list.add("Task 2");
                System.out.println("[run] Task 2 shared object: " + sharedService);
            });
        });
    }
}
