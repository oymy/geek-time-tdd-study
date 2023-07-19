package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Created by manyan.ouyang ON 2023/7/18
 */
@Nested
public class InjectionTest {

    private final Dependency dependency = Mockito.mock(Dependency.class);

    private final Context context = Mockito.mock(Context.class);

    @BeforeEach
    public void setup() {
        Mockito.when(context.get(eq(Dependency.class)))
                .thenReturn(Optional.of(dependency));

    }

    @Nested
    public class MethodInjection {

        @Nested
        class Injection {

            public static class InjectMethodWithoutDependency {

                private boolean isInjected = false;

                @Inject
                public void install() {
                    isInjected = true;
                }
            }

            public static class InjectMethodWithDependency {
                private Dependency dependency;

                @Inject
                public void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }


            @Test
            void should_call_inject_method_with_no_dependencies() {
                InjectMethodWithoutDependency component =
                        new ConstructorInjectionProvider<>(InjectMethodWithoutDependency.class).get(context);
                assertTrue(component.isInjected);

            }

            @Test
            public void should_call_inject_method_with_dependencies() {
                InjectMethodWithDependency component = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class).get(context);
                assertSame(dependency, component.dependency);

            }

            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;

                }
            }

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;

                }

            }


            @Test
            public void should_inject_dependencies_via_inject_method_from_superclass() {
                SubclassWithInjectMethod component =
                        new ConstructorInjectionProvider<>(SubclassWithInjectMethod.class).get(context);
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);


            }

            static class SubclassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            void should_only_call_once_if_subclass_override_inject_method_with_inject() {
                SubclassOverrideSuperClassWithInject component =
                        new ConstructorInjectionProvider<>(SubclassOverrideSuperClassWithInject.class).get(context);
                assertEquals(1, component.superCalled);
            }

            static class SubclassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            void should_not_call_inject_method_if_override_with_no_inject() {
                SubclassOverrideSuperClassWithNoInject component =
                        new ConstructorInjectionProvider<>(SubclassOverrideSuperClassWithNoInject.class).get(context);
                assertEquals(0, component.superCalled);
            }

            @Test
            void should_include_dependencies_from_inject_method() {
                ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
            }

        }

        @Nested
        class IllegalInjectMethods {
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {

                }
            }

            @Test
            void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () ->
                        new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
            }


        }

        //TODO throw exception if type parameter defined

    }

    @Nested
    public class FieldInjection {

        @Nested
        class Injection {
            public static class ComponentWithFieldInjection {
                @Inject
                public Dependency dependency;
            }

            static class SubClassWithFieldInjection extends ComponentWithFieldInjection {

            }

            @Test
            public void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component =
                        new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);

            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                ComponentWithFieldInjection component =
                        new ConstructorInjectionProvider<>(SubClassWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);

            }

            @Test
            void should_include_field_dependency_from_field_dependencies() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());

            }


        }

        @Nested
        class IllegalInjectFields {

            @Test
            public void should_throw_exception_when_field_dependency_missing() {
                ContextConfig config = new ContextConfig();
                config.bind(Injection.ComponentWithFieldInjection.class, Injection.ComponentWithFieldInjection.class);
                assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            }

        }


    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {
            @Test
            void should_call_default_constructor_if_no_inject_constructor() {
                ComponentWithDefaultConstructor instance = new ConstructorInjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            static class InjectConstructor implements Component {
                Dependency dependency;


                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }

            }

            @Test
            void should_inject_dependency_via_inject_constructor() {
                InjectConstructor instance = new ConstructorInjectionProvider<>(InjectConstructor.class).get(context);
                assertNotNull(instance);
                assertSame(dependency, instance.dependency);

            }

            @Test
            void should_include_dependency_from_inject_constructor() {
                final ConstructorInjectionProvider<InjectConstructor> provider
                        = new ConstructorInjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(
                        new Class<?>[]{Dependency.class},
                        provider.getDependencies().toArray()
                );
            }

        }

        @Nested
        class IllegalInjectConstructor {
            @Test
            void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(ContainerTest.ComponentConstruction.AbstractComponent.class));
            }

            @Test
            void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(Component.class));
            }

            @Test
            void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class)
                );
            }

            @Test
            void should_throw_no_inject_constructor_or_default_constructor_found() {
                assertThrows(IllegalComponentException.class,
                        () -> new ConstructorInjectionProvider<>(ComponentWithoutInjectConstructor.class)
                );
            }

        }

    }

}
