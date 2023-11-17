package geektime.tdd.di;

/**
 * Created by manyan.ouyang ON 2023/8/25
 */
interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
