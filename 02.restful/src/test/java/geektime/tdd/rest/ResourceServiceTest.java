package geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by manyan.ouyang ON 2023/9/20
 */
class ResourceServiceTest extends ServletTest {
    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;
    private Providers providers;
    private OutboundResponseBuilder response;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);

        when(runtime.getProviders()).thenReturn(providers);
        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(resourceContext);
        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(resourceContext);
        return new ResourceServlet(runtime);
    }


    @Test
    void should_use_status_from_response() throws Exception {

        response.status(Response.Status.NOT_MODIFIED).returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @BeforeEach
    void beforeEach() {

        response = new OutboundResponseBuilder();
        RuntimeDelegate delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);

        when(delegate.createHeaderDelegate(NewCookie.class)).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            @Override
            public String toString(NewCookie value) {
                return value.getName() + "=" + value.getValue();
            }
        });


    }


    @Test
    void should_use_http_headers_from_response() throws Exception {

        response.status(Response.Status.NOT_MODIFIED)
                .headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(),
                        new NewCookie.Builder("USER_ID").value("user").build())
                .returnFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray());
    }

    @Test
    void should_write_entity_to_http_response_using_message_body_writer() throws Exception {
        response.entity(new GenericEntity<>("entity", String.class), new Annotation[0]).returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());


    }

    @Test
    void should_use_response_from_web_application_exception() throws Exception {
        response.status(Response.Status.FORBIDDEN)
                .headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(),
                        new NewCookie.Builder("USER_ID").value("user").build())
                .entity(new GenericEntity<>("error", String.class), new Annotation[0])
                .throwFrom(router);
        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray());
        assertEquals("error", httpResponse.body());


    }

    @Test
    void should_build_response_by_exception_mapper_if_null_response_from_web_application_exception() throws Exception {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(
                exception -> response.status(Response.Status.FORBIDDEN)
                        .build());


        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());

    }


    class OutboundResponseBuilder {
        Response.Status status = Response.Status.OK;
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public OutboundResponseBuilder headers(String name, Object... values) {
            headers.addAll(name, values);
            return this;
        }

        public OutboundResponseBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        public OutboundResponseBuilder entity(GenericEntity<Object> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        void returnFrom(ResourceRouter router) {
            build(response -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));
        }

        void throwFrom(ResourceRouter router) {
            build(response -> {
                WebApplicationException ex = new WebApplicationException(response);
                when(router.dispatch(any(), eq(resourceContext))).thenThrow(ex);
            });
        }

        void build(Consumer<OutboundResponse> consumer) {
            OutboundResponse response = build();
            consumer.accept(response);


        }

        OutboundResponse build() {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getHeaders()).thenReturn(headers);
            when(response.getStatusInfo()).thenReturn(status);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);


            stubMessageBodyWriter();
            return response;
        }

        private void stubMessageBodyWriter() {

            when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), same(annotations),
                    eq(mediaType))).thenReturn(new MessageBodyWriter<>() {

                @Override
                public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                    return false;
                }

                @Override
                public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                    PrintWriter writer = new PrintWriter(entityStream);
                    writer.write(s);
                    writer.flush();

                }
            });
        }


    }

}
