package ru.vyarus.gradle.plugin.sample4.value;

import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2025
 */
public abstract class NonCacheableValue implements ValueSource<String, ValueSourceParameters.None> {

    @Override
    public @Nullable String obtain() {
        String val = System.getProperty("foo");
        System.out.println("NonCacheableValue: " + val);
        return val;
    }
}
