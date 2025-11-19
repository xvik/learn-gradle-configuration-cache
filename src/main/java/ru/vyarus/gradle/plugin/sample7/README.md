# Preserve build service's state

The only way to preserve state in build service is to use parameters.
But there is a caveat: service would remember parameters state **on first initialization**.
So, if you need to collect some state during configuration, you need to delay the first service initialization.

Note: in the [previous example](../sample6) service was initialized at configuration time
(service was resolved in order to append state). In that case such parameter trick would not work. 

## Service

[Service](Service.java) just declares parameters.

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

[Plugin](Sample7Plugin.java) use custom `List` to collect state during configuration,
and pass the link to this list as service parameter.

```java
public class Sample7Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final List<String> values = new ArrayList<>();

        Provider<Service> service = project.getGradle().getSharedServices().registerIfAbsent(
                "service", Service.class, spec -> {
                    System.out.println("[configuration] Creating service");
                    // initial "persisted storage" value
                    spec.getParameters().getValues().value(values);
                });

        values.add("val1");
        values.add("val2");

        project.getTasks().register("sample7Task", task ->
                task.doFirst(task1 ->
                        System.out.println("Task see state: " + service.get().getParameters().getValues().get()))
        );
    }
}
```

Service will initialize only at runtime and so the collected configuration state would be preserved.

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample7/Sample7PluginKitTest.java) shows that delayed service 
initialization would save the state. 

### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample7/Sample7PluginKitTest.java:L31) with cache enabled: `sample7Task --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: sample7Task

> Configure project :
[configuration] Creating service

> Task :sample7Task
Service created with state: [val1, val2]
Task see state: [val1, val2]

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample7/Sample7PluginKitTest.java:L43) with cache enabled: `sample7Task --configuration-cache --configuration-cache-problems=warn`

```
Reusing configuration cache.

> Task :sample7Task
Service created with state: [val1, val2]
Task see state: [val1, val2]

BUILD SUCCESSFUL in 67ms
1 actionable task: 1 executed
Configuration cache entry reused.
```

Service parameter contains state, available at the first service initialization.

But remember that recovering service state in many cases is not required as you can just [cache
service access result](../sample6) under configuration cache.
