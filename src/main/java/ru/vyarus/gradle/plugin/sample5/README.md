# Task constructor

`Project` object can't be used in task's run method, but it *could* be used in task constructor.
This way, all required project-related properties could be easily initialized.

NOTE: Of course, it is better to initialize a task through properties, but often task requires
some internal staff, which should not be exposed as a public api.

Task:

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

Plugin:

```java
public abstract class Sample5Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
          project.getTasks().register("sample5Task", Sample5Task.class);
    }
}
```

Run with cache enabled: `sample5Task --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: sample5Task
[configuration] Task created

> Task :sample5Task
Task executed: junit6467732403551041708

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

Run from cache: `sample5Task --configuration-cache --configuration-cache-problems=warn`

```
Reusing configuration cache.

> Task :sample5Task
Task executed: junit6467732403551041708

BUILD SUCCESSFUL in 81ms
1 actionable task: 1 executed
Configuration cache entry reused.
```