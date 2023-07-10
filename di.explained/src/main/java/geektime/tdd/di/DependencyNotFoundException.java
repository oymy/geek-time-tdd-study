package geektime.tdd.di;

/**
 * Created by manyan.ouyang ON 2023/6/30
 */
public class DependencyNotFoundException extends RuntimeException {
    private final Class<?> dependency;
    private final Class<?> component;
    public Class<?> getDependency() {
        return dependency;
    }

    public DependencyNotFoundException(Class<?> dependency, Class<?> component) {
        this.dependency = dependency;
        this.component = component;
    }

    public Class<?> getComponent() {
        return component;
    }
}
