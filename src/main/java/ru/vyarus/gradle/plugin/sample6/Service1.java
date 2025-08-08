package ru.vyarus.gradle.plugin.sample6;

import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Vyacheslav Rusakov
 * @since 08.08.2025
 */
public abstract class Service1 implements BuildService<Service1.Params>, OperationCompletionListener, AutoCloseable {

    private final List<String> state = new CopyOnWriteArrayList<>();

    public Service1() {
        System.out.println("Service 1 created: " + printState());
    }

    public List<String> getState() {
        return state;
    }

    public String printState() {
        return "state=" + state + ", property=" + getParameters().getListHolder().get();
    }

    // ATTENTION: if service state is populated under the configuration phase, then gradle almost
    //  certainly will close it before execution, so better to implement listener to prevent it
    public void addState(String value) {
        state.add(value);
        getParameters().getListHolder().get().add(value);
        System.out.println("Added value '" + value + "': " + printState());
    }

    @Override
    public void onFinish(FinishEvent finishEvent) {
        // only to prevent closing
    }

    @Override
    public void close() throws Exception {
        System.out.println("Service 1 closed");
    }

    interface Params extends BuildServiceParameters {
        // ListProperty will not work! Have to use wrapper object instead
        Property<ListHolder> getListHolder();
    }

    // used as persistent storage which will survive the configuration cache (because values would be appended inside
    // the configuration phase)
    public static class ListHolder implements Serializable {
        private final List<String> list = new CopyOnWriteArrayList<>();

        public List<String> getList() {
            return list;
        }

        public void add(String value) {
            list.add(value);
        }

        @Override
        public String toString() {
            return System.identityHashCode(this) + "@" + list;
        }
    }
}
