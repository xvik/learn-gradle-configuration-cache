# 2. Shared objects

Object, referenced in multiple tasks:

```java
public class SharedState {

    // show how externally assigned values survive
    public String direct;
    public String configTime;
    public List<String> list = new ArrayList<>();

    public SharedState() {
        System.out.println("Shared state created: " + System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + "@" + list.toString() + ", direct=" + direct
                + ", configTime=" + configTime;
    }
}
```

Plugin, declaring two tasks, referencing THE SAME object instance:

```java
public abstract class Sample2Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final Sample1Extension ext = project.getExtensions().create("sample2", Sample1Extension.class);
        // some object, common for two tasks
        final SharedState state = new SharedState();
        state.direct = "Custom";
        System.out.println("Initial shared object: " + state);

        // delayed configuration from extension
        project.afterEvaluate(p -> state.configTime = ext.message);

        project.getTasks().register("task1").configure(task ->
                task.doLast(task1 -> {
                    state.list.add("Task 1");
                    System.out.println("Task 1 shared object: " + state);
                }));

        project.getTasks().register("task2").configure(task ->
                task.doLast(task1 -> {
                    state.list.add("Task 2");
                    System.out.println("Task 2 shared object: " + state);
                }));
    }
}
```

Run tasks: `task1 task2`

```
> Configure project :
[configuration] Shared state created: 672905948
[configuration] Initial shared object: 672905948@[], direct=Custom, configTime=null

> Task :task1
Task 1 shared object: 672905948@[Task 1], direct=Custom, configTime=Configured!

> Task :task2
Task 2 shared object: 672905948@[Task 1, Task 2], direct=Custom, configTime=Configured!

BUILD SUCCESSFUL in 3s
2 actionable tasks: 2 executed
```

Same instance used in tasks, as expected.

Now run with the configuration cache enabled: `task1 task2 --configuration-cache`

```
Calculating task graph as no cached configuration is available for tasks: task1 task2

> Configure project :
[configuration] Shared state created: 933202558
[configuration] Initial shared object: 933202558@[], direct=Custom, configTime=null

> Task :task2
Task 2 shared object: 1657468480@[Task 2], direct=Custom, configTime=Configured!

> Task :task1
Task 1 shared object: 1796357920@[Task 1], direct=Custom, configTime=Configured!

BUILD SUCCESSFUL in 411ms
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

IMPORTANT: here the project executed as usual (there was no configuration cache record),
but object instances are ALREADY DIFFERENT!
Note that when configuration cache enabled, objects in tasks are not created with constructor!

Pay attention that string field values survive! List value does not survive because it was updated
in execution time.

And finally, run again (this time configuration cache record would be used):  `task1 task2 --configuration-cache`

```
Reusing configuration cache.

> Task :task2
Task 2 shared object: 1707968050@[Task 2], direct=Custom, configTime=Configured!

> Task :task1
Task 1 shared object: 211975597@[Task 1], direct=Custom, configTime=Configured!

BUILD SUCCESSFUL in 53ms
2 actionable tasks: 2 executed
Configuration cache entry reused.
```

Instances are different.

So with configuration cache you CAN'T rely on objects uniqueness, but can preserve values in
custom objects (when uniqueness is not important, only contained values accessed).
