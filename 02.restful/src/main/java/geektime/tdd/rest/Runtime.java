package geektime.tdd.rest;

import geektime.tdd.di.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.ext.Providers;

/**
 * Created by manyan.ouyang ON 2023/9/13
 */
public interface Runtime {
    Providers getProviders();

    ResourceContext createResourceContext(HttpServletRequest request, HttpServletResponse response);

    Context getApplicationContext();

    ResourceRouter getResourceRouter();
}
