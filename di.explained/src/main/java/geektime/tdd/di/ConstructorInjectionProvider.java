package geektime.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.util.Arrays.stream;

/**
 * Created by manyan.ouyang ON 2023/7/10
 */
class ConstructorInjectionProvider<T> implements Provider<T> {
    private final Context context;

    private final Class<?> componentType;
    private final Constructor<T> injectConstructor;
    private boolean constructing = false;

    public ConstructorInjectionProvider(Context context, Class<?> componentType, Constructor<T> injectConstructor) {
        this.context = context;
        this.componentType = componentType;
        this.injectConstructor = injectConstructor;
    }

    @Override
    public T get() {
        if (constructing) throw new CyclicDependenciesFoundException(componentType);
        try {
            constructing = true;
            Object[] dependencies = stream(injectConstructor.getParameters())
                    .map(p -> context.get(p.getType()).orElseThrow(() -> new DependencyNotFoundException(p.getType(), this.componentType))).toArray(Object[]::new);

            return injectConstructor.newInstance(dependencies);
        } catch (CyclicDependenciesFoundException e) {
            throw new CyclicDependenciesFoundException(componentType, e);
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            constructing = false;
        }
    }
}
