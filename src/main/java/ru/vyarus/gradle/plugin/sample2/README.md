# 2. Shared objects

Object, referenced in multiple tasks:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample2/SharedState.java#L10-L26

Plugin, declaring two tasks, referencing THE SAME object instance:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample2/Sample2Plugin.java#L14-L39

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
