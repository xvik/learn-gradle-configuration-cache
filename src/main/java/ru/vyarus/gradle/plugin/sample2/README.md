# 2. Shared objects

It is very important to keep in mind that you [can't rely on objects uniqueness](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html#config_cache:requirements:shared_objects):
Under configuration cache, when you use the same objects in various runtime blocks
it would not be the same object at runtime (**even on the first run!**) due to serialization/deserialization.

## Shared object

Here is a [special object](SharedState.java)
used in multiple tasks:

```java
public class SharedState { 
  
     // show how externally assigned values survive 
     public String direct; 
     public String configTime; 
     public List<String> list = new ArrayList<>(); 
  
     public SharedState() { 
         System.out.println("[configuration] Shared state created: " + System.identityHashCode(this)); 
     } 
  
     @Override 
     public String toString() { 
         return System.identityHashCode(this) + "@" + list.toString() + ", direct=" + direct 
                 + ", configTime=" + configTime; 
     } 
} 
```

To string would be used to show object state in console (`System.identityHashCode` would identify object instance)

## Extension

Simple [extension](Sample2Extension.java) used 
to show if the state object could preserve state, assigned from the extension (user input).

```java
public class Sample2Extension {
    public String message = "Default";
}
```

## Plugin

[Plugin](Sample2Plugin.java) 
declares two tasks, referencing THE SAME object instance:

```java
public abstract class Sample2Plugin implements Plugin<Project> { 
  
     @Override 
     public void apply(Project project) { 
         final Sample1Extension ext = project.getExtensions().create("sample2", Sample1Extension.class); 
         // some object, common for two tasks 
         final SharedState state = new SharedState(); 
         state.direct = "Custom"; 
         System.out.println("[configuration] Initial shared object: " + state); 
  
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

User-configured value (from extension) is assigned to the shared object in `afterEvaluate` block (delayed assignment):

```java
project.afterEvaluate(p -> state.configTime = ext.message);
```

Each task modifies `list` state inside the shared object and then prints its state.

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample2/Sample2PluginKitTest.java)
configure plugin extension:

```java
plugins {
    id 'java'
    id 'ru.vyarus.sample2'
}

sample2 {
    message = "Configured!"
}
```

### Simple run

First, let's [run](/src/test/java/ru/vyarus/gradle/plugin/sample2/Sample2PluginKitTest.java:L35) it without the configuration cache enabled:
`task1 task2`

```
> Configure project :
[configuration] Shared state created: 672905948
[configuration] Initial shared object: 672905948@[], direct=Custom, configTime=null

> Task :task1
[run] Task 1 shared object: 672905948@[Task 1], direct=Custom, configTime=Configured!

> Task :task2
[run] Task 2 shared object: 672905948@[Task 1, Task 2], direct=Custom, configTime=Configured!

BUILD SUCCESSFUL in 3s
2 actionable tasks: 2 executed
```

The same instance used everywhere, as expected.

### Configuration cache entry creation

Now [run](/src/test/java/ru/vyarus/gradle/plugin/sample2/Sample2PluginKitTest.java:L43) with the configuration cache enabled: 
`task1 task2 --configuration-cache`

```
Calculating task graph as no cached configuration is available for tasks: task1 task2

> Configure project :
[configuration] Shared state created: 933202558
[configuration] Initial shared object: 933202558@[], direct=Custom, configTime=null

> Task :task2
[run] Task 2 shared object: 1657468480@[Task 2], direct=Custom, configTime=Configured!

> Task :task1
[run] Task 1 shared object: 1796357920@[Task 1], direct=Custom, configTime=Configured!

BUILD SUCCESSFUL in 411ms
2 actionable tasks: 2 executed
Configuration cache entry stored.
```

You can see:

* Tasks **already use different object instances**!
* Objects, referenced in tasks, were **not created with constructor**!
* Shared object **field values survive**! 
* List value does not survive because it is updated at execution time.

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample2/Sample2PluginKitTest.java:L54) again: `task1 task2 --configuration-cache`

```
Reusing configuration cache.

> Task :task2
[run] Task 2 shared object: 1707968050@[Task 2], direct=Custom, configTime=Configured!

> Task :task1
[run] Task 1 shared object: 211975597@[Task 1], direct=Custom, configTime=Configured!

BUILD SUCCESSFUL in 53ms
2 actionable tasks: 2 executed
Configuration cache entry reused.
```

Shared object instances are different.

So, with configuration cache, you **can't rely on objects uniqueness**, but can preserve values in
custom objects (when uniqueness is not important, only contained values accessed).

If you really need to share some state between tasks, you'll have to use build services (see the next sample).
