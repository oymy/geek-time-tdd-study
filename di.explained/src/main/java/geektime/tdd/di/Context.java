package geektime.tdd.di;

import java.util.Optional;

/**
 * Created by manyan.ouyang ON 2023/7/11
 */
public interface Context {
    <Type> Optional<Type> get(Class<Type> type);
}
