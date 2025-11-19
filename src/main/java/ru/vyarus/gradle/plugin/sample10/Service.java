package ru.vyarus.gradle.plugin.sample10;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;
import org.gradle.tooling.events.task.TaskFinishEvent;

/**
 * Sample of collecting task information to use in the build service (task listener).
 *
 * @author Vyacheslav Rusakov
 * @since 19.11.2025
 */
public abstract class Service implements BuildService<Service.Params>, OperationCompletionListener {

    public Service() {
        System.out.println("Service created with state: " + getParameters().getValues().get());
    }

    @Override
    public void onFinish(FinishEvent finishEvent) {
        if (finishEvent instanceof TaskFinishEvent) {
            TaskFinishEvent taskEvent = (TaskFinishEvent) finishEvent;
            String taskPath = taskEvent.getDescriptor().getTaskPath();
            TaskDesc desc = getParameters().getValues().get().stream()
                    .filter(it -> it.getPath().equals(taskPath))
                    .findFirst().orElse(null);

            if (desc != null) {
                if (!desc.isCalled()) {
                    System.out.println("Task " + taskPath + " listened by service");
                    desc.setCalled(true);
                } else {
                    System.out.println("Task " + taskPath + " listened, but ignored");
                }
            }
        }
    }

    interface Params extends BuildServiceParameters {
        ListProperty<TaskDesc> getValues();
    }
}
