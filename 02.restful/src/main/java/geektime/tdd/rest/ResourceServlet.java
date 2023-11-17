package geektime.tdd.rest;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;

/**
 * Created by manyan.ouyang ON 2023/9/13
 */
public class ResourceServlet extends HttpServlet {
    private final Runtime runtime;

    public ResourceServlet(Runtime runtime) {

        this.runtime = runtime;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ResourceRouter resourceRouter = runtime.getResourceRouter();
        Providers providers = runtime.getProviders();
        OutboundResponse response;

        try {
            response = resourceRouter.dispatch(req, runtime.createResourceContext(req, resp));
        } catch (WebApplicationException e) {
            response = (OutboundResponse) (e.getResponse());
        } catch (Throwable throwable) {
            ExceptionMapper exceptionMapper = providers.getExceptionMapper(throwable.getClass());
            response = (OutboundResponse) exceptionMapper.toResponse(throwable);
        }
        resp.setStatus(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        for (String name : headers.keySet())
            for (Object value : headers.get(name)) {
                RuntimeDelegate.HeaderDelegate delegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
                resp.addHeader(name, delegate.toString(value));
            }

        GenericEntity entity = response.getGenericEntity();
        MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
        writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(), headers, resp.getOutputStream());
    }
}
