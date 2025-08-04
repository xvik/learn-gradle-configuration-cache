package ru.vyarus.gradle.plugin.sample1;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public abstract class Sample1Task extends DefaultTask {

    @Input
    abstract Property<String> getMessage();

    @Input
    abstract Property<String> getMessage2();

    public String value;
    private String privateValue;

    public Sample1Task() {
        System.out.println("[configuration] Task created");
        privateValue = "set";
    }

    @TaskAction
    public void run() {
        System.out.println("Task executed: param1=" + getMessage().get()
                + ", param2=" + getMessage2().get()
                + ", public field=" + value
                + ", private field=" + privateValue);
    }
}
