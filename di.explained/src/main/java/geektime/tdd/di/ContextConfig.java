package geektime.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContextConfig {

    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private final Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {

        providers.put(type, context -> instance);
        dependencies.put(type, asList());
    }

    public Context getContext() {
        dependencies.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (Type) provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for(Class<?> dependency : dependencies.get(component)) {
            if (!dependencies.containsKey(dependency))
                throw new DependencyNotFoundException( dependency, component );
            if (visiting.contains(dependency)) {
                throw new CyclicDependenciesFoundException(visiting);
            }
            visiting.push(dependency);
            checkDependencies(dependency, visiting);
            visiting.pop();
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider<>(injectConstructor));

        dependencies.put(type, stream(injectConstructor.getParameters()).map(
                Parameter::getType
        ).collect(Collectors.toList()));

    }

    private <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(
                c -> c.isAnnotationPresent(Inject.class)
        ).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }
}
