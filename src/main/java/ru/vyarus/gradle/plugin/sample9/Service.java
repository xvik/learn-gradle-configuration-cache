package ru.vyarus.gradle.plugin.sample9;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * @author Vyacheslav Rusakov
 * @since 14.08.2025
 */
public abstract class Service implements BuildService<Service.Params> {

    public Service() {
        System.out.println("Service created with state: " + getParameters().getValues().get());
    }

    interface Params extends BuildServiceParameters {
        ListProperty<String> getValues();
    }
}
