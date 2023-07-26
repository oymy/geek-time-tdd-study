package geektime.tdd.di;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Created by manyan.ouyang ON 2023/7/11
 */
public interface Context {


    Optional get(Type type);
}
