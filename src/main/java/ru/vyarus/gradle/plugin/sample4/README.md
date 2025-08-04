# 4. Method calls are not cached, but providers do

At runtime, method calls remain as-is, but calls to providers are replaced with direct value.

A plugin with two method calls and one provider call inside the task's doLast method:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample4/Sample4Plugin.java#L13-L34

`startTime` property configured from command line to be able to change it easily

Run with cache enabled: `task1 -PstartTime=1 --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: task1
[configuration] Provider called: 1

> Task :task1
called computeMessage('static')
Task exec / static value: Computed message: static
called computeMessage('provider 1')
Task exec / provider value: Computed message: provider 1

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

Provider and methods were called.

Run from cache: `task1 -PstartTime=1 --configuration-cache --configuration-cache-problems=warn`

```
Reusing configuration cache.

> Task :task1
called computeMessage('static')
Task exec / static value: Computed message: static
called computeMessage('provider 1')
Task exec / provider value: Computed message: provider 1

BUILD SUCCESSFUL in 82ms
1 actionable task: 1 executed
Configuration cache entry reused.
```

Provider value cacheв, methods called. So Provider is not for "cache avoidance" but
for complex data extractions encapsulation so gradle could cache only its value.

Note that `computeMessage` method is still called.

And now change property value (note that special gradle providers for properties were not used!):

`task1 -PstartTime=2 --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as configuration cache cannot be reused because the set of Gradle properties has changed: the value of 'startTime' was changed.
[configuration] Provider called: 2

> Task :task1
called computeMessage('static')
Task exec / static value: Computed message: static
called computeMessage('provider 2')
Task exec / provider value: Computed message: provider 2

BUILD SUCCESSFUL in 108ms
1 actionable task: 1 executed
Configuration cache entry stored.
```

Cache invalidated, the result is correct.

### Build file changes

If property would be declared inside build file:

```groovy
ext.startTime = 1
```

Then, changing the property value would also lead to cache invalidation:

```groovy
ext.startTime = 2
```

```
Calculating task graph as configuration cache cannot be reused because file 'build.gradle' has changed.
[configuration] Provider called: 2

> Task :task1
called computeMessage('static')
Task exec / static value: Computed message: static
called computeMessage('provider 2')
Task exec / provider value: Computed message: provider 2

BUILD SUCCESSFUL in 131ms
1 actionable task: 1 executed
Configuration cache entry stored.
```

### Always called "provider"

Gradle provides `ValueSource` which could be used for "always called" providers implementation:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample4/value/NonCacheableValue.java#L11-L19

Use it instead of the previous provider:

https://github.com/xvik/learn-gradle-configuration-cache/blob/d72120bba0c73231e509165665e8482d14128218/src/main/java/ru/vyarus/gradle/plugin/sample4/value/Sample4ValuePlugin.java#L16-L25

Run with cache enabled: `task1 -Dfoo=1 --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: task1

> Task :task1
called computeMessage('static 1')
Task exec / static value: Computed message: static 1
NonCacheableValue: 1
called computeMessage('provider 1')
Task exec / provider value: Computed message: provider 1

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

Run from cache: `task1 -Dfoo=1 --configuration-cache --configuration-cache-problems=warn`

```
Reusing configuration cache.

> Task :task1
called computeMessage('static 1')
Task exec / static value: Computed message: static 1
NonCacheableValue: 1
called computeMessage('provider 1')
Task exec / provider value: Computed message: provider 1

BUILD SUCCESSFUL in 79ms
1 actionable task: 1 executed
Configuration cache entry reused.
```

Run with different property value: `task1 -Dfoo=2 --configuration-cache --configuration-cache-problems=warn`

```
Reusing configuration cache.

> Task :task1
called computeMessage('static 2')
Task exec / static value: Computed message: static 2
NonCacheableValue: 2
called computeMessage('provider 2')
Task exec / provider value: Computed message: provider 2

BUILD SUCCESSFUL in 44ms
```

No cache invalidation performed because changed value does not affect configuration phase,
but still, new value applied.

Note: in this example, custom value usage looks useless and it is! The example just shows
how providers are called and how to do always called propvider. ValueSource could be
very useful in configuration time (when gradle cache would be too aggressive).