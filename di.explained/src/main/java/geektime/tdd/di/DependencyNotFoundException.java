package geektime.tdd.di;

/**
 * Created by manyan.ouyang ON 2023/6/30
 */
public class DependencyNotFoundException extends RuntimeException {

    private final Component dependency;
    private final Component component;



    public Component getDependency() {
        return dependency;
    }

    public Component getComponent() {
        return component;
    }

    public DependencyNotFoundException(Component component, Component dependency) {
        this.dependency = dependency;
        this.component = component;
    }
}
