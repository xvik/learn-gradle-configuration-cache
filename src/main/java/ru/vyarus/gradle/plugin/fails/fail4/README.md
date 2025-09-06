# Task and plugin serialization difference

Tasks and plugins are serialized differently!

## Sample source

[Task](Fail4Task.java) assignes `SourceSet` into the private property and does not use it at runtime:

```java
public abstract class Fail4Task extends DefaultTask {

    private SourceSet sourceSet;
    private String name;

    public Fail4Task() {
        sourceSet = getProject().getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main");
        name = sourceSet.getName();
    }

    @TaskAction
    public void run() {
        System.out.println("[run] Task source set: " + name);
    }
}
```

[Plugin](Fail4Plugin.java) assignes `SourceSet` into the private property and does not use it at runtime:

```java
public class Fail4Plugin implements Plugin<Project> {
    
    private SourceSet sourceSet;

    @Override
    public void apply(Project project) {
        sourceSet = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("test");

        project.getTasks().register("fail4Task", Fail4Task.class, task ->  {
            String set = sourceSet.getName();
            task.doLast(task1 ->
                    System.out.println("[run] Project source set: " + set));
        });
    }
}
```

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/fails/fail4/Fail4PluginKitTest.java)
output `fail4Task  --configuration-cache --configuration-cache-problems=warn`:

```
Calculating task graph as no cached configuration is available for tasks: fail4Task

> Task :fail4Task
[run] Task source set: main
[run] Project source set: test

2 problems were found storing the configuration cache.
- Task `:fail4Task` of type `ru.vyarus.gradle.plugin.fails.fail4.Fail4Task`: cannot deserialize object of type 'org.gradle.api.tasks.SourceSet' as these are not supported with the configuration cache.
  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types
- Task `:fail4Task` of type `ru.vyarus.gradle.plugin.fails.fail4.Fail4Task`: cannot serialize object of type 'org.gradle.api.internal.tasks.DefaultSourceSet', a subtype of 'org.gradle.api.tasks.SourceSet', as these are not supported with the configuration cache.
  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types

See the complete report at file:///tmp/junit3909871766538813794/build/reports/configuration-cache/ej5hwigx1x3swf7o2d6u9466r/6lf4x0dgeq19zwf53mjr1wh3t/configuration-cache-report.html

[Incubating] Problems report is available at: file:///tmp/junit3909871766538813794/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
Configuration cache entry stored with 2 problems.
```

Gradle complains only about the task! So the task is serialized completely, but the plugin is not!

## Fix

Fixing only task (plugin remains the same):

```java
public class Fail4FixTask extends DefaultTask {

    private String name;

    public Fail4FixTask() {
        SourceSet sourceSet = getProject().getExtensions()
                .getByType(JavaPluginExtension.class).getSourceSets().getByName("main");
        name = sourceSet.getName();
    }

    @TaskAction
    public void run() {
        System.out.println("[run] Task source set: " + name);
    }
}
```

There would be no problems anymore `fail4Fix  --configuration-cache --configuration-cache-problems=warn`:

```
Calculating task graph as no cached configuration is available for tasks: fail4Fix

> Task :fail4Fix
[run] Task source set: main
[run] Project source set: test

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
Configuration cache entry stored.
```