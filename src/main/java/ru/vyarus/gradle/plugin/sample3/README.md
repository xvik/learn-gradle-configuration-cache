# 3. Share state with build service

[Sample 2](../sample2) showed that usual objects can't be used to share state between different tasks
under the configuration cache (because task configurations are serialized and so separate object instances 
appear on each deserialization).

The only option to share state between tasks is to use gradle [build services](https://docs.gradle.org/current/userguide/build_services.html): 
basically, it's the same as a shared object, but managed by gradle.

## Naive implementation

Straightforward implementation, but with a side effect. 

### Extension

Simple [extension](Sample3Extension.java) (to see how configuration values apply):

```java
public class Sample3Extension {
    public String message = "Default";
}
```

### Service

[Service](SharedService.java) used to share state between tasks:

```java
public abstract class SharedService implements BuildService<SharedService.Params>, AutoCloseable {

    public String extParam;
    // tasks might be executed in parallel (this simply avoids ConcurrentModificationException)
    public List<String> list = new CopyOnWriteArrayList<>();

    public SharedService() {
        // could appear both in configuration and execution time
        System.out.println("Shared service created " + System.identityHashCode(this) + "@");
    }

    public interface Params extends BuildServiceParameters {
        Property<String> getExtParam();
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + "@" + list.toString()
                + ", param: " + getParameters().getExtParam().getOrNull()
                + ", field: " + extParam;
    }

    // IMPORTANT: gradle could close service at any time and start a new instance!
    @Override
    public void close() throws Exception {
        System.out.println("Shared service closed: " + System.identityHashCode(this));
    }
}
```

Note that the same extension parameter is applied as the service parameter and directly into the property (to see the difference).

`AutoClosable` implemented to show when gradle kills service.

Thread-safe array (`CopyOnWriteArrayList`) used because **service could be accessed concurrently**
(and will be, without manual tasks ordering)

### Plugin

[Plugin](Sample3Plugin.java):

* Register extension, shared service
* Declare 2 tasks, using the service
* Assign extension state into service using properties and direct assignment

```java
public abstract class Sample3Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final Sample3Extension ext = project.getExtensions().create("sample3", Sample3Extension.class);
        // IMPORTANT: service not created at this moment! It's just a provider
        // It is also important to not resolve it too early because parameters might be initialized with defaults
        // (user-defined configuration might not be applied to extension yet)
        final Provider<SharedService> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", SharedService.class, spec -> {
                    // configuration value set with parameter
                    spec.getParameters().getExtParam().convention(project.provider(() -> ext.message));
                });

        // configuration value set DIRECTLY (to show difference)
        project.afterEvaluate(p -> {
            service.get().extParam = ext.message;
            System.out.println("[configuration] Project evaluated. Direct assigning: " + ext.message + " to service " + service.get() + ")");
        });

        project.getTasks().register("task1").configure(task ->
                task.doLast(task1 -> {
                    final SharedService sharedService = service.get();
                    sharedService.list.add("Task 1");
                    System.out.println("[run] Task 1 shared object: " + sharedService);
                }));

        project.getTasks().register("task2").configure(task -> {
            // For predictable execution sequence (simpler to validate in test).
            // Without it, tasks will run concurrently!
            task.mustRunAfter("task1");
            
            task.doLast(task1 -> {
                final SharedService sharedService = service.get();
                sharedService.list.add("Task 2");
                System.out.println("[run] Task 2 shared object: " + sharedService);
            });
        });
    }
}
```

Task dependency (`task.mustRunAfter("task1")`) used to order tasks execution.
It would work without it, but it's harder to write output assertions.

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample2/Sample3PluginKitTest.java)
configure plugin extension:

```groovy
plugins {
    id 'java'
    id 'ru.vyarus.sample3'
}

sample3 {
    message = "Configured!"
}
```

### Simple run

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample3/Sample3PluginKitTest.java:L35) it without the configuration cache enabled: `task1 task2`

```
> Configure project :
Shared service created 2051638568@
[configuration] Project evaluated. Direct assigning: Configured! to service 2051638568@[], param: Configured!, direct param: Configured!)

> Task :task1
[run] Task 1 shared object: 2051638568@[Task 1], param: Configured!, field: Configured!

> Task :task2
[run] Task 2 shared object: 2051638568@[Task 1, Task 2], param: Configured!, field: Configured!
Shared service closed: 2051638568

BUILD SUCCESSFUL in 3s
2 actionable tasks: 2 executed
```

As expected, shared service created once and closed after tasks execution. 
Both service parameter and field contain the same value.

### Configuration cache entry creation

Now [run](/src/test/java/ru/vyarus/gradle/plugin/sample3/Sample3PluginKitTest.java:L43) with configuration cache enabled:
`task1 task2 --configuration-cache`

