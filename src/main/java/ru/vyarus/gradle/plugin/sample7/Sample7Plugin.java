package ru.vyarus.gradle.plugin.sample7;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin shows how to preserve build service state using parameter. The key moment is to delay service initialization
 * until the state would be prepared.
 *
 * @author Vyacheslav Rusakov
 * @since 10.08.2025
 */
public class Sample7Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final List<String> values = new ArrayList<>();

        Provider<Service> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", Service.class, spec -> {
                    System.out.println("[configuration] Creating service");
                    // initial "persisted storage" value
                    spec.getParameters().getValues().value(values);
                });

        values.add("val1");
        values.add("val2");

        project.getTasks().register("sample7Task", task ->
                task.doFirst(task1 ->
                        System.out.println("Task see state: " + service.get().getParameters().getValues().get()))
        );
    }
}
