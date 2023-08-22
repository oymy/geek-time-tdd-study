package geektime.tdd.di;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manyan.ouyang ON 2023/8/22
 */
class PooledProvider<T> implements ContextConfig.ComponentProvider<T> {

    static int MAX = 2;
    private List<T> pool= new ArrayList<>();
    int current;
    private final ContextConfig.ComponentProvider<T> provider;

    public PooledProvider(ContextConfig.ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (pool.size() < MAX) pool.add(provider.get(context));
        return pool.get(current++ % MAX);
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }

}
