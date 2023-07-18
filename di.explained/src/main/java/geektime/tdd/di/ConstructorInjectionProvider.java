package geektime.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

/**
 * Created by manyan.ouyang ON 2023/7/10
 */
class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {

    private final Constructor<T> injectConstructor;

    private final List<Field> injectFields;
    private final List<Method> injectMethods;


    public ConstructorInjectionProvider(Class<T> component) {
        if(Modifier.isAbstract(component.getModifiers()))
            throw new IllegalComponentException();
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);

        if(injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers())))
            throw new IllegalComponentException();

        if(injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0))
            throw new IllegalComponentException();
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(stream(current.getDeclaredMethods()).filter(
                            m -> m.isAnnotationPresent(Inject.class)
                    ).filter(m -> injectMethods.stream().noneMatch(o ->
                            o.getName().equals(m.getName()) &&
                                    Arrays.equals(o.getParameterTypes(), m.getParameterTypes())
                    ))
                    .filter(m ->
                            stream(component.getDeclaredMethods()).filter(
                                            m1 -> !m1.isAnnotationPresent(Inject.class))
                                    .noneMatch(m1 -> m1.getName().equals(m.getName()) &&
                                            Arrays.equals(m1.getParameterTypes(), m.getParameterTypes()
                                            )))
                    .collect(Collectors.toList()));
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        List<Field> injectFields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectFields.addAll(stream(current.getDeclaredFields()).filter(
                    f -> f.isAnnotationPresent(Inject.class)
            ).collect(Collectors.toList()));
            current = current.getSuperclass();
        }
        return injectFields;
    }

    static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(
                c -> c.isAnnotationPresent(Inject.class)
        ).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }


    @Override
    public T get(Context context) {
        try {
            Object[] dependencies = stream(injectConstructor.getParameters())
                    .map(p -> {
                        Class<?> type = p.getType();
                        return context.get(type).get();
                    }).toArray(Object[]::new);
            T instance = injectConstructor.newInstance(dependencies);
            for (Field field : injectFields) {
                field.set(instance, context.get(field.getType()).get());
            }
            for (Method method : injectMethods) {
                List<Object> objects = new ArrayList<>();
                objects.addAll(stream(method.getParameterTypes()).map(
                                it -> context.get(it).get()
                        ).toList()
                );
                method.invoke(instance, objects.toArray());
            }

            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(Stream.concat(
                        stream(injectConstructor.getParameters()).map(Parameter::getType),
                        injectFields.stream().map(Field::getType)),
                injectMethods.stream().flatMap(m -> stream(m.getParameterTypes()))
        ).toList();
    }
}
