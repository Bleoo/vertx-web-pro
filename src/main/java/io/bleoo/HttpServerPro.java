package io.bleoo;

import io.bleoo.process.AnnotationProcessor;
import io.bleoo.process.ProcessResult;
import io.bleoo.process.RouteMethod;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpServerPro {

    private Vertx vertx;
    private ProcessResult processResult;

    public HttpServerPro(Vertx vertx) {
        this.vertx = vertx;
    }

    public static HttpServerPro run(Vertx vertx, Class<?> mainClass) {
        HttpServerPro server = new HttpServerPro(vertx);
        server.start(mainClass);
        return server;
    }


    public void start(Class<?> mainClass) {
        processResult = new AnnotationProcessor().process(mainClass);
        startHttpServer();
    }

    private void startHttpServer() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        for (RouteMethod routeMethod : processResult.getRouteMethods()) {
            for (HttpMethod httpMethod : routeMethod.getHttpMethods()) {
                for (String path : routeMethod.getPaths()) {
                    router.route(httpMethod, path).handler(routingContext -> {

                        HttpServerRequest request = routingContext.request();

                        // This handler will be called for every request
                        HttpServerResponse response = routingContext.response();
                        response.putHeader("content-type", "text/plain");

                        // Write to the response and end it
                        try {
                            String text = (String) routeMethod.getMethod().invoke(routeMethod.getInstance());
                            response.end(text);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    log.debug("HTTP Mapping {} {}", httpMethod.name(), path);
                }
            }
        }

        server.requestHandler(router).listen(8080);
        log.info("Http listen on {}", 8080);
    }

}
