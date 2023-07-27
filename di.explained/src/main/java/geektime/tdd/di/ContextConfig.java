package geektime.tdd.di;

import jakarta.inject.Provider;

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


    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional<?> get(Ref ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return Optional.ofNullable(
                            providers.get(ref.getComponent())).map(provider ->
                            (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent()))
                        .map(provider -> provider.get(this));
            }


        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Context.Ref dependency : providers.get(component).getDependencyRefs()) {
            if (!providers.containsKey(dependency.getComponent()))
                throw new DependencyNotFoundException(dependency.getComponent(), component);
            if (!dependency.isContainer()) {
                if (!providers.containsKey(dependency.getComponent()))
                    throw new DependencyNotFoundException(dependency.getComponent(), component);
                if (visiting.contains(dependency.getComponent())) {
                    throw new CyclicDependenciesFoundException(visiting);
                }
                visiting.push(dependency.getComponent());
                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);


        default List<Context.Ref> getDependencyRefs() {
            return getDependencies().stream().map(Context.Ref::of).toList();
        }

        default List<Type> getDependencies() {
            return List.of();
        }
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {

        providers.put(type, new InjectionProvider<>(implementation));

    }

}
