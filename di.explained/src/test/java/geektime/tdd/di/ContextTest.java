package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
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


    public static final SkyWalkerLiteral SKY_WALKER_LITERAL = new SkyWalkerLiteral();
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();

    }


    @Nested
    class TypeBinding {

        @Test
        void should_bind_type_to_a_specific_instance() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            final Context context = config.getContext();
            assertSame(instance, context.get(ComponentRef.of(TestComponent.class)).get());


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
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        //TODO could get Provider<T> from context
        @Test
        void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();

            final Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {
            }).get();

            assertSame(instance, provider.get());

        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();

            assertFalse(context.get(new ComponentRef<List<TestComponent>>() {
            }).isPresent());


        }

        @Nested
        class WithQualifier {

            @Test
            void should_bind_instance_with_multi_qualifiers() {
                TestComponent instance = new TestComponent() {
                };

                config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"), SKY_WALKER_LITERAL);
                Context context = config.getContext();
                TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, SKY_WALKER_LITERAL)).get();
                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);

            }

            @Test
            void should_bind_component_with_multi_qualifiers() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                config.bind(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        new NamedLiteral("ChosenOne"), SKY_WALKER_LITERAL);
                Context context = config.getContext();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor chosenOne = context.get(ComponentRef.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor skywalker = context.get(ComponentRef.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, SKY_WALKER_LITERAL)).get();
                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skywalker.dependency);

            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent instance = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class,
                        () -> config.bind(TestComponent.class, instance, new AbcLiteral()));

            }

            //TODO Provider

        }

    }

    @Nested
    class DependencyCheck {
        @Test
        void should_throw_exception_if_dependency_not_found() {
            config.bind(TestComponent.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class);
            final DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> config.getContext());
            assertEquals(Dependency.class, exception.getDependency().type());
            assertEquals(TestComponent.class, exception.getComponent().type());
        }


        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        @Test
        void should_throw_exception_if_cyclic_dependencies_found() {
            config.bind(TestComponent.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class);
            config.bind(Dependency.class, DependencyDependOnComponent.class);
            final CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue((classes.contains(Dependency.class)));


        }

        @Test
            // A->B->C->A
        void should_throw_exception_if_transitive_cyclic_dependencies_found() {
            config.bind(TestComponent.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class);
            config.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);
            final CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

            List<Class<?>> components = Arrays.asList(exception.getComponents());

            assertEquals(3, components.size());
            assertTrue(components.contains(TestComponent.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));


        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
            }
        }

        static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            final Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());

        }

        @Nested
        class WithQualifier {

            @Test
            void should_throw_exception_if_dependency_with_qualifier_not_found() {
                config.bind(Dependency.class, new Dependency() {
                });
                config.bind(InjectConstructor.class, InjectConstructor.class, new NamedLiteral("Owner"));
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(new Component(InjectConstructor.class, new NamedLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkyWalkerLiteral()), exception.getDependency());

            }

            static class InjectConstructor {
                @Inject
                public InjectConstructor(@SkyWalker Dependency dependency) {
                }
            }

            //TODO check cyclic dependencies with qualifier
            // A -> @Skywalker A -> @Name A(instance)

            static class SkywalkerDependency implements Dependency {
                @Inject
                public SkywalkerDependency(@Named("ChosenOne") Dependency dependency) {
                }
            }

            static class NotCyclicDependency implements  Dependency {
                @Inject
                public NotCyclicDependency(@SkyWalker Dependency dependency) {
                }

            }

            @Test
            void should_not_throw_cyclic_exception_if_component_with_same_type_taged_with_different_qualifier() {
                Dependency instance = new Dependency() {
                };
                config.bind(Dependency.class, instance, new NamedLiteral("ChosenOne"));
                config.bind(Dependency.class, SkywalkerDependency.class , new SkyWalkerLiteral());
                config.bind(Dependency.class, NotCyclicDependency.class);

                assertDoesNotThrow(() -> config.getContext());

            }
        }

    }

}

record NamedLiteral(String value) implements Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Named) {
            return value.equals(((Named) obj).value());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return"value".hashCode() * 127 ^ value.hashCode();
    }
}


@java.lang.annotation.Documented
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
@jakarta.inject.Qualifier
@interface SkyWalker {
}

record SkyWalkerLiteral() implements SkyWalker {

    @Override
    public Class<? extends Annotation> annotationType() {
        return SkyWalker.class;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof  SkyWalker;
    }
}


@java.lang.annotation.Documented
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
@interface Abc {
}

record AbcLiteral() implements Abc {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Abc.class;
    }
}