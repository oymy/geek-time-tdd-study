package geektime.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by manyan.ouyang ON 2023/7/11
 */
public interface Context {

    Optional get(Ref ref);

    /**
     * Created by manyan.ouyang ON 2023/7/27
     */
    class Ref {
        private Type container;
        private final Class<?> component;

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

        Ref(Class<?> component) {
            this.component = component;
        }

        static Ref of(Type type) {
            if (type instanceof ParameterizedType parameterizedType) return new Ref(parameterizedType);
            return new Ref((Class<?>) type);
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
