# 1. Configuration cache demo

Extension:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample1/Sample1Extension.java#L7-L10

Task:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample1/Sample1Task.java#L12-L34

Plugin:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample1/Sample1Plugin.java#L12-L43

Configuration:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/test/java/ru/vyarus/gradle/plugin/sample1/Sample1PluginKitTest.java#L19-L26

Run task with configuration cache enabled:  `sample1Task --configuration-cache`

```
Calculating task graph as no cached configuration is available for tasks: sample1Task

> Configure project :
[configuration] Plugin applied
[configuration] Task registered. Ext message: Default
[configuration] Project evaluated. Ext message: Configured!
[configuration] Task created
[configuration] Task configured. Ext message: Configured!
[configuration] Task delayed configuration. Ext message: Configured!

> Task :sample1Task
Before task: Configured!
Task executed: param1=Configured!, param2=Custom, public field=assigned value, private field=set

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

Run again, this time configuration cache applied:

```
Reusing configuration cache.

> Task :sample1Task
Before task: Configured!
Task executed: param1=Configured!, param2=Custom, public field=assigned value, private field=set

BUILD SUCCESSFUL in 60ms
1 actionable task: 1 executed
Configuration cache entry reused.
```

NOTE: it is not recommended to directly reference an extension like this, but not a problem in this case as this extension object is a serializable.

Pay attention that under configuration cache plugin code wasn't called at all!
Only task.doFirst block and task action itself were executed.
Project configuration was completely serialized.

Also, even task constructor was not called! But, as you can see, task fields were preserved!
