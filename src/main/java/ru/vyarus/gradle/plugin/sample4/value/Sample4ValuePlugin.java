package ru.vyarus.gradle.plugin.sample4.value;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

/**
 * Shows the difference of Provider and ValueSource.
 *
 * @author Vyacheslav Rusakov
 * @since 04.08.2025
 */
public class Sample4ValuePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("task1").configure(task -> {
            final Provider<String> provider = project.getProviders()
                    .of(NonCacheableValue.class, noneValueSourceSpec -> {});
            task.doLast(task1 -> {
                System.out.println("Task exec / static value: " + computeMessage("static " + System.getProperty("foo")));
                System.out.println("Task exec / provider value: " + computeMessage("provider " + provider.get()));
            });
        });
    }

    private String computeMessage(String source) {
        System.out.println("called computeMessage('" + source + "')");
        return "Computed message: " + source;
    }
}
