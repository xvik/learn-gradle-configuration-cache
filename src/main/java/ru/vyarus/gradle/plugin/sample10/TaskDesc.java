package ru.vyarus.gradle.plugin.sample10;

import java.io.Serializable;

/**
 * Simple value object.
 *
 * @author Vyacheslav Rusakov
 * @since 19.11.2025
 */
public class TaskDesc implements Serializable {
    private String name;
    private String path;
    private boolean called;

    public TaskDesc() {
    }

    public TaskDesc(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isCalled() {
        return called;
    }

    public void setCalled(boolean called) {
        this.called = called;
    }

    @Override
    public String toString() {
        return name;
    }
}
