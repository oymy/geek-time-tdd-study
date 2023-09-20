package geektime.tdd.rest;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.RuntimeDelegate;

/**
 * Created by manyan.ouyang ON 2023/9/13
 */
public class ResourceServlet extends HttpServlet {
    private final Runtime runtime;

    public ResourceServlet(Runtime runtime) {

        this.runtime = runtime;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        ResourceRouter resourceRouter = runtime.getResourceRouter();
        OutboundResponse response = resourceRouter.dispatch(req, runtime.createResourceContext(req, resp));
        resp.setStatus(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        for (String name : headers.keySet())
            for (Object value : headers.get(name)) {
                RuntimeDelegate.HeaderDelegate delegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
                resp.addHeader(name, delegate.toString(value));
            }
    }
}
