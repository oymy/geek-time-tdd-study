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
import static org.mockito.ArgumentMatchers.eq;

/**
 * Created by manyan.ouyang ON 2023/6/28
 */
public class ContainerTest {


    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();

    }


    @Nested
    class ContextTest {

        @Nested
        public class TypeBinding {

            //TODO: instance
            @Test
            void should_bind_type_to_a_specific_instance() {
                Component instance = new Component() {
                };
                config.bind(Component.class, instance);
                final Context context = config.getContext();
                assertSame(instance, context.get(Component.class).get());


            }




            //TODO throw exception if field is final
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            void should_throw_exception_if_field_is_final() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectionProvider<>(FinalInjectField.class));
            }


            @Test
            void should_return_empty_if_component_undefined() {
                Optional<Component> component = config.getContext().get(Component.class);
                assertTrue(component.isEmpty());
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

            @Test // A->B->C->A
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



class DependencyWithInjectConstructor implements Dependency {

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