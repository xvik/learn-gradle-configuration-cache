# Serialization problem

If runtime block references an object property, then the entire object is serialized.
These errors are not so obvious.

## Sample source

[Extension](Fail3Extension.java) reference not serializable `SourceSet` objects

```java
public class Fail3Extension {

    public Set<SourceSet> sets;
    public String message;

    public Fail3Extension(Project project) {
        this.sets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
    }
}
```

[Plugin](Fail3Plugin.java) uses only string extension property:

```java
public class Fail3Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Fail3Extension ext = project.getExtensions().create("fail3", Fail3Extension.class, project);

        project.getTasks().register("fail3Task", task ->  {
            task.doLast(task1 ->
                    System.out.println("[run] Message: " + ext.message));
        });
    }
}
```

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/fails/fail3/Fail3PluginKitTest.java)
output `fail3Task  --configuration-cache --configuration-cache-problems=warn`:

```
Calculating task graph as no cached configuration is available for tasks: fail3Task

> Task :fail3Task
[run] Message: Configured!

2 problems were found storing the configuration cache.
- Task `:fail3Task` of type `org.gradle.api.DefaultTask`: cannot deserialize object of type 'org.gradle.api.tasks.SourceSetContainer' as these are not supported with the configuration cache.
  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types
- Task `:fail3Task` of type `org.gradle.api.DefaultTask`: cannot serialize object of type 'org.gradle.api.internal.tasks.DefaultSourceSetContainer', a subtype of 'org.gradle.api.tasks.SourceSetContainer', as these are not supported with the configuration cache.
  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types

See the complete report at file:///tmp/junit10544361871289601409/build/reports/configuration-cache/5tucq48qd6ozznv386suhaql7/9u8dhuyl2ja1knarlvrf25z8q/configuration-cache-report.html

[Incubating] Problems report is available at: file:///tmp/junit10544361871289601409/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
Configuration cache entry stored with 2 problems.
```

Extension object can't be serialized due to not serializable `SourceSet` objects. 
Note that there are no hints about the source of the problem - you must guess it.

## Fix

To fix this problem, assign required data to variable in configuration phase:

```java
@Override
public void apply(Project project) {
    Fail3Extension ext = project.getExtensions().create("fail3", Fail3Extension.class, project);

    project.getTasks().register("fail3Task", task ->  {
        String message = ext.message;
        task.doLast(task1 ->
                System.out.println("[run] Message: " + message));
    });
}
```

Here variable is assigned in the task lazy initialization block (and so user configuration is applied).


NOTE: it is important to not assign variable too early as user configuration may not be applied:

```java
String message = ext.message;
project.getTasks().register("fail3Task", task ->  {
    task.doLast(task1 ->
            System.out.println("[run] Message: " + message));
});
```

message would be null.

It is also possible to use a provider.