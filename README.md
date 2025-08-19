# Learn gradle configuration cache by examples
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![CI](https://github.com/xvik/learn-gradle-configuration-cache/actions/workflows/CI.yml/badge.svg)](https://github.com/xvik/learn-gradle-configuration-cache/actions/workflows/CI.yml)

Repository contains various samples to show the behavior of Gradle configuration cache.
You can use it as a learning playground: each sample could be modified and re-run (with test). 

Useful links:

* [General description](https://docs.gradle.org/current/userguide/configuration_cache.html)
* [**Configuration cache usage guide**](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html) 
* [Build services](https://docs.gradle.org/current/userguide/build_services.html)
* [Gradle services](https://docs.gradle.org/current/userguide/service_injection.html#service_injection)

## TL;DR

* You can do whatever you want at *configuration* time. You are limited at *execution* time
    - `Provider` and `ValueSource` are common workarounds for execution time
    - Objects (custom objects, tasks, etc.) field values are preserved (values assigned at configuration time!) 
* You can't rely on objects uniqueness because configurations are serialized, which means the same object,
used at different places would be deserialized into different instances
    - Build services are the only way for uniqueness
    - Build services could also replace TaskExecutionListener (not compatible with configuration cache)
    - The first run with the enabled configuration cache is **not the same** as the usual Gradle run (objects
      serialization side effects already present)!
* Method calls (at execution time) are not cached, but provider values are
* Task constructor is in configuration scope, so the project is accessible!
* Simple configuration cache usage tips are not covered, read [gradle guide](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html)

## Samples

1. [Simple demo](src/main/java/ru/vyarus/gradle/plugin/sample1/) shows what plugin parts are not executed under cache
2. [Shared objects](src/main/java/ru/vyarus/gradle/plugin/sample2/) shows configuration serialization side effects
3. [Build services](src/main/java/ru/vyarus/gradle/plugin/sample3/) shows how to communicate between tasks under cache and listen for tasks execution
4. [Method calls, providers](src/main/java/ru/vyarus/gradle/plugin/sample4/) shows provider behavior at execution time 
5. [Task constructor](src/main/java/ru/vyarus/gradle/plugin/sample5/) shows that task constructor could be used to reference all required 
project-related properties under configuration time 
6. [Build service runtime access](src/main/java/ru/vyarus/gradle/plugin/sample6/) shows that build service
can't remember its state (changed during configuration), but gradle could cache service access
points, so there is (probably) no need to maintain the correct state.
7. [Build service parameter](src/main/java/ru/vyarus/gradle/plugin/sample7/) state, collected under configuration phase
could be stored in the build service parameter (but with a caveat)
8. [Build cache](src/main/java/ru/vyarus/gradle/plugin/sample8/) might be used together with the configuration cache,
but this might lead to not executed `doFirst`/`doLast` blocks (on which you could rely on). Service could workaround this
limitation.
9. [Multi module projects](src/main/java/ru/vyarus/gradle/plugin/sample9/) pays attention to multi-module
projects side effects (which must be also counted)

For each sample a test output is present in readme. But you can run tests yourself 
(with modifications or other gradle versions).

## Implementation details

Requires java 17 (multiline strings used to simplify tests).

Each sample is in its own package. Relative test lay out in the same package.

Tests use Gradle TestKit. To demonstrate configuration cache, the same build must be run 
multiple times:

* First with configuration cache enabled (`--configuration-cache`) to create cache record
* The second run shows behavior under the configuration cache
* Some tests use a third execution to show cache invalidation.

Each test run creates a temp directory as a project root (`@TempDir File projectDir`).
Build file created inside this directory.

Example run:

```java
BuildResult result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(List.of("myTask", "--configuration-cache"))
        .withPluginClasspath()
        .forwardOutput()
        .build();

// validation
result.getOutput().contains("something");
```

All tests end with `KitTest` because usually gradle plugin projects also contain `ProjectBuilder`-based
tests. There are no such tests, but convention preserved.

AssertJ used because of its great errors output on strings comparison.

## Configuration cache errors

Just in case, when there is a configuration cache problem, gradle would idicate it like this:

```
> Task :sample5Task
Task executed: junit12045893691932608949

1 problem was found storing the configuration cache.
- Task `:sample5Task` of type `ru.vyarus.gradle.plugin.sample5.Sample5Task`: invocation of 'Task.project' at execution time is unsupported.
  See https://docs.gradle.org/8.13/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution

```

During the real plugin project migration, the target is to eliminate all such errors.

Note that different errors might appear in different cases: all plugin execution "branches" must be checked for 
configuration cache compatibility (gradle will not print all warnings at once - only for actually executed code).

### TestKit jococo problem

When running TestKit-based tests with enabled jococo plugin (for coverage), you'll have [an issue](https://docs.gradle.org/8.14.3/userguide/configuration_cache.html#config_cache:not_yet_implemented:testkit_build_with_java_agent):

```
1 problem was found storing the configuration cache.
- Gradle runtime: support for using a Java agent with TestKit builds is not yet implemented with the configuration cache.
  See https://docs.gradle.org/8.14.3/userguide/configuration_cache.html#config_cache:not_yet_implemented:testkit_build_with_java_agent
```

But, it's not a critical problem: test must check that it was THE ONLY problem:

```java
BuildResult result = run('someTask', '--configuration-cache', '--configuration-cache-problems=warn');
Assertions.assertThat(result.getOutput()).contains(
                "1 problem was found storing the configuration cache",
                "Gradle runtime: support for using a Java agent with TestKit",
                "Calculating task graph as no cached configuration is available for tasks:"
);
```
