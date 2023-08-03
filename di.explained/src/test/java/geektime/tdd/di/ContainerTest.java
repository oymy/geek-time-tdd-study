package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

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
    public class DependenciesSelection {



    }


    @Nested
    public class LifecycleManagement {

    }




}

interface TestComponent {

}




interface Dependency {

}


interface AnotherDependency {

}




class DependencyDependOnComponent implements Dependency {
    private final TestComponent component;

    @Inject
    public DependencyDependOnComponent(TestComponent component) {
        this.component = component;
    }
}

class AnotherDependencyDependOnComponent implements AnotherDependency {
    private final TestComponent component;

    @Inject
    public AnotherDependencyDependOnComponent(TestComponent component) {
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