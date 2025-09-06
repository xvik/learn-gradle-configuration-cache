# Project usage in task

The most common fail is `project` usage in task's action.

## Sample source

[Task](Fail1Task.java)

```java
public abstract class Fail1Task extends DefaultTask {

    @TaskAction
    public void run() {
        System.out.println("[run] Task executed for project: " + getProject().getName());
    }
}
```

[Plugin](Fail1Plugin.java)

```java
public abstract class Fail1Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
          project.getTasks().register("fail1Task", Fail1Task.class);
    }
}
```

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/fails/fail1/Fail1PluginKitTest.java) 
output `fail1Task  --configuration-cache --configuration-cache-problems=warn`:

```
Calculating task graph as no cached configuration is available for tasks: fail1Task

> Task :fail1Task
[run] Task executed for project: junit14814233387960710613

1 problem was found storing the configuration cache.
- Task `:fail1Task` of type `ru.vyarus.gradle.plugin.fails.fail1.Fail1Task`: invocation of 'Task.project' at execution time is unsupported.
  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution

See the complete report at file:///tmp/junit14814233387960710613/build/reports/configuration-cache/2n0qomukpcgdka6np2vpry01g/a4zpdprgh0056ayfdj5dwvtkk/configuration-cache-report.html

[Incubating] Problems report is available at: file:///tmp/junit14814233387960710613/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 2s
1 actionable task: 1 executed
Configuration cache entry stored with 1 problem.
```

Note: `configuration-cache-problems=warn` is used to prevent build failure (simpler output).

## Fix

One possible fix is to move property resolution to constructor (which is executed at configuration phase):

```java
public abstract class Fail1Task extends DefaultTask {

    private final String projectName;
    
    public Fail1Task() {
        projectName = getProject().getName();
    }
    
    @TaskAction
    public void run() {
        System.out.println("[run] Task executed for project: " + projectName);
    }
}
```

Another option is to use provider (when resolution should be delayed until project configuration):

```java
public abstract class Fail1Task extends DefaultTask {

    private final Provider<String> projectName;
    
    public Fail1Task() {
        projectName = getProject().provider(() -> getProject().getName());
    }
    
    @TaskAction
    public void run() {
        System.out.println("[run] Task executed for project: " + projectName.get());
    }
}
```