package ru.vyarus.gradle.plugin.sample8;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import javax.inject.Inject;

/**
 * Shows that doFirst/doLast not called under build cache, but service is still notified.
 *
 * @author Vyacheslav Rusakov
 * @since 10.08.2025
 */
public abstract class Sample8Plugin implements Plugin<Project> {

    @Inject
    public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    @Override
    public void apply(Project project) {
        // service listen for tasks
        final Provider<Service> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", Service.class);
        getEventsListenerRegistry().onTaskCompletion(service);

        project.getTasks().register("sample8Task", Sample8Task.class, task -> {
            task.getOut().set(project.getLayout().getBuildDirectory().dir("sample8/out.txt").get().getAsFile());
            task.doLast(t -> System.out.println("doLast for sample8Task"));
        });
    }
}
