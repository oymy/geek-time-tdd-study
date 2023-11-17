package geektime.tdd.rest;

import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;

import java.lang.annotation.Annotation;

/**
 * Created by manyan.ouyang ON 2023/9/13
 */
public abstract class OutboundResponse extends Response {
    abstract GenericEntity getGenericEntity();

    abstract Annotation[] getAnnotations();
    //abstract void write(HttpServletRequest response, Providers providers);

}