```
Calculating task graph as no cached configuration is available for tasks: task1 task2

> Configure project :
Shared service created 1676456863@
[configuration] Project evaluated. Direct assigning: Configured! to service 1676456863@[], param: Configured!, direct param: Configured!)
Shared service closed: 1676456863

> Task :task1
Shared service created 206909926@
[run] Task 1 shared object: 206909926@[Task 1], param: Configured!, field: null

> Task :task2
[run] Task 2 shared object: 206909926@[Task 1, Task 2], param: Configured!, field: null
Shared service closed: 206909926

BUILD SUCCESSFUL in 388ms
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

You can already see that shared service **created 2 times** here. That's because gradle
guess required service lifetime by its usage and, with configuration cache enabled, it
can't guess correctly.

Also, because service was created and closed BEFORE tasks execution, 
direct field value does not survive.

So, a build service is always **created with constructor** and does not use serialization,
and so you **can't rely on internal service state** (to be serialized)

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample3/Sample3PluginKitTest.java:L54) again: `task1 task2 --configuration-cache`

```
Reusing configuration cache.

> Task :task1
Shared service created 1754437046@
[run] Task 1 shared object: 1754437046@[Task 1], param: Configured!, field: null

> Task :task2
[run] Task 2 shared object: 1754437046@[Task 1, Task 2], param: Configured!, field: null
Shared service closed: 1754437046
```

As you can see, direct field initialization (external, in plugin) is useless, but the same service
 instance is used in both tasks. Mission accomplished - data shared between tasks.

## Real singleton

There is a way to achieve a real singleton: service must [listen tasks execution](https://docs.gradle.org/current/userguide/build_services.html#operation_listener). 
This way, gradle would not know when to close service (because it's required to call it after each task) and so the same
instance will survive within the entire build.

### Updated service

Service [implements `OperationCompletionListener`](singleton/SharedServiceSingleton.java):

```java
public abstract class SharedServiceSingleton implements BuildService<SharedServiceSingleton.Params>, AutoCloseable,
        OperationCompletionListener {
    
    ...

    @Override
    public void onFinish(FinishEvent finishEvent) {
        System.out.println("Finish event: " + finishEvent.getDescriptor().getName() + " caught on service " + this);
    }
}
```

And plugin must [register it as a listener](singleton/Sample3SingletonPlugin.java):

```java
public abstract class Sample3SingletonPlugin implements Plugin<Project> {

    @Inject
    public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    @Override
    public void apply(Project project) {
        ...
        final Provider<SharedServiceSingleton> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", SharedServiceSingleton.class, spec -> {
                    // configuration value set with parameter
                    spec.getParameters().getExtParam().convention(project.provider(() -> ext.message));
                });
        // service listens for tasks completion, which prevents gradle from stopping it in the middle of the build
        getEventsListenerRegistry().onTaskCompletion(service);

        ...
    }
}
```

## Test 2

Running the same test with the updated plugin and service.

### Simple run

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample3/singleton/Sample3SingletonPluginKitTest.java:L35) without cache enabled:
`task1 task2`

```
> Configure project :
Shared service created 392451429@
[configuration] Project evaluated. Direct assigning: Configured! to service 392451429@[], param: Configured!, field: Configured!)

> Task :task1
[run] Task 1 shared object: 392451429@[Task 1], param: Configured!, field: Configured!

> Task :task2
[run] Task 2 shared object: 392451429@[Task 1, Task 2], param: Configured!, field: Configured!
Finish event: :task1 caught on service 392451429@[Task 1, Task 2], param: Configured!, field: Configured!
Finish event: :task2 caught on service 392451429@[Task 1, Task 2], param: Configured!, field: Configured!
Shared service closed: 392451429

BUILD SUCCESSFUL in 2s
2 actionable tasks: 2 executed
```

No surprises - it works the same (just task listener logs added)

### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample3/singleton/Sample3SingletonPluginKitTest.java:L43) with the configuration cache enabled:
`task1 task2 --configuration-cache`

```
Calculating task graph as no cached configuration is available for tasks: task1 task2

> Configure project :
Shared service created 16533414@
[configuration] Project evaluated. Direct assigning: Configured! to service 16533414@[], param: Configured!, field: Configured!)

> Task :task1
[run] Task 1 shared object: 16533414@[Task 1], param: Configured!, field: Configured!
Finish event: :task1 caught on service 16533414@[Task 1], param: Configured!, direct param: Configured!

> Task :task2
[run] Task 2 shared object: 16533414@[Task 1, Task 2], param: Configured!, direct param: Configured!
Finish event: :task2 caught on service 16533414@[Task 1, Task 2], param: Configured!, field: Configured!
Shared service closed: 16533414

BUILD SUCCESSFUL in 397ms
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

As you can see, **service is not closed anymore**!

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample3/singleton/Sample3SingletonPluginKitTest.java:L54) again: `task1 task2 --configuration-cache`

```
Reusing configuration cache.

> Task :task1
Shared service created 1490472003@
[run] Task 1 shared object: 1490472003@[Task 1], param: Configured!, field: null
Finish event: :task1 caught on service 1490472003@[Task 1], param: Configured!, direct param: null

> Task :task2
[run] Task 2 shared object: 1490472003@[Task 1, Task 2], param: Configured!, field: null
Finish event: :task2 caught on service 1490472003@[Task 1, Task 2], param: Configured!, direct param: null
Shared service closed: 1490472003

BUILD SUCCESSFUL in 50ms
2 actionable tasks: 2 executed
Configuration cache entry reused.
```

Also works as before (but with new task listener-related logs).