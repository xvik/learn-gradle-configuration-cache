# Multi-module projects

This is a similar demo as [sample 7](../sample7), but now plugin would be applied in
multiple modules.

The simplest solution is to make a shared collection static: this way it would be populated
under the configuration phase and stored in service parameters (service is singleton).

The other solution is to make service per-module (by simlply adding project name to service name).
This way, each module will have its own service instance with module-specific cache.

Will demonstrate just the first solution.

NOTE: this demo was separated from [sample 7](../sample7) to pay attention to the potential **multi-module usage side effects**.

NOTE2: Normally, such cases should not appear as gradle direct values caching is usually enough.
This could be required, for example, if you did not own configured tasks and additional 
actions should be performed at doFirst or doLast blocks. As an example, my [quality plugin](https://github.com/xvik/gradle-quality-plugin)
have to use this trick to remember quality tasks parameters to perform console output in
doLast block (but it uses solution 2 for specific reasons).

## Service

[Service](Service.java) just declares parameters (same service as in sample 7).

```java
public abstract class Service implements BuildService<Service.Params> {

    public Service() {
        System.out.println("Service created with state: " + getParameters().getValues().get());
    }

    interface Params extends BuildServiceParameters {
        ListProperty<String> getValues();
    }
}
```

## Plugin

[Plugin](Sample9Plugin.java) use static `List` to collect state during configuration
of all modules, and pass the link to this list as service parameter.

```java
 public class Sample9Plugin implements Plugin<Project> {

    // shared values (same instance for all plugins)
    static List<String> sharedValues = new ArrayList<>();

    @Override
    public void apply(Project project) {

        // service will store shared values in parameters
        final Provider<Service> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", Service.class, spec ->
                        spec.getParameters().getValues().set(sharedValues));

        System.out.println("Value added: " + project.getName());
        sharedValues.add(project.getName());

        project.getTasks().register("sample9Task", task -> {
            task.doLast(task1 ->
                    // service initialization (at this time parameters would be stored)
                    System.out.println("sharedState: " + service.get().getParameters().getValues().get()));
        });
    }
}
```

Service will initialize only at runtime and so the collected configuration state from all modules would be preserved.

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample9/Sample9PluginKitTest.java) shows that delayed service
initialization would save the static state.

### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample9/Sample9PluginKitTest.java:L35) with cache enabled: `sample9Task --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: sample9Task

> Configure project :
Value added: sub1
Value added: sub2

> Task :sub2:sample9Task
sharedState: [sub1, sub2]

> Task :sub1:sample9Task
Service created with state: [sub1, sub2]
sharedState: [sub1, sub2]

BUILD SUCCESSFUL in 2s
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample9/Sample9PluginKitTest.java:L46) with cache enabled: `sample9Task --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: sample9Task

> Configure project :
Value added: sub1
Value added: sub2

> Task :sub2:sample9Task
sharedState: [sub1, sub2]

> Task :sub1:sample9Task
Service created with state: [sub1, sub2]
sharedState: [sub1, sub2]

BUILD SUCCESSFUL in 2s
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

Service parameter contains state, available at the first service initialization
(configuration from both modules).

