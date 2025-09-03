package ru.vyarus.gradle.plugin.sample4;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

/**
 * Plugin shows that providers are cached, but react on property changes. Direct method calls preserved.
 *
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public class Sample4Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("task1").configure(task -> {
            Provider<String> provider = project.provider(() -> {
                String res = String.valueOf(project.findProperty("startTime"));
                System.out.println("Provider called: " + res);
                return res;
            });
            task.doLast(task1 -> {
                System.out.println("Task exec / static value: " + computeMessage("static"));
                System.out.println("Task exec / provider value: " + computeMessage("provider " + provider.get()));
            });
        });
    }

    private String computeMessage(String source) {
        System.out.println("called computeMessage('" + source + "')");
        return "Computed message: " + source;
    }
}
