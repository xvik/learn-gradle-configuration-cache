package ru.vyarus.gradle.plugin.sample9;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin shows configuration time cache storage in service parameters for multi-module project and singleton
 * service.
 *
 * @author Vyacheslav Rusakov
 * @since 11.08.2025
 */
public class Sample9Plugin implements Plugin<Project> {

    // shared values (same instance for all plugins)
    static List<String> sharedValues = new ArrayList<>();

    @Override
    public void apply(Project project) {

        // service will store shared values in parameters
        final Provider<Service> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", Service.class, spec ->
                        spec.getParameters().getValues().set(sharedValues));

        System.out.println("Value added: " + project.getName());
        sharedValues.add(project.getName());

        project.getTasks().register("sample9Task", task -> {
            task.doLast(task1 ->
                    // service initialization (at this time parameters would be stored)
                    System.out.println("sharedState: " + service.get().getParameters().getValues().get()));
        });
    }
}
