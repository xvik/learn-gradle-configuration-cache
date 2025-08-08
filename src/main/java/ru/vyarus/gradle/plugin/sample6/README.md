# Build service state

Sometimes, we need to collect some state during the configuration phase and use
it at runtime. As we have seen before, a [simple object](../sample2) (e.g. plugin field) could be used,
but we will lose uniqueness when configuration cache would be enabled.

In such cases build service could help, but not with dynamic state: 

* Build service state not persisted (neither fields nor updated parameters)
* Build service could call other build service
* Direct access to service from runtime will bring nothing when running from cache
* Variables and providers could be used to cache values (and solve state absence problem).

The last point is the main: in many cases we **don't need to somehow preserve state**
in build service - we just need to **let gradle cache it**.

## Service 1

[Service 1](Service1.java) use array in field and parameter (with custom object) to store some state.

Note that ListProperty can't be used in parameters, so have to use custom object to store list
in parameter.

`OperationCompletionListener` used only to prevent service closing (and looksing state early).

```java
public abstract class Service1 implements BuildService<Service1.Params>, OperationCompletionListener, AutoCloseable {

    private final List<String> state = new CopyOnWriteArrayList<>();

    public Service1() {
        System.out.println("Service 1 created: " + printState());
    }

    public List<String> getState() {
        return state;
    }

    public String printState() {
        return "state=" + state + ", property=" + getParameters().getListHolder().get();
    }

    // ATTENTION: if service state is populated under the configuration phase, then gradle almost
    //  certainly will close it before execution, so better to implement listener to prevent it
    public void addState(String value) {
        state.add(value);
        getParameters().getListHolder().get().add(value);
        System.out.println("Added value '" + value + "': " + printState());
    }

    @Override
    public void onFinish(FinishEvent finishEvent) {
        // only to prevent closing
    }

    @Override
    public void close() throws Exception {
        System.out.println("Service 1 closed");
    }

    interface Params extends BuildServiceParameters {
        // ListProperty will not work! Have to use wrapper object instead
        Property<ListHolder> getListHolder();
    }

    // used as persistent storage which will survive the configuration cache (because values would be appended inside
    // the configuration phase)
    public static class ListHolder implements Serializable {
        private final List<String> list = new CopyOnWriteArrayList<>();

        public List<String> getList() {
            return list;
        }

        public void add(String value) {
            list.add(value);
        }

        @Override
        public String toString() {
            return System.identityHashCode(this) + "@" + list;
        }
    }
}
```

## Service 2

[Service 2](Service2.java) only shows how to seference other service

```java
public abstract class Service2 implements BuildService<Service2.Params>, AutoCloseable {

    public Service2() {
        System.out.println("Service 2 created");
    }

    public void doAction() {
        // just show that service 1 could be called and it's state is correct
        System.out.println("Service 2 called Service 1 with state: "
                + getParameters().getService1().get().printState());
    }

    @Override
    public void close() throws Exception {
        System.out.println("Service 2 closed");
    }

    interface Params extends BuildServiceParameters {
        // this is the only way to reference other service
        Property<Service1> getService1();
    }
}
```

## Plugin

[Plugin](Sample6Plugin.java) configures both services, apply some state to the service (at configuration time)
and creates simple tasks, accessing this state in various ways.

```java
public abstract class Sample6Plugin implements Plugin<Project> {

    @Inject
    public abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    @Override
    public void apply(Project project) {
        // service 1 with "state" in parameter and in private field
        Provider<Service1> service1 = project.getGradle().getSharedServices().registerIfAbsent(
                "service1", Service1.class, spec -> {
                    // initial "persisted storage" value
                    spec.getParameters().getListHolder().set(new Service1.ListHolder());
                });
        // service 1 must not be closed; otherwise the stored (in it) configuration state would be lost
        getEventsListenerRegistry().onTaskCompletion(service1);

        // service 2 just with a link to service 1
        Provider<Service2> service2 = project.getGradle().getSharedServices().registerIfAbsent(
                "service2", Service2.class, spec -> {
                    // initial "persisted storage" value
                    spec.getParameters().getService1().set(service1);
                });
        // updating service state in configuration phase
        service1.get().addState("val1");

        project.getTasks().register("sample6Task", task -> {
            service1.get().addState("val2");
            // different state access at runtime: directly from service or with variable
            final Service1.ListHolder parameter = service1.get().getParameters().getListHolder().get();
            final List<String> state = service1.get().getState();
            Provider<List<String>> cachedState = project.provider(() -> service1.get().getState());
            // runtime: call service 2, which calls service 1 and prints state
            task.doLast(task1 -> {
                service2.get().doAction();
                System.out.println("Parameter: " + service1.get().getParameters().getListHolder().get());
                System.out.println("Parameter var: " + parameter);
                System.out.println("Direct state: " + service1.get().getState());
                System.out.println("Direct state var: " + state);
                System.out.println("Provider: " + cachedState.get());
            });
        });
    }
}
```

## Test

[Test](/src/test/java/ru/vyarus/gradle/plugin/sample6/Sample6PluginKitTest.java) would show that service state not preserved under configuration cache

### Configuration cache entry creation

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample6/Sample6PluginKitTest.java:L31) with cache enabled: `sample5Task --configuration-cache --configuration-cache-problems=warn`

```
Calculating task graph as no cached configuration is available for tasks: sample6Task

> Configure project :
Service 1 created: state=[], property=1782247779@[]
Added value 'val1': state=[val1], property=1782247779@[val1]
Added value 'val2': state=[val1, val2], property=1782247779@[val1, val2]

> Task :sample6Task
Service 2 created
Service 2 called Service 1 with state: state=[val1, val2], property=1782247779@[val1, val2]
Parameter: 1782247779@[val1, val2]
Parameter var: 27445508@[val1, val2]
Direct state: [val1, val2]
Direct state var: [val1, val2]
Provider: [val1, val2]
Service 1 closed
Service 2 closed

BUILD SUCCESSFUL in 3s
1 actionable task: 1 executed
Configuration cache entry stored.
```

As expected, state present everywhere.

### Run from cache 

[Run](/src/test/java/ru/vyarus/gradle/plugin/sample6/Sample6PluginKitTest.java:L45) with cache enabled: `sample5Task --configuration-cache --configuration-cache-problems=warn`

```
Reusing configuration cache.

> Task :sample6Task
Service 2 created
Service 1 created: state=[], property=1078300843@[]
Service 2 called Service 1 with state: state=[], property=1078300843@[]
Parameter: 1078300843@[]
Parameter var: 774569087@[val1, val2]
Direct state: []
Direct state var: [val1, val2]
Provider: [val1, val2]
Service 1 closed
Service 2 closed

BUILD SUCCESSFUL in 78ms
1 actionable task: 1 executed
Configuration cache entry reused.
```

Under cache, service created, but neither parameter, nor field preserved it's state
(parameters only preserve initial state, assigned on service registration).

Direct access to service is not cached, and so no surprise that, this time, state is empty inside task:

```
Parameter: 1078300843@[]
Direct state: []
```

But not state, stored in variable and provider:

```
Parameter var: 774569087@[val1, val2]
Direct state var: [val1, val2]
Provider: [val1, val2]
```

So, under configuration cache we may not need to recover service state, but just **let gradle 
to cache the correct value**!