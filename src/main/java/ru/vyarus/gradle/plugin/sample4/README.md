# 4. Providers cached, method calls not

As we have seen in [sample 1](../sample1), objects referenced from runtime blocks would be serialized.
Usually, this is the main problem for plugin authors - to avoid redundant serialization.
Here we will see some additional runtime blocks serialization aspects. 

Inside runtime blocks:

* Method calls remain as-is 
* Calls to providers are replaced with direct value.
* `ValueSource` values not cached

## Plugin

[Plugin](Sample4Plugin.java) with two method calls and one provider call inside the task's `doLast` block:

```java
public class Sample4Plugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("task1").configure(task -> {
            Provider<String> provider = project.provider(() -> {
                String res = String.valueOf(project.findProperty("startTime"));
                System.out.println("[configuration] Provider called: " + res);
                return res;
            });
            task.doLast(task1 -> {
                System.out.println("Task exec / static value: " + computeMessage("static"));
                System.out.println("Task exec / provider value: " + computeMessage("provider " + provider.get()));
            });
        });
    }

    private String computeMessage(String source) {
        System.out.println("called computeMessage('" + source + "')");
        return "Computed message: " + source;
    }
}
```

`startTime` property assumed to be set in build script (or through command line).

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample4/Sample4PluginKitTest.java) would 
use the command line to specify a custom property value to see if cache will invalidate
when the property value changes.

### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample4/Sample4PluginKitTest.java:L31) with cache enabled: `task1 -PstartTime=1 --configuration-cache --configuration-cache-problems=warn`

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

Provider was called to calculate value.

### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample4/Sample4PluginKitTest.java:L42) from cache: `task1 -PstartTime=1 --configuration-cache --configuration-cache-problems=warn`

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

* Provider value **cached** (no provider call)
* Methods called (see `computeMessage` method logs). 

`Provider` is not for "cache avoidance" but for complex data extractions encapsulation, so gradle could 
cache only its value.

Overall, **provider is your best friend** with configuration cache. Any custom block,
failed to serialize (using too heavy external objects), could always be extracted from runtime
with `project.provider(() -> // some computations here)`, which would be called at 
configuration time and value cached.

Of course, there are possible side effects, like working with system properties or relying
on other runtime state, but there are [ways to workaround it](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html#config_cache:requirements:reading_sys_props_and_env_vars).

### Run with cache invalidation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample4/Sample4PluginKitTest.java:L42) again, but with different property value
(configuration cache record already exists):
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

Cache **invalidated**, the result is correct (because gradle is aware of its properties, 
but invalidation wouldn't work with [system properties or environment variables](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html#config_cache:requirements:reading_sys_props_and_env_vars)). 

## Build file property

The same behavior would be for property defined inside the build file (and chane after cache record creation):

```groovy
plugins {
    id 'java'
    id 'ru.vyarus.sample4'
}

ext.startTime = 2
```

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample4/Sample4PluginKitTest.java:L119) with the existing cache record:

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

Cache also invalidated.

## Always called "provider"

As we have seen, `Provider` value is cached, but, if you need to always calculate the value,
gradle provides `ValueSource`:

### Non cacheable value

[ValueSource implementation](value/NonCacheableValue.java):

```java
public abstract class NonCacheableValue implements ValueSource<String, ValueSourceParameters.None> {

    @Override
    public @Nullable String obtain() {
        String val = System.getProperty("foo");
        System.out.println("NonCacheableValue: " + val);
        return val;
    }
}
```

Here computed value relies on system property and so must be checked on each run.  

### Updated plugin

[Plugin](value/Sample4ValuePlugin.java) now use this value instead of the provider:

```java
public class Sample4ValuePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getTasks().register("task1").configure(task -> {
            final Provider<String> provider = project.getProviders()
                    .of(NonCacheableValue.class, noneValueSourceSpec -> {});
            task.doLast(task1 -> {
                System.out.println("Task exec / static value: " + computeMessage("static " + System.getProperty("foo")));
                System.out.println("Task exec / provider value: " + computeMessage("provider " + provider.get()));
            });
        });
    }

    private String computeMessage(String source) {
        System.out.println("called computeMessage('" + source + "')");
        return "Computed message: " + source;
    }
}
```

### Value test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample4/value/Sample4ValuePluginKitTest.java)
would run updated plugin (with value instead of provider).

#### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample4/value/Sample4ValuePluginKitTest.java:L31) with cache enabled: `task1 -Dfoo=1 --configuration-cache --configuration-cache-problems=warn`

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

Note that value was called not under configuration phase, but at runtime.

#### Run from cache

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample4/value/Sample4ValuePluginKitTest.java:L42) from cache: `task1 -Dfoo=1 --configuration-cache --configuration-cache-problems=warn`

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

`ValueSource` is **called** under configuration cache

### Run with different property value

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample4/value/Sample4ValuePluginKitTest.java:L52) with different property value: `task1 -Dfoo=2 --configuration-cache --configuration-cache-problems=warn`

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

**No cache invalidation** performed because the changed value does not affect configuration phase,
but still, a new value applied.

Note that the static value was also updated. Most likely it's some "gradle smartness" because, in theory, it should cache it.

Custom value usage looks useless in this example, and it is (there are custom providers for such cases)! 
The example just shows how providers are called and how to do "always called" provider. `ValueSource` 
could be very useful when gradle cache would be too aggressive.