package geektime.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContextConfig {

    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {

        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    private boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    private Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            private <Type> Optional<Type> getComponent(Class<Type> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (Type) provider.get(this));
            }

            @Override
            public Optional get(Type type) {
                if (isContainerType(type)) return getContainer((ParameterizedType) type);
                return getComponent((Class<?>) type);
            }


            private Optional getContainer(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                Class<?> componentType = getComponentType(type);
                return Optional.ofNullable(
                        providers.get(componentType)).map(provider ->
                        (Provider<Object>) () -> provider.get(this));
            }


        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {

            if (isContainerType(dependency)) {
                checkContainerTypeDependency(component, dependency);
            } else {
                checkComponentDependency(component, visiting, (Class<?>) dependency);
            }
        }
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        Class<?> type = getComponentType(dependency);

        if (!providers.containsKey(type))
            throw new DependencyNotFoundException(type, component);
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency))
            throw new DependencyNotFoundException(dependency, component);
        if (visiting.contains(dependency)) {
            throw new CyclicDependenciesFoundException(visiting);
        }
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencies() {
            return List.of();
        }
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {

        providers.put(type, new InjectionProvider<>(implementation));

    }

}
