package geektime.tdd.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.util.Arrays.stream;

/**
 * Created by manyan.ouyang ON 2023/7/10
 */
class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {

    private final Constructor<T> injectConstructor;

    public ConstructorInjectionProvider(Constructor<T> injectConstructor) {
        this.injectConstructor = injectConstructor;
    }


    @Override
    public T get(Context context) {
        try {
            Object[] dependencies = stream(injectConstructor.getParameters())
                    .map(p -> {
                        Class<?> type = p.getType();
                        return context.get(type).get();
                    }).toArray(Object[]::new);

            return injectConstructor.newInstance(dependencies);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
