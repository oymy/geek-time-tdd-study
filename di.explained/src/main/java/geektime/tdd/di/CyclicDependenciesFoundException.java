package geektime.tdd.di;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by manyan.ouyang ON 2023/6/30
 */
public class CyclicDependenciesFoundException extends RuntimeException {
    private final Set<Component> components = new HashSet<>();

    public CyclicDependenciesFoundException(List<Component> visiting) {
        this.components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.stream().map(Component::type).toArray(Class[]::new);
    }
}
