package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by manyan.ouyang ON 2023/7/24
 */
@Nested
class ContextTest {


    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();

    }


    @Nested
    public class TypeBinding {

        @Test
        void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            final Context context = config.getContext();
            assertSame(instance, context.get(Context.Ref.of(Component.class)).get());


        }


        //TODO throw exception if field is final
        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        void should_throw_exception_if_field_is_final() {
            assertThrows(IllegalComponentException.class,
                    () -> new InjectionProvider<>(TypeBinding.FinalInjectField.class));
        }


        @Test
        void should_return_empty_if_component_undefined() {
            Context context = config.getContext();
            Optional<Component> component = context.get(Context.Ref.of(Component.class));
            assertTrue(component.isEmpty());
        }

        //TODO could get Provider<T> from context
        @Test
        void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            final Provider<Component> provider = context.get(new Context.Ref<Provider<Component>>() {
            }).get();

            assertSame(instance, provider.get());

        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            assertFalse(context.get(new Context.Ref<List<Component>>() {
            }).isEmpty());


        }

    }

    @Nested
    class DependencyCheck {
        @Test
        void should_throw_exception_if_dependency_not_found() {
            config.bind(Component.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class);
            final DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> config.getContext());
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }


        static class MissingDependencyProviderConstructor implements Component {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        @Test
        void should_throw_exception_if_cyclic_dependencies_found() {
            config.bind(Component.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class);
            config.bind(Dependency.class, DependencyDependOnComponent.class);
            final CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue((classes.contains(Dependency.class)));


        }

        @Test
            // A->B->C->A
        void should_throw_exception_if_transitive_cyclic_dependencies_found() {
            config.bind(Component.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class);
            config.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);
            final CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

            List<Class<?>> components = Arrays.asList(exception.getComponents());

            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));


        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<Component> component) {
            }
        }

        static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(Component.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            final Context context = config.getContext();
            assertTrue(context.get(Context.Ref.of(Component.class)).isPresent());

        }

    }

}
