package ru.vyarus.gradle.plugin.sample6;

import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * @author Vyacheslav Rusakov
 * @since 08.08.2025
 */
public abstract class Service2 implements BuildService<Service2.Params>, AutoCloseable {

    public Service2() {
        System.out.println("Service 2 created");
    }

    public void doAction() {
        // just show that service 1 could be called and it's state is correct
        System.out.println("Service 2 called Service 1 with state: "
                + getParameters().getService1().get().printState());
    }

    @Override
    public void close() throws Exception {
        System.out.println("Service 2 closed");
    }

    interface Params extends BuildServiceParameters {
        // this is the only way to reference other service
        Property<Service1> getService1();
    }
}
