package ru.vyarus.gradle.plugin.sample1;

/**
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public class Sample1Extension {

    public String message = "Default";

    public Sample1Extension() {
        System.out.println("[configuration] Extension created");
    }

    // used to show if message getter used under cache
    public String getMessage() {
        System.out.println("Extension get message: " + message);
        return message;
    }
}
