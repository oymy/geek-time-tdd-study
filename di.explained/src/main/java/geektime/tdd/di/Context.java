package geektime.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by manyan.ouyang ON 2023/7/11
 */
public interface Context {

    <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref);

    /**
     * Created by manyan.ouyang ON 2023/7/27
     */
    class Ref<ComponentType> {

        public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component) {
            return new Ref<>(component);
        }
        private Type container;
        Class<?> component;

        public Type getContainer() {
            return container;
        }

        public Class<?> getComponent() {
            return component;
        }

        Ref(ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        }

        Ref(Type type) {
            init(type);
        }

        Ref(Class<?> component) {
            init(component);
        }

        protected Ref() {
            Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            init(type);
        }

        private void init(Type type) {
            if(type instanceof ParameterizedType parameterizedType) {
                this.container = parameterizedType.getRawType();
                this.component = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            } else {
                this.component = (Class<?>) type;
            }
        }

        static Ref of(Type type) {
            return new Ref(type);
        }

        public boolean isContainer() {
            return container != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Ref ref)) return false;
            return Objects.equals(isContainer(), ref.isContainer()) && Objects.equals(getComponent(), ref.getComponent());
        }

        @Override
        public int hashCode() {
            return Objects.hash(isContainer(), getComponent());
        }

    }
}
