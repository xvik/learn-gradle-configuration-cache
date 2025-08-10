package ru.vyarus.gradle.plugin.sample8;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;

/**
 * @author Vyacheslav Rusakov
 * @since 10.08.2025
 */
public abstract class Service implements BuildService<BuildServiceParameters.None>,
        OperationCompletionListener {

    @Override
    public void onFinish(FinishEvent finishEvent) {
        System.out.println("Finish event: " + finishEvent.getDescriptor().getName());
    }
}
