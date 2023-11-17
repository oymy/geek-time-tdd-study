package geektime.tdd.di;

import java.util.List;

/**
 * Created by manyan.ouyang ON 2023/8/25
 */
interface ComponentProvider<T> {
    T get(Context context);


    default List<ComponentRef<?>> getDependencies() {
        return List.of();
    }


}
