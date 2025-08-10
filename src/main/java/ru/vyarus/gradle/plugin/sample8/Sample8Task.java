package ru.vyarus.gradle.plugin.sample8;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Build cache works for task output.
 *
 * @author Vyacheslav Rusakov
 * @since 10.08.2025
 */
public abstract class Sample8Task extends DefaultTask {

    @OutputFile
    public abstract Property<File> getOut();

    @TaskAction
    public void run() throws Exception {
        System.out.println("Task executed");
        File out = getOut().get();

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        writer.append("Sample file content");
        writer.close();
    }
}
