# Task constructor

`Project` object [can't be used in task's run](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html#config_cache:requirements:use_project_during_execution) 
method, but it **could** be used in task constructor. 
This way, all required project-related properties could be easily initialized (cached).

NOTE: Of course, it is better to initialize a task through properties, but often task requires
some internal staff, which should not be exposed as a public api.

## Task

[Task](Sample5Task.java) requiring project name at runtime:

```java
public abstract class Sample5Task extends DefaultTask {

    private final String projectName;

    public Sample5Task() {
        // this is configuration time! project access allowed!
        projectName = getProject().getName();
        System.out.println("[configuration] Task created");
    }

    @TaskAction
    public void run() {
        // project can't be accessed at runtime
        System.out.println("Task executed: " + projectName);
    }
}
```

## Plugin

[Plugin](Sample5Plugin.java) just declares the task:

```java
public abstract class Sample5Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
          project.getTasks().register("sample5Task", Sample5Task.class);
    }
}
```

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample5/Sample5PluginKitTest.java) would show that 
task constructor runs at configuration time.

### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample5/Sample5PluginKitTest.java:L31) with cache enabled: `sample5Task --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: sample5Task
[configuration] Task created

> Task :sample5Task
Task executed: junit6467732403551041708

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

### Run from cache 

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample5/Sample5PluginKitTest.java:L42) from cache: `sample5Task --configuration-cache --configuration-cache-problems=warn`

```
Reusing configuration cache.

> Task :sample5Task
Task executed: junit6467732403551041708

BUILD SUCCESSFUL in 81ms
1 actionable task: 1 executed
Configuration cache entry reused.
```

Project name was serialized inside task property and used under configuration cache.