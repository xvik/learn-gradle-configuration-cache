package ru.vyarus.gradle.plugin.fails.fail3;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.util.Set;

/**
 * @author Vyacheslav Rusakov
 * @since 05.09.2025
 */
public class Fail3Extension {

    public Set<SourceSet> sets;
    public String message;

    public Fail3Extension(Project project) {
        this.sets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
    }
}
