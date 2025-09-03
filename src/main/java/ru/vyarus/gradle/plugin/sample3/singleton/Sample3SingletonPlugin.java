package ru.vyarus.gradle.plugin.sample3.singleton;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;
import ru.vyarus.gradle.plugin.sample3.Sample3Extension;

import javax.inject.Inject;

/**
 * Same as the original plugin, but service is also a task listener, which prevents gradle from closing it early.
 *
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public abstract class Sample3SingletonPlugin implements Plugin<Project> {

    @Inject
    public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    @Override
    public void apply(Project project) {
        final Sample3Extension ext = project.getExtensions().create("sample3", Sample3Extension.class);
        // IMPORTANT: service not created at this moment! It's just a provider
        // It is also important to not resolve it too early because parameters might be initialized with defaults
        // (user-defined configuration might not be applied to extension yet)
        final Provider<SharedServiceSingleton> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", SharedServiceSingleton.class, spec -> {
                    // configuration value set with parameter
                    spec.getParameters().getExtParam().convention(project.provider(() -> ext.message));
                });
        // service listens for tasks completion, which prevents gradle from stopping it in the middle of the build
        getEventsListenerRegistry().onTaskCompletion(service);

        // configuration value set DIRECTLY (to show difference)
        project.afterEvaluate(p -> {
            service.get().extParam = ext.message;
            System.out.println("[configuration] Project evaluated. Direct assigning: " + ext.message + " to service " + service.get() + ")");
        });

        project.getTasks().register("task1").configure(task ->
                task.doLast(task1 -> {
                    final SharedServiceSingleton sharedService = service.get();
                    sharedService.list.add("Task 1");
                    System.out.println("[run] Task 1 shared object: " + sharedService);
                }));

        project.getTasks().register("task2").configure(task -> {
            // For predictable execution sequence (simpler to validate in test).
            // Without it, tasks will run concurrently!
            task.mustRunAfter("task1");

            task.doLast(task1 -> {
                final SharedServiceSingleton sharedService = service.get();
                sharedService.list.add("Task 2");
                System.out.println("[run] Task 2 shared object: " + sharedService);
            });
        });
    }
}
