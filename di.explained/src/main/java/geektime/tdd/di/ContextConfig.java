package geektime.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContextConfig {

    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, Function<ComponentProvider<?>, ComponentProvider<?>>> scopes = new HashMap<>();

    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    public <Type> void bind(Class<Type> type, Type instance) {

        components.put(new Component(type, null), context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(qualifier -> !qualifier.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), context -> instance);
        }

    }


    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.component().qualifier() != null) {
                    return Optional.ofNullable(components.get(ref.component()))
                            .map(provider -> (ComponentType) (provider.get(this)));

                }
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(
                            getScopeProvider(ref)).map(provider ->
                            (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getScopeProvider(ref))
                        .map(provider -> (ComponentType) (provider.get(this)));
            }


        };
    }

    private <ComponentType> ComponentProvider<?> getScopeProvider(ComponentRef<ComponentType> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component()))
                throw new DependencyNotFoundException(component, dependency.component());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) {
                    throw new CyclicDependenciesFoundException(visiting);
                }
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> pooledClass, Function<ComponentProvider<?>, ComponentProvider<?>> aNew) {

        scopes.put(pooledClass, aNew);

    }


    interface ComponentProvider<T> {
        T get(Context context);


        default List<ComponentRef<?>> getDependencies() {
            return List.of();
        }


    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        bind(type, implementation, implementation.getAnnotations());
    }


    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {

        if (Arrays.stream(annotations).anyMatch(qualifier ->
                !qualifier.annotationType().isAnnotationPresent(Qualifier.class)
                        && !qualifier.annotationType().isAnnotationPresent(Scope.class))) {
            throw new IllegalComponentException();
        }
        Optional<Annotation> scopeFromType = Arrays.stream(type.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();

        List<Annotation> qualifiers = Arrays.stream(annotations).filter(t -> t.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        Optional<Annotation> scope = Arrays.stream(annotations)
                .filter(a -> a.annotationType().isAnnotationPresent(Scope.class))
                .findFirst()
                .or(() -> scopeFromType);

        ComponentProvider<Implementation> injectionProvider = new InjectionProvider<>(implementation);
        ComponentProvider<Implementation> provider = scope.map(s -> (ComponentProvider<Implementation>) getScopeProvider(s, injectionProvider)).orElse(injectionProvider);


        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private ComponentProvider<?> getScopeProvider(Annotation scope, ComponentProvider<?> provider) {
        return scopes.get(scope.annotationType()).apply(provider);
    }


    static class SingletonProvider<T> implements ComponentProvider<T> {
        private T singleton;
        private final ComponentProvider<T> provider;

        public SingletonProvider(ComponentProvider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get(Context context) {
            if (singleton == null) singleton = provider.get(context);
            return singleton;
        }

        @Override
        public List<ComponentRef<?>> getDependencies() {
            return provider.getDependencies();
        }

    }

}
