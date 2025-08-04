# 3. Shared state with build service

Sample 2 showed that usual objects can't be used to share state between different tasks
under configuration cache (because task configurations serialized and so separate object instances 
appears on each deserialization).

The workaround is to use gradle build services: basically it's the same as shared object, but managed by gradle.

## Naive implementation

Simple extension (to see how configuration values apply):

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample3/Sample3Extension.java#L7-L9

Service:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample3/SharedService.java#L14-L41

Note that the same extension parameter is applied as service parameter and directly into property (to see the difference).

Plugin:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample3/Sample3Plugin.java#L13-L52

Running without configuration cache: `task1 task2`

```
> Configure project :
Shared service created 2051638568@
[configuration] Project evaluated. Direct assigning: Configured! to service 2051638568@[], param: Configured!, direct param: Configured!)

> Task :task1
Task 1 shared object: 2051638568@[Task 1], param: Configured!, direct param: Configured!

> Task :task2
Task 2 shared object: 2051638568@[Task 1, Task 2], param: Configured!, direct param: Configured!
Shared service closed: 2051638568

BUILD SUCCESSFUL in 3s
2 actionable tasks: 2 executed
```

As expected, shared service created once and closed after tasks execution. Value set with parameter and
direct values correctly applied.

Now run with configuration cache enabled (it's NOT work from cache - just cache instance creation):
`task1 task2 --configuration-cache`

```
Calculating task graph as no cached configuration is available for tasks: task1 task2

> Configure project :
Shared service created 1676456863@
[configuration] Project evaluated. Direct assigning: Configured! to service 1676456863@[], param: Configured!, direct param: Configured!)
Shared service closed: 1676456863

> Task :task1
Shared service created 206909926@
Task 1 shared object: 206909926@[Task 1], param: Configured!, direct param: null

> Task :task2
Task 2 shared object: 206909926@[Task 1, Task 2], param: Configured!, direct param: null
Shared service closed: 206909926

BUILD SUCCESSFUL in 388ms
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

Pay attention that, this time, service was create and closed BEFORE tasks execution, and so
direct field value does not survive.

And now run using stored cache: `task1 task2 --configuration-cache`

```
Reusing configuration cache.

> Task :task1
Shared service created 1754437046@
Task 1 shared object: 1754437046@[Task 1], param: Configured!, direct param: null

> Task :task2
Task 2 shared object: 1754437046@[Task 1, Task 2], param: Configured!, direct param: null
Shared service closed: 1754437046
```

As you can see, direct field initialization (external, in plugin) is useless, but the same service
 instance is used in both tasks. Mission accomplished - shared object works.

## Real singleton

There is a way to achieve a real singleton: service must listen tasks execution. This way,
gradle would not now when to close service (because it's not used by tasks) and so the same
instance will survive within the entire build.

Service [implements `OperationCompletionListener`](https://github.com/xvik/learn-gradle-configuration-cache/blob/master/src/main/java/ru/vyarus/gradle/plugin/sample3/singleton/SharedServiceSingleton.java):

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

And plugin must [register it as listener](https://github.com/xvik/learn-gradle-configuration-cache/blob/master/src/main/java/ru/vyarus/gradle/plugin/sample3/singleton/Sample3SingletonPlugin.java):

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

Run without configuration cache would look the same: `task1 task2`

```
> Configure project :
Shared service created 392451429@
[configuration] Project evaluated. Direct assigning: Configured! to service 392451429@[], param: Configured!, direct param: Configured!)

> Task :task1
Task 1 shared object: 392451429@[Task 1], param: Configured!, direct param: Configured!

> Task :task2
Task 2 shared object: 392451429@[Task 1, Task 2], param: Configured!, direct param: Configured!
Finish event: :task1 caught on service 392451429@[Task 1, Task 2], param: Configured!, direct param: Configured!
Finish event: :task2 caught on service 392451429@[Task 1, Task 2], param: Configured!, direct param: Configured!
Shared service closed: 392451429

BUILD SUCCESSFUL in 2s
2 actionable tasks: 2 executed
```

But initial run with configuration cache shows that service is not re-created anymore: `task1 task2 --configuration-cache`

```
Calculating task graph as no cached configuration is available for tasks: task1 task2

> Configure project :
Shared service created 16533414@
[configuration] Project evaluated. Direct assigning: Configured! to service 16533414@[], param: Configured!, direct param: Configured!)

> Task :task1
Task 1 shared object: 16533414@[Task 1], param: Configured!, direct param: Configured!
Finish event: :task1 caught on service 16533414@[Task 1], param: Configured!, direct param: Configured!

> Task :task2
Task 2 shared object: 16533414@[Task 1, Task 2], param: Configured!, direct param: Configured!
Finish event: :task2 caught on service 16533414@[Task 1, Task 2], param: Configured!, direct param: Configured!
Shared service closed: 16533414

BUILD SUCCESSFUL in 397ms
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

And run from cache: `task1 task2 --configuration-cache`

```
Reusing configuration cache.

> Task :task1
Shared service created 1490472003@
Task 1 shared object: 1490472003@[Task 1], param: Configured!, direct param: null
Finish event: :task1 caught on service 1490472003@[Task 1], param: Configured!, direct param: null

> Task :task2
Task 2 shared object: 1490472003@[Task 1, Task 2], param: Configured!, direct param: null
Finish event: :task2 caught on service 1490472003@[Task 1, Task 2], param: Configured!, direct param: null
Shared service closed: 1490472003

BUILD SUCCESSFUL in 50ms
2 actionable tasks: 2 executed
Configuration cache entry reused.
```

Also works as before.