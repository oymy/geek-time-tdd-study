package geektime.rest;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Assertions;
import tdd.di.ComponentRef;
import tdd.di.Config;
import tdd.di.Context;
import tdd.di.ContextConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by manyan.ouyang ON 2023/8/29
 */
class ASpike {

    Server server;

    @BeforeEach
    public void start() throws Exception {
        server = new Server(8080);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        TestApplication application = new TestApplication();
        handler.addServlet(new ServletHolder(new ResourceServlet(application, new TestProviders(application))), "/");
        server.setHandler(handler);
        server.start();

    }

    @AfterEach
    public void stop() throws Exception {
        server.stop();

    }

    @Test
    void should() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        Assertions.assertEquals("prefixprefixtest", response.body());


    }

    @Path("/test")
    static class TestResource {
        @Inject
        @Named("prefix")
        String prefix;

        @jakarta.ws.rs.core.Context
        Providers providers;

        @GET
        public String get() {
            return prefix + "test";
        }

        public TestResource() {
        }
    }

    static class ResourceServlet extends HttpServlet {
        private final TestApplication application;
        private final Providers providers;
        private final Context context;


        public ResourceServlet(TestApplication application, Providers providers) {
            this.application = application;
            this.providers = providers;

            context = application.getContext();

        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Stream<Class<?>> rootResources = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));

            Object result = dispatch(req, rootResources);
            MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(result.getClass(), null, null, null);
            writer.writeTo(result, null, null, null, null, null, resp.getOutputStream());
        }

        Object dispatch(HttpServletRequest req, Stream<Class<?>> rootResources) {
            try {

                Class<?> rootClass = rootResources.findFirst().get();
                Object rootResource = context.get(ComponentRef.of(rootClass)).get();
                Method method = Arrays.stream(rootClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
                return method.invoke(rootResource);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class TestApplication extends Application {

        private final Context context;

        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class, StringMessageBodyWriter.class);
        }

        public Config getConfig() {
            return new Config() {
                @Named("prefix")
                public final String name = "prefix";
            };

        }

        public Context getContext() {
            return context;
        }

        public TestApplication() {
            ContextConfig config = new ContextConfig();
            config.from(getConfig());


            List<Class<?>> writerClasses = this.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            for (Class writerClass : writerClasses) {
                config.component(writerClass, writerClass);
            }
            List<Class<?>> rootResources = this.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class)).toList();
            for (Class rootResource : rootResources) {
                config.component(rootResource, rootResource);
            }
            context = config.getContext();
        }


    }

    @Provider
    static class StringMessageBodyWriter implements MessageBodyWriter<String> {
        @Inject
        @Named("prefix")
        String prefix;

        public StringMessageBodyWriter() {
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
            PrintWriter writer = new PrintWriter(entityStream);
            writer.write(prefix);
            writer.write(s);
            writer.flush();

        }
    }

    static class TestProviders implements Providers {
        private final Application application;
        private final List<MessageBodyWriter> writers;

        public TestProviders(TestApplication application) {
            this.application = application;


            Context context = application.getContext();
            List<Class<?>> writerClasses = this.application.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            writers = (List<MessageBodyWriter>) writerClasses.stream().map(c -> context.get(ComponentRef.of(c)).get()).toList();

        }

        @Override
        public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return null;
        }

        @Override
        public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return writers.stream().filter(w -> w.isWriteable(type, genericType, annotations, mediaType)).findFirst().get();
        }

        @Override
        public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
            return null;
        }

        @Override
        public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
            return null;
        }
    }

}
