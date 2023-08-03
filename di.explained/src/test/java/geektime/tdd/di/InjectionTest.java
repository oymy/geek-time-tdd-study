package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by manyan.ouyang ON 2023/7/18
 */
@Nested
public class InjectionTest {

    // InjectionProvider
    private final Dependency dependency = mock(Dependency.class);
    private final Provider<Dependency> dependencyProvider = mock(Provider.class);

    private ParameterizedType dependencyProviderType;

    private final Context context = mock(Context.class);

    public InjectionTest() {
    }

    @BeforeEach
    public void setup() throws NoSuchFieldException {
        dependencyProviderType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();

        when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));

        when(context.get(eq(ComponentRef.of(dependencyProviderType)))).thenReturn(Optional.of(dependencyProvider));

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
                        new InjectionProvider<>(InjectMethodWithoutDependency.class).get(context);
                assertTrue(component.isInjected);

            }

            @Test
            void should_call_inject_method_with_dependencies() {
                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);
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
            void should_inject_dependencies_via_inject_method_from_superclass() {
                SubclassWithInjectMethod component =
                        new InjectionProvider<>(SubclassWithInjectMethod.class).get(context);
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
                        new InjectionProvider<>(SubclassOverrideSuperClassWithInject.class).get(context);
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
                        new InjectionProvider<>(SubclassOverrideSuperClassWithNoInject.class).get(context);
                assertEquals(0, component.superCalled);
            }

            @Test
            void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }

            @Test
            void should_include_dependency_type_from_inject_methods() {
                final InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(
                        new ComponentRef[]{ComponentRef.of(dependencyProviderType)},
                        provider.getDependencies().toArray()
                );
            }

            static class ProviderInjectMethod {
                Provider<Dependency> dependency;

                @Inject
                public void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_provider_via_method() {
                ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
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
                        new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }


        }

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
            void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component =
                        new InjectionProvider<>(ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);

            }

            @Test
            void should_inject_dependency_via_superclass_inject_field() {
                ComponentWithFieldInjection component =
                        new InjectionProvider<>(SubClassWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);

            }

            @Test
            void should_include_dependency_type_from_inject_fields() {
                final InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(
                        new ComponentRef[]{ComponentRef.of(dependencyProviderType)},
                        provider.getDependencies().toArray()
                );
            }
            @Test
            void should_include_dependency_from_field_dependencies() {
                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());

            }

            static class ProviderInjectField {
                @Inject
                Provider<Dependency> dependency;
            }

            @Test
            void should_inject_provider_via_method() {
                ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

        }

        @Nested
        class IllegalInjectFields {

            @Test
            void should_throw_exception_when_field_dependency_missing() {
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
                ComponentWithDefaultConstructor instance = new InjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            static class InjectConstructor implements TestComponent {
                Dependency dependency;


                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }

            }

            @Test
            void should_inject_dependency_via_inject_constructor() {
                InjectConstructor instance = new InjectionProvider<>(InjectConstructor.class).get(context);
                assertNotNull(instance);
                assertSame(dependency, instance.dependency);

            }

            @Test
            void should_include_dependency_from_inject_constructor() {
                final InjectionProvider<InjectConstructor> provider
                        = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(
                        new ComponentRef[]{ComponentRef.of(Dependency.class)},
                        provider.getDependencies().toArray()
                );
            }


            @Test
            void should_include_dependency_type_from_inject_constructor() {
                final InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                assertArrayEquals(
                        new ComponentRef[]{ComponentRef.of(dependencyProviderType)},
                        provider.getDependencies().toArray()
                );

            }


            static class ProviderInjectConstructor {
                Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_provider_via_constructor() {
                ProviderInjectConstructor instance = new InjectionProvider<>(ProviderInjectConstructor.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

        }

        @Nested
        class IllegalInjectConstructor {

            abstract class AbstractComponent implements TestComponent {
                @Inject
                public AbstractComponent() {
                }
            }

            @Test
            void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(AbstractComponent.class));
            }

            @Test
            void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(TestComponent.class));
            }

            @Test
            void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(ComponentWithMultiInjectConstructors.class)
                );
            }

            @Test
            void should_throw_no_inject_constructor_or_default_constructor_found() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(ComponentWithoutInjectConstructor.class)
                );
            }


        }

    }

}

class ComponentWithDefaultConstructor implements TestComponent {
    public ComponentWithDefaultConstructor() {
    }

}

class ComponentWithMultiInjectConstructors implements TestComponent {
    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class ComponentWithoutInjectConstructor implements TestComponent {
    public ComponentWithoutInjectConstructor(String name) {
    }
}

