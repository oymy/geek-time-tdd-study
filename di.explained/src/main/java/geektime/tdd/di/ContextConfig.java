package geektime.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContextConfig {

    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {

        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (Type) provider.get(this));
            }

            @Override
            public Optional get(ParameterizedType type) {
                if(type.getRawType() != Provider.class) return Optional.empty();
                return Optional.ofNullable(
                        providers.get(type.getActualTypeArguments()[0])).map(provider ->
                        (Provider<Object>) () -> provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency))
                throw new DependencyNotFoundException(dependency, component);
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

        default List<Class<?>> getDependencies() {
            return List.of();
        }
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {

        providers.put(type, new InjectionProvider<>(implementation));

    }

}
