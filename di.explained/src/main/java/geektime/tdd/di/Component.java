package geektime.tdd.di;

import java.lang.annotation.Annotation;

/**
 * Created by manyan.ouyang ON 2023/8/3
 */
public record Component(Class<?> type, Annotation qualifier) {

}
