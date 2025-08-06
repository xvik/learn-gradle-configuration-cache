# 1. Configuration cache demo

Gradle configuration cache records state after the configuration phase:

* All plugin code, related to the configuration phase, would not be executed
* Task field values, applied in the configuration phase, would be preserved
* Task and plugin (any object) constructors would not be called
* Objects, referenced from runtime blocks are serialized

## Sample source

### Extension 

Simple [extension](Sample1Extension.java)
with one field, just to show that user-provided value (in build script) is applied

```java
public class Sample1Extension {

    public String message = "Default";

    // used to show if message getter used under cache
    public String getMessage() {
        System.out.println("Extension get message: " + message);
        return message;
    }
}
```

Custom getter used to show if extension object was cached.

### Task

[Task](Sample1Extension.java) with 2 properties and 2 fields.
Plugin would configure properties. Private field initialized in constructor (under configuration phase).
Another field would also be assigned in plugin.

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

### Plugin

[Plugin](Sample1Plugin.java):

* Register extension
* Configure task (on registration and with delayed configuration)
* Use `afterEvaluate` block (executed in configuration phase)

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
            task.doFirst(task1 -> System.out.println("Before task: " + ext.getMessage()));
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

Almost all plugin code would not be executed under the configuration cache: 
all output with `[configuration]` prefix indicates configuration phase execution.

The only plugin line executed under the configuration cache would be `task.doFirst(...)` block. 

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample1/Sample1PluginKitTest.java)
configure plugin extension:

```groovy
plugins {
    id 'java'
    id 'ru.vyarus.sample1'
}

sample1 {
    message = "Configured!"
}
```

### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample1/Sample1PluginKitTest.java:L35) task with configuration cache enabled:  `sample1Task --configuration-cache`

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
Extension get message: Configured!
Before task: Configured!
Task executed: param1=Configured!, param2=Custom, public field=assigned value, private field=set

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
Configuration cache entry stored.
```

All plugin code executed.

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample1/Sample1PluginKitTest.java:L58) again: `sample1Task --configuration-cache`

```
Reusing configuration cache.

> Task :sample1Task
Extension get message: Configured!
Before task: Configured!
Task executed: param1=Configured!, param2=Custom, public field=assigned value, private field=set

BUILD SUCCESSFUL in 56ms
1 actionable task: 1 executed
```

* Note the absence of output from the configuration phase (which was not executed this time).
* Task constructor was also not called, but task field values were preserved
* Only task `run` and `doFirst` block (applied by plugin) were executed

**The hardest point for gradle** is to *track all required info for runtime blocks*.
Just think of it for a moment: it needs to annalyze such blocks and make sure referenced
data is availble, when outside code is not executed anymore.

Task's `doFirst` block (assigned by plugin) access extension object directly:

```java
task.doFirst(task1 -> System.out.println("Before task: " + ext.getMessage()));
```

As you can see from the output, extension getter is still called, which means
that the entire extension object *was serialized*.

Such serialization could be easily avoided by:

1. Assigning extension value into a local variable outside of runtime block (so gradle wouldn't need 
to cache the entire object anymore)
2. Using providers (would be shown in the next samples)