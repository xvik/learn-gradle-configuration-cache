package ru.vyarus.gradle.plugin.sample3;

import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Vyacheslav Rusakov
 * @since 03.08.2025
 */
public abstract class SharedService implements BuildService<SharedService.Params>, AutoCloseable {

    public String extParam;
    // tasks might be executed in parallel (this simply avoids ConcurrentModificationException)
    public List<String> list = new CopyOnWriteArrayList<>();

    public SharedService() {
        // could appear both in configuration and execution time
        System.out.println("Shared service created " + System.identityHashCode(this) + "@");
    }

    public interface Params extends BuildServiceParameters {
        Property<String> getExtParam();
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + "@" + list.toString()
                + ", param: " + getParameters().getExtParam().getOrNull()
                + ", direct param: " + extParam;
    }

    // IMPORTANT: gradle could close service at any time and start a new instance!
    @Override
    public void close() throws Exception {
        System.out.println("Shared service closed: " + System.identityHashCode(this));
    }
}
