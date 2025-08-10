# Build cache

It's not directly related to the configuration cache, but it's important to remember.
[Build cache](https://docs.gradle.org/current/userguide/build_cache.html) may be used together with configuration cache.

"Build cache" cache tasks output, which means that such tasks **may not be executed**! 

For example, you may want to react after some 3rd party task. To do it, you configure `doLast`
block, but it would not work as `doFirst`/`doLast` are also **not executed for UP-TO-DATE** tasks 
(when output taken from cache).

The only way to react on UP-TO-DATE tasks is to use build service with `OperationCompletionListener`
(but it would provide ONLY task path, so, if you need to access any task properties, it must be 
done in configuration time by storing it somewhere).

## Service

[Service](Service.java) listen for tasks execution. 

```java
public abstract class Service implements BuildService<BuildServiceParameters.None>,
        OperationCompletionListener {

    @Override
    public void onFinish(FinishEvent finishEvent) {
        System.out.println("Finish event: " + finishEvent.getDescriptor().getName());
    }
}
```

## Task

[Task](Sample8Task.java) creates output file, so build cache could cache its execution.

```java
public abstract class Sample8Task extends DefaultTask {

    @OutputFile
    public abstract Property<File> getOut();

    @TaskAction
    public void run() throws Exception {
        System.out.println("Task executed");
        File out = getOut().get();

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        writer.append("Sample file content");
        writer.close();
    }
}
```

## Plugin

[Plugin](Sample8Plugin.java) registers service and task and declares custom `doLast` block for the task
(to show that it is not executed)

```java
public abstract class Sample8Plugin implements Plugin<Project> {

    @Inject
    public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    @Override
    public void apply(Project project) {
        // service listen for tasks
        final Provider<Service> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", Service.class);
        getEventsListenerRegistry().onTaskCompletion(service);

        project.getTasks().register("sample8Task", Sample8Task.class, task -> {
            task.getOut().set(project.getLayout().getBuildDirectory().dir("sample8/out.txt").get().getAsFile());
            task.doLast(t -> System.out.println("doLast for sample8Task"));
        });
    }
}
```

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample8/Sample8PluginKitTest.java) would use 
both build and configuration caches.

Build cache would use test-specific temp directory:

```java
 write(file("settings.gradle"), String.format("""
                rootProject.name='sample8'
                buildCache {
                    local {
                        directory = new File("%s")
                    }
                }
                """, cacheDir.getCanonicalPath()));
```

### Cache entries creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample8/Sample8PluginKitTest.java:L47) with cache enabled: `sample8Task --build-cache --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: sample8Task

> Task :sample8Task
Task executed
doLast for sample8Task
Finish event: :sample8Task

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

Task executed, doLast called, service notified.

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample8/Sample8PluginKitTest.java:L62) with cache enabled: `sample8Task --build-cache --configuration-cache --configuration-cache-problems=warn`

```
Reusing configuration cache.
> Task :sample8Task UP-TO-DATE
Finish event: :sample8Task

BUILD SUCCESSFUL in 80ms
1 actionable task: 1 up-to-date
Configuration cache entry reused.
```

This time, the task is UP-TO-DATE (because of the build cache), so doLast is **not called**, but **service notified**.