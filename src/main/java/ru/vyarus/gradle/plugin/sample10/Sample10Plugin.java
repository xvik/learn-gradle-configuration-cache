package ru.vyarus.gradle.plugin.sample10;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Plugin demonstrates tasks state collection and use it in build service task listener.
 *
 * @author Vyacheslav Rusakov
 * @since 19.11.2025
 */
public abstract class Sample10Plugin implements Plugin<Project> {

    @Inject
    public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    // collecting tasks info during configuration phase
    private final List<TaskDesc> tasksInfo = new CopyOnWriteArrayList<>();

    @Override
    public void apply(Project project) {
        Provider<Service> service = project.getGradle().getSharedServices()
                .registerIfAbsent("service", Service.class, spec -> {
                    System.out.println("Service configured: " + tasksInfo);
                    spec.getParameters().getValues().value(tasksInfo);
                });

        getEventsListenerRegistry().onTaskCompletion(service);

        // register multiple tasks
        project.getTasks().register("task1", TrackedTask.class);
        project.getTasks().register("task2", TrackedTask.class);

        // capture information about tasks using lazy block - we will need only actually executed tasks
        project.getTasks().withType(TrackedTask.class).configureEach(task -> {
                captureTaskInfo(task);
                task.doLast(task1 -> {
                    final TaskDesc desc = service.get().getParameters().getValues().get().stream()
                            .filter(taskDesc -> taskDesc.getPath().equals(task.getPath()))
                            .findAny().orElse(null);
                    if (!desc.isCalled()) {
                        System.out.println("Task " + task1.getName() + " doLast");
                        desc.setCalled(true);
                    }
                });
        });
    }

    private void captureTaskInfo(Task task) {
        System.out.println("Store task descriptor: " + task.getName());
        tasksInfo.add(new TaskDesc(task.getName(), task.getPath()));
    }
}
