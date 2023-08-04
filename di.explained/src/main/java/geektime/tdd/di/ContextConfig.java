package geektime.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContextConfig {

    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

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
                            getProvider(ref)).map(provider ->
                            (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getProvider(ref))
                        .map(provider -> (ComponentType) (provider.get(this)));
            }


        };
    }

    private <ComponentType> ComponentProvider<?> getProvider(ComponentRef<ComponentType> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Stack<Class<?>> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component()))
                throw new DependencyNotFoundException(dependency.getComponentType(), component.type());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponentType())) {
                    throw new CyclicDependenciesFoundException(visiting);
                }
                visiting.push(dependency.getComponentType());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }


    interface ComponentProvider<T> {
        T get(Context context);


        default List<ComponentRef> getDependencies() {
            return List.of();
        }


    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        components.put(new Component(type, null), new InjectionProvider<>(implementation));
    }


    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... qualifiers) {

        if (Arrays.stream(qualifiers).anyMatch(qualifier -> !qualifier.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
        }
    }

}
