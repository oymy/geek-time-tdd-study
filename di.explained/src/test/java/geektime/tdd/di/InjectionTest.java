package geektime.tdd.di;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by manyan.ouyang ON 2023/7/18
 */
@Nested
public class InjectionTest {

    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();

    }

    @Nested
    public class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            config.bind(Component.class, ComponentWithDefaultConstructor.class);
            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);
            assertTrue(instance instanceof ComponentWithDefaultConstructor);
        }

        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());

        }

        @Test
        public void should_bind_type_to_a_class_with_transitive_dependencies() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            config.bind(String.class, "indirect dependency");

            Component instance = config.getContext().get(Component.class).get();
            assertNotNull(instance);
            Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
            assertNotNull(dependency);
            assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());


        }


        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ContainerTest.ComponentConstruction.AbstractComponent.class));
        }

        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(Component.class));
        }

        @Test
        public void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class, () ->
                    new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class)
            );
        }

        @Test
        public void should_throw_no_inject_constructor_or_default_constructor_found() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ComponentWithoutInjectConstructor.class)
            );
        }

        @Test
        public void should_include_dependency_from_inject_constructor() {
            final ConstructorInjectionProvider<ComponentWithInjectConstructor> provider
                    = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
            assertArrayEquals(
                    new Class<?>[]{Dependency.class},
                    provider.getDependencies().toArray()
            );
        }


    }

}
