# Real life case

I faced this case migrating my [quality plugin](https://github.com/xvik/gradle-quality-plugin):
this plugin listens for quality tasks execution (checkstyle, pmd, spotbugs).
After each quality task it prints found issues into console.

The problem here is that quality tasks are not controlled by plugin (3rd party tasks) - so
I need to listen for tasks execution and convert task output (xml report) into console output.

There are two ways to listen task: `doLast` block and build service listener.
As was shown in [sample 8](../sample8), doLast block is not called if task was not
executed (UP-TO-DATE or FROM-CACHE). Quality tasks are cacheable,
so they might not execute under configuration cache.

The problem with a build service listener is that it knows only the task path, but
xml report is required to build console output.

That means that tasks info must be collected in the configuration phase and passed
into build service (so it can use it in listener method).

## Service

[Service](Service.java) receive required info as parameter and use it in task listener. 

```java
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
```

## Plugin

[Plugin](Sample10Plugin.java) use tasks lazy configuration block to collect required task info
inside plugin field and then use this field for build service initialization.

```java
public abstract class Sample10Plugin implements Plugin<Project> {

    @Inject
    public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    // collecting tasks info during configuration phase
    private final List<TaskDesc> tasksInfo = new CopyOnWriteArrayList<>();

    @Override
    public void apply(Project project) {
        Provider<Service> service = project.getGradle().getSharedServices()
                .registerIfAbsent("service", Service.class, spec -> {
                    System.out.println("Service configured: " + tasksInfo);
                    spec.getParameters().getValues().value(tasksInfo);
                });

        getEventsListenerRegistry().onTaskCompletion(service);

        // register multiple tasks
        project.getTasks().register("task1", TrackedTask.class);
        project.getTasks().register("task2", TrackedTask.class);

        // capture information about tasks using lazy block - we will need only actually executed tasks
        project.getTasks().withType(TrackedTask.class).configureEach(task -> {
            captureTaskInfo(task);
            task.doLast(task1 -> {
                final TaskDesc desc = service.get().getParameters().getValues().get().stream()
                        .filter(taskDesc -> taskDesc.getPath().equals(task.getPath()))
                        .findAny().orElse(null);
                if (!desc.isCalled()) {
                    System.out.println("Task " + task1.getName() + " doLast");
                    desc.setCalled(true);
                }
            });
        });
    }

    private void captureTaskInfo(Task task) {
        System.out.println("Store task descriptor: " + task.getName());
        tasksInfo.add(new TaskDesc(task.getName(), task.getPath()));
    }
}
```

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample10/Sample10PluginKitTest.java)
demonstrates that service is initialized properly

### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample10/Sample10PluginKitTest.java:31)
`task1, task2, --configuration-cache, --configuration-cache-problems=warn` :

```
Calculating task graph as no cached configuration is available for tasks: task1 task2

> Configure project :
Service configured: []
Store task descriptor: task1
Store task descriptor: task2

> Task :task1
Service created with state: [task1, task2]
Task task1 doLast

> Task :task2
Task task2 doLast
Task :task1 listened, but ignored
Task :task2 listened, but ignored

BUILD SUCCESSFUL in 3s
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample10/Sample10PluginKitTest.java:47)
`task1, task2, --configuration-cache, --configuration-cache-problems=warn` : 

```
Reusing configuration cache.

> Task :task1
Service created with state: [task1, task2]
Task task1 doLast

> Task :task2
Task task2 doLast
Task :task1 listened, but ignored
Task :task2 listened, but ignored

BUILD SUCCESSFUL in 74ms
2 actionable tasks: 2 executed
Configuration cache entry reused.
```

Service parameter contains state, prepared in the first run and so task listener could work properly.