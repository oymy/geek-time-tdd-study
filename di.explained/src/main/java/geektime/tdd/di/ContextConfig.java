package geektime.tdd.di;

import java.util.*;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContextConfig {

    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {

        providers.put(type, new ComponentProvider<Type>() {
            @Override
            public Type get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return List.of();
            }
        });
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (Type) provider.get(this));
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

        List<Class<?>> getDependencies();
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {

        providers.put(type, new ConstructorInjectionProvider<>(implementation));

    }

}
