# 1. Configuration cache demo

Extension:

```java
public class Sample1Extension {
    public String message = "Default";
}
```

Task:

```java
public abstract class Sample1Task extends DefaultTask {

    @Input
    abstract Property<String> getMessage();

    @Input
    abstract Property<String> getMessage2();

    public String value;
    private String privateValue;

    public Sample1Task() {
        System.out.println("[configuration] Task created");
        privateValue = "set";
    }

    @TaskAction
    public void run() {
        System.out.println("Task executed: param1=" + getMessage().get()
                + ", param2=" + getMessage2().get()
                + ", public field=" + value
                + ", private field=" + privateValue);
    }
}
```

Plugin:

```java
public abstract class Sample1Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("[configuration] Plugin applied");
        // register configuration extension
        final Sample1Extension ext = project.getExtensions().create("sample1", Sample1Extension.class);

        // register custom task
        project.getTasks().register("sample1Task", Sample1Task.class, task -> {
            // task configured from extension, by default (note provider usage for lazy initialization)
            task.getMessage().convention(project.provider(() -> ext.message));
            task.getMessage2().convention("Default");
            task.value = "assigned value";
            System.out.println("[configuration] Task configured. Ext message: " + ext.message);

            // the only line that works also under the configuration cache
            task.doFirst(task1 -> System.out.println("Before task: " + ext.message));
        });
        // task registered but not yet configured (user configuration also not yet applied)
        System.out.println("[configuration] Task registered. Ext message: " + ext.message);

        // afterEvaluate often used by plugins as the first point where user configuration applied
        project.afterEvaluate(p -> System.out.println("[configuration] Project evaluated. Ext message: " + ext.message));

        // custom (lazy) task configuration
        project.getTasks().withType(Sample1Task.class).configureEach(task -> {
            System.out.println("[configuration] Task delayed configuration. Ext message: " + ext.message);
            task.getMessage2().set("Custom");
        });
    }
}
```

Configuration:

```
sample1 {
    message = "Configured!"
}
```

Run task with configuration cache enabled:  `sample1Task --configuration-cache`

```
Calculating task graph as no cached configuration is available for tasks: sample1Task

> Configure project :
[configuration] Plugin applied
[configuration] Task registered. Ext message: Default
[configuration] Project evaluated. Ext message: Configured!
[configuration] Task created
[configuration] Task configured. Ext message: Configured!
[configuration] Task delayed configuration. Ext message: Configured!

> Task :sample1Task
Before task: Configured!
Task executed: param1=Configured!, param2=Custom, public field=assigned value, private field=set

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

Run again, this time configuration cache applied:

```
Reusing configuration cache.

> Task :sample1Task
Before task: Configured!
Task executed: param1=Configured!, param2=Custom, public field=assigned value, private field=set

BUILD SUCCESSFUL in 60ms
1 actionable task: 1 executed
Configuration cache entry reused.
```

NOTE: it is not recommended to directly reference an extension like this, but not a problem in this case as this extension object is a serializable.

Pay attention that under configuration cache plugin code wasn't called at all!
Only task.doFirst block and task action itself were executed.
Project configuration was completely serialized.

Also, even task constructor was not called! But, as you can see, task fields were preserved!
