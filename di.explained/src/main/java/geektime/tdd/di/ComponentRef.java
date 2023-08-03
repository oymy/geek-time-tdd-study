package geektime.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Created by manyan.ouyang ON 2023/7/27
 */
public class ComponentRef<ComponentType> {

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef<>(component);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef<>(component, qualifier);
    }

    private Type container;

    private Component component;


    public Type getContainer() {
        return container;
    }

    public Class<?> getComponentType() {
        return component.type();
    }

    ComponentRef(ParameterizedType container) {
        init(container, null);
    }

    ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    ComponentRef(Class<?> componentType) {
        init(componentType, null);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, null);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType parameterizedType) {
            this.container = parameterizedType.getRawType();
            this.component = new Component((Class<?>) (parameterizedType.getActualTypeArguments()[0]), qualifier);
        } else {
            this.component = new Component((Class<?>) type, qualifier);
        }
    }

    static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    public boolean isContainer() {
        return container != null;
    }


    public Component component() {
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentRef<?> that)) return false;
        return Objects.equals(isContainer(), that.isContainer()) && Objects.equals(component, that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isContainer(), component);
    }
}
