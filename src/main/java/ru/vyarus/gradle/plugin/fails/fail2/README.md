# Project usage at runtime block

`project` usage at runtime is not allowed. For plugin it means all runtime blocks must not use project 
(or any other prohibited object)

## Sample source

[Plugin](Fail2Plugin.java)

```java
public abstract class Fail1Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("[configuration] Project name: " + project.getName());

        project.getTasks().register("fail1Task", task ->  {
            task.doLast(task1 ->
                    System.out.println("[run] Project name: " + project.getName()));
        });
    }
}
```

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/fails/fail2/Fail2PluginKitTest.java)
output `fail2Task  --configuration-cache --configuration-cache-problems=warn`:

```
Calculating task graph as no cached configuration is available for tasks: fail2Task

> Configure project :
[configuration] Project name: junit16727929338108926619

> Task :fail2Task FAILED

2 problems were found storing the configuration cache.
- Task `:fail2Task` of type `org.gradle.api.DefaultTask`: cannot deserialize object of type 'org.gradle.api.Project' as these are not supported with the configuration cache.
  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types
- Task `:fail2Task` of type `org.gradle.api.DefaultTask`: cannot serialize object of type 'org.gradle.api.internal.project.DefaultProject', a subtype of 'org.gradle.api.Project', as these are not supported with the configuration cache.
  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:disallowed_types

See the complete report at file:///tmp/junit16727929338108926619/build/reports/configuration-cache/47ehjfb227oo5kzbg615m5h3q/dghkqx053bip47mmv9kiov6f3/configuration-cache-report.html

[Incubating] Problems report is available at: file:///tmp/junit16727929338108926619/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':fail2Task'.
> Cannot invoke "org.gradle.api.Project.getName()" because "project" is null

* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':fail2Task'.
...	
Caused by: java.lang.NullPointerException: Cannot invoke "org.gradle.api.Project.getName()" because "project" is null
...

BUILD FAILED in 3s
1 actionable task: 1 executed
Configuration cache entry stored with 2 problems.
```

Note that the behavior is different from with [task case](../fail1) because here
runtime block is part of the serialized configuration, and so there are two errors due to 
serialization and deserialization and eventual NPE fail

## Fix

As with task, there are 2 ways to fix it: use variable or use provider. The latter 
would be useful for lazy-evaluation:

```java
final String projectName = project.getName();

project.getTasks().register("fail2Fix", task ->  {
    task.doLast(task1 ->
            System.out.println("[run] Project name: " + projectName));
});


final Provider<String> nameProvider = project.provider(() -> {
    System.out.println("[configuration] Provider called");
    return project.getName();
});

project.getTasks().register("fail2Fix2", task ->  {
    task.doLast(task1 ->
            System.out.println("[run] Project name: " + nameProvider.get()));
});
```

Running `fail2Fix fail2Fix2 --configuration-cache --configuration-cache-problems=warn`:

```
Calculating task graph as no cached configuration is available for tasks: fail2Fix fail2Fix2
[configuration] Provider called

> Task :fail2Fix2
[run] Project name: junit17295962152626235093

> Task :fail2Fix
[run] Project name: junit17295962152626235093

BUILD SUCCESSFUL in 3s
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

(the order of tasks is not determinate as they run in parallel)