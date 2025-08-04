# Task constructor

`Project` object can't be used in task's run method, but it *could* be used in task constructor.
This way, all required project-related properties could be easily initialized.

NOTE: Of course, it is better to initialize a task through properties, but often task requires
some internal staff, which should not be exposed as a public api.

Task:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample5/Sample5Task.java#L10-L25

Plugin:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample5/Sample5Plugin.java#L12-L18

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