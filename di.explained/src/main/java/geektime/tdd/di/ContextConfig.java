package geektime.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContextConfig {

    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

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

    public <ScopeType extends Annotation> void scope(Class<ScopeType> pooledClass, ScopeProvider aNew) {
        scopes.put(pooledClass, aNew);

    }


    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        bind(type, implementation, implementation.getAnnotations());
    }


    private Class<?> typeOf(Annotation annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        return Stream.of(Qualifier.class, Scope.class).filter(type::isAnnotationPresent).findFirst().orElse(Illegal.class);
    }

    private @interface Illegal {
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {

        Map<Class<?>, List<Annotation>> annotationGroup = Arrays.stream(annotations).collect(Collectors.groupingBy(this::typeOf, Collectors.toList()));
        if (annotationGroup.containsKey(Illegal.class)) {
            throw new IllegalComponentException();
        }

        bind(type, annotationGroup.getOrDefault(Qualifier.class, List.of()),
                createScopeProvider(implementation, annotationGroup.getOrDefault(Scope.class, List.of())));
    }

    private <Type> ComponentProvider<?> createScopeProvider(Class<Type> implementation, List<Annotation> scopes) {
        if (scopes.size() > 1) throw new IllegalComponentException();
        ComponentProvider<?> injectionProvider = new InjectionProvider<>(implementation);
        return scopes.stream()
                .findFirst()
                .or(() -> scopeFrom(implementation))
                .map(s -> (ComponentProvider) getScopeProvider(s, injectionProvider))
                .orElse(injectionProvider);
    }

    private <Type> void bind(Class<Type> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private static <Type> Optional<Annotation> scopeFrom(Class<Type> implementation) {
        return Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();
    }

    private ComponentProvider<?> getScopeProvider(Annotation scope, ComponentProvider<?> provider) {
        if (!scopes.containsKey(scope.annotationType())) throw new IllegalComponentException();
        return scopes.get(scope.annotationType()).create(provider);
    }


}
