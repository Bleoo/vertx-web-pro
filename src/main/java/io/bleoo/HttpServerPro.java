package io.bleoo;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.TypeUtil;
import io.bleoo.annotation.PathVariable;
import io.bleoo.annotation.RequestBody;
import io.bleoo.annotation.RequestParam;
import io.bleoo.exception.AnnotationEmptyValueException;
import io.bleoo.process.AnnotationProcessor;
import io.bleoo.process.ProcessResult;
import io.bleoo.process.RouteMethod;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;

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
                    router.route(httpMethod, path).handler(ctx -> {

                        HttpServerRequest request = ctx.request();

                        // This handler will be called for every request
                        HttpServerResponse response = ctx.response();
                        response.putHeader("content-type", "text/plain");

                        try {
                            Method method = routeMethod.getMethod();
                            Parameter[] parameters = method.getParameters();
                            Object[] objects = new Object[parameters.length];
                            for (int i = 0; i < parameters.length; i++) {
                                Parameter parameter = parameters[i];
                                Class<?> type = parameter.getType();
                                dealRequestParam(ctx, objects, i, parameter, type);
                                dealPathVariable(ctx, objects, i, parameter, type);
                                dealRequestBody(ctx, objects, i, parameter, type);
                            }

                            // Write to the response and end it
                            String text = (String) method.invoke(routeMethod.getInstance(), objects);
                            response.end(text);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    log.debug("HTTP Mapping {} {}", httpMethod.name(), path);
                }
            }
        }

        server.requestHandler(router).listen(8080, event -> {
            if (event.succeeded()) {
                log.info("Http listen on {}", 8080);
            } else {
                event.cause().printStackTrace();
                vertx.close();
            }
        });

    }

    private void dealRequestBody(RoutingContext ctx, Object[] objects, int i, Parameter parameter, Class<?> type) throws Exception {
        RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            // 类型转换
            if (Collection.class.isAssignableFrom(type)) {
                JsonArray bodyAsJsonArray = ctx.getBodyAsJsonArray();
                objects[i] = bodyAsJsonArray.getList();
            } else if (type.isArray()) {
                JsonArray bodyAsJsonArray = ctx.getBodyAsJsonArray();
                objects[i] = bodyAsJsonArray.getList().toArray();
            } else {
                // TODO
            }
        }
    }

    private void dealRequestParam(RoutingContext ctx, Object[] objects, int i, Parameter parameter, Class<?> type) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            String value = requestParam.value().trim();
            if (StringUtils.isBlank(value)) {
                throw new AnnotationEmptyValueException();
            }
            List<String> params = ctx.queryParam(value);
            // 类型转换
            if (type == Integer.class) {
                objects[i] = Integer.valueOf(params.get(0));
            } else if (type == String.class) {
                objects[i] = params.get(0);
            }
        }
    }

    private void dealPathVariable(RoutingContext ctx, Object[] objects, int i, Parameter parameter, Class<?> type) {
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            String value = pathVariable.value().trim();
            if (StringUtils.isBlank(value)) {
                throw new AnnotationEmptyValueException();
            }
            String param = ctx.pathParam(value);
            // 类型转换
            if (type == Integer.class) {
                objects[i] = Integer.valueOf(param);
            } else if (type == String.class) {
                objects[i] = param;
            }
        }
    }

}
