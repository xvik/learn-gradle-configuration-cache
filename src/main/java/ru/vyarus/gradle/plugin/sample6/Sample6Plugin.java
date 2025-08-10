package ru.vyarus.gradle.plugin.sample6;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import javax.inject.Inject;
import java.util.List;

/**
 * Plugin shows different cache behavior for variables and direct service access. Also, it shows that service
 * does not preserve the state and service can call other services.
 *
 * @author Vyacheslav Rusakov
 * @since 08.08.2025
 */
public abstract class Sample6Plugin implements Plugin<Project> {

    @Inject
    public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    @Override
    public void apply(Project project) {
        // service 1 with "state" in a private field
        Provider<Service1> service1 = project.getGradle().getSharedServices()
                .registerIfAbsent("service1", Service1.class);
        // service 1 must not be closed; otherwise the stored (in it) configuration state would be lost
        getEventsListenerRegistry().onTaskCompletion(service1);

        // service 2 just with a link to service 1
        Provider<Service2> service2 = project.getGradle().getSharedServices().registerIfAbsent(
                "service2", Service2.class, spec -> {
                    // initial "persisted storage" value
                    spec.getParameters().getService1().set(service1);
                });
        // updating service state in configuration phase
        service1.get().addState("val1");

        project.getTasks().register("sample6Task", task -> {
            service1.get().addState("val2");
            // different state access at runtime: directly from service or with variable
            final List<String> state = service1.get().getState();
            Provider<List<String>> cachedState = project.provider(() -> service1.get().getState());
            // runtime: call service 2, which calls service 1 and prints state
            task.doLast(task1 -> {
                service2.get().doAction();
                System.out.println("Direct state: " + service1.get().getState());
                System.out.println("Direct state var: " + state);
                System.out.println("Provider: " + cachedState.get());
            });
        });
    }
}
