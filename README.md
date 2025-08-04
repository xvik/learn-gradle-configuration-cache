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

## TLDR;

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