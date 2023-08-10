package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

/**
 * Created by manyan.ouyang ON 2023/7/10
 */
class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {

    private final List<ComponentRef<?>> dependencies;

    private final Injectable<Constructor<T>> injectConstructor;
    private final List<Injectable<Method>> injectMethods;

    private final List<Injectable<Field>> injectFields;

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return concat(concat(stream(injectConstructor.required()),
                        injectFields.stream().flatMap(f -> stream(f.required()))),
                injectMethods.stream().flatMap(m -> stream(m.required()))).toList();
    }

    private static ComponentRef toComponentRef(Field field) {
        Annotation qualifier = getQualifier(field);
        return ComponentRef.of(field.getGenericType(), qualifier);
    }

    private static ComponentRef<?> toComponentRef(Parameter parameter) {
        Annotation qualifier = getQualifier(parameter);
        return ComponentRef.of(parameter.getParameterizedType(), qualifier);

    }

    private static Annotation getQualifier(AnnotatedElement ae) {
        List<Annotation> list = stream(ae.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        if (list.size() > 1) throw new IllegalComponentException();
        return list.isEmpty() ? null : list.get(0);
    }


    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers()))
            throw new IllegalComponentException();

        Constructor<T> constructor = getInjectConstructor(component);


        this.injectConstructor = Injectable.of(constructor);

        List<Field> injectFields1 = getInjectFields(component);
        this.injectFields = getInjectFields(component).stream().map(Injectable::of).toList();

        this.injectMethods = getInjectMethods(component).stream().map(Injectable::of).toList();

        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.element().getModifiers())))
            throw new IllegalComponentException();

        if (injectMethods.stream().map(m -> m.element).anyMatch(m -> m.getTypeParameters().length != 0))
            throw new IllegalComponentException();

        this.dependencies = getDependencies();
    }


    private static <T> List<Method> getInjectMethods(Class<T> component) {
        final List<Method> injectMethods = traverse(component, (methods, current) ->
                injectable(current.getDeclaredMethods()).filter(m -> isOverrideByInjectMethod(methods, m))
                        .filter(m -> isOverrideByNoInjectMethod(component, m))
                        .toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(finder.apply(injectMethods, current));
            current = current.getSuperclass();
        }
        return injectMethods;
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(
                        m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(m1 -> isOverridden(m, m1));
    }

    private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverridden(m, o));
    }

    private static boolean isOverridden(Method m, Method o) {
        return o.getName().equals(m.getName()) &&
                Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
    }


    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(
                f -> f.isAnnotationPresent(Inject.class)
        );
    }

    static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();

        return (Constructor<Type>) injectConstructors.stream()
                .findFirst()
                .orElseGet(() -> defaultConstructor(implementation));
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }


    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.element.newInstance(injectConstructor.toDependencies(context));
            for (Injectable<Field> field : injectFields) {
                field.element().set(instance, field.toDependencies(context)[0]);
            }
            for (Injectable<Method> method : injectMethods) {
                method.element.invoke(instance, method.toDependencies(context));
            }

            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, field.getGenericType(), getQualifier(field));
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameters()).map(
                p -> toDependency(context, p.getParameterizedType(), getQualifier(p))
        ).toArray();
    }

    private static Object toDependency(Context context, Type type, Annotation qualifier) {
        return context.get(ComponentRef.of(type, qualifier)).get();
    }

    record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {
        private static <Element extends Executable> Injectable<Element> of(Element constructor) {
            return new Injectable<>(constructor, stream(constructor.getParameters())
                    .map(InjectionProvider::toComponentRef)
                    .toArray(ComponentRef<?>[]::new));
        }


        private static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef<?>[]{toComponentRef(field)});
        }

        Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }
    }
}
