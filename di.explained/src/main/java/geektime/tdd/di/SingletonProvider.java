package geektime.tdd.di;

import java.util.List;

/**
 * Created by manyan.ouyang ON 2023/8/25
 */
class SingletonProvider<T> implements ComponentProvider<T> {
    private T singleton;
    private final ComponentProvider<T> provider;

    public SingletonProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (singleton == null) singleton = provider.get(context);
        return singleton;
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }

}
