package ru.vyarus.gradle.plugin.sample2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public class SharedState {

    // show how externally assigned values survive
    public String direct;
    public String configTime;
    public List<String> list = new ArrayList<>();

    public SharedState() {
        System.out.println("[configuration] Shared state created: " + System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + "@" + list.toString() + ", direct=" + direct
                + ", configTime=" + configTime;
    }
}
