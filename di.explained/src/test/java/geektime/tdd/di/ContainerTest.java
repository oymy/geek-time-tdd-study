package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContainerTest {


    ContextConfig contextConfig;

    @BeforeEach
    public void setup() {
        contextConfig = new ContextConfig();

    }

    @Nested
    public class ComponentConstruction {
        //TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            contextConfig.bind(Component.class, instance);
            final Context context = contextConfig.getContext();
            assertSame(instance, context.get(Component.class).get());


        }

        //TODO: abstract class
        //TODO: interface


        @Test
        public void should_return_empty_if_component_undefined() {
            Optional<Component> component = contextConfig.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }


        @Nested
        public class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                contextConfig.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = contextConfig.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                Dependency dependency = new Dependency() {
                };
                contextConfig.bind(Dependency.class, dependency);
                Component instance = contextConfig.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());

            }

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
                contextConfig.bind(String.class, "indirect dependency");

                Component instance = contextConfig.getContext().get(Component.class).get();
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());


            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> contextConfig.bind(Component.class, ComponentWithMultiInjectConstructors.class));
            }

            @Test
            public void should_throw_no_inject_constructor_or_default_constructor_found() {
                assertThrows(IllegalComponentException.class, () -> contextConfig.bind(Component.class, ComponentWithoutInjectConstructor.class));
            }


            @Test
            public void should_throw_exception_if_dependency_not_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                final DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                        () -> contextConfig.getContext());
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
                DependencyNotFoundException exception =
                        assertThrows(DependencyNotFoundException.class, () -> contextConfig.getContext());
                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());

            }

            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyDependOnComponent.class);
                final CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> contextConfig.getContext());
                Set<Class<?>> classes = Sets.newSet(exception.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue((classes.contains(Dependency.class)));


            }

            @Test // A->B->C->A
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
                contextConfig.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);
                final CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> contextConfig.getContext());

                List<Class<?>> components = Arrays.asList(exception.getComponents());

                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));

            }

        }

        @Nested
        public class FieldInjection {

        }

        @Nested
        public class MethodInjection {

        }

    }

    @Nested
    public class DependenciesSelection {

    }


    @Nested
    public class LifecycleManagement {

    }

}

interface Component {

}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }

}


interface Dependency {

}

interface AnotherDependency {

}

class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    public Dependency getDependency() {
        return dependency;
    }

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

}

class DependencyWithInjectConstructor implements Dependency {
    public String getDependency() {
        return dependency;
    }

    String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class ComponentWithoutInjectConstructor implements Component {
    public ComponentWithoutInjectConstructor(String name) {
    }
}

class DependencyDependOnComponent implements Dependency {
    private final Component component;

    @Inject
    public DependencyDependOnComponent(Component component) {
        this.component = component;
    }
}

class AnotherDependencyDependOnComponent implements AnotherDependency {
    private final Component component;

    @Inject
    public AnotherDependencyDependOnComponent(Component component) {
        this.component = component;
    }
}

class DependencyDependOnAnotherDependency implements Dependency {
    private final AnotherDependency anotherDependency;

    @Inject
    public DependencyDependOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}