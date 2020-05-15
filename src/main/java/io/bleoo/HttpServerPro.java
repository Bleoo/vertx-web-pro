package io.bleoo;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.http.ContentType;
import io.bleoo.annotation.PathVariable;
import io.bleoo.annotation.RequestBody;
import io.bleoo.annotation.RequestHeader;
import io.bleoo.annotation.RequestParam;
import io.bleoo.exception.AnnotationEmptyValueException;
import io.bleoo.exception.MappingDuplicateException;
import io.bleoo.process.AnnotationProcessor;
import io.bleoo.process.ProcessResult;
import io.bleoo.process.RouteMethod;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Slf4j
public class HttpServerPro {

    private Vertx vertx;
    private ProcessResult processResult;
    private Map<HttpMethod, Set<String>> existsPathMap;

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
        existsPathMap = new HashMap<>();
        for (HttpMethod method : HttpMethod.values()) {
            existsPathMap.put(method, new HashSet<>());
        }
        startHttpServer();
    }

    private void startHttpServer() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        for (RouteMethod routeMethod : processResult.getRouteMethods()) {
            for (HttpMethod httpMethod : routeMethod.getHttpMethods()) {
                for (String path : routeMethod.getPaths()) {
                    router.route(httpMethod, path).handler(BodyHandler.create()).handler(ctx -> {

                        // This handler will be called for every request
                        HttpServerResponse response = ctx.response();

                        try {
                            Method method = routeMethod.getMethod();
                            Parameter[] parameters = method.getParameters();
                            Object[] objects = new Object[parameters.length];
                            for (int i = 0; i < parameters.length; i++) {
                                Parameter parameter = parameters[i];
                                Class<?> type = parameter.getType();
                                dealRequestHeader(ctx, objects, i, parameter, type);
                                dealRequestParam(ctx, objects, i, parameter, type);
                                dealPathVariable(ctx, objects, i, parameter, type);
                                dealRequestBody(ctx, objects, i, parameter, type);
                            }

                            Class<?> returnType = method.getReturnType();
                            // Write to the response and end it
                            Object result = method.invoke(routeMethod.getInstance(), objects);
                            dealResponse(response, returnType);
                            response.end(Json.encode(result));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    Set<String> pathSet = existsPathMap.get(httpMethod);
                    if (pathSet.contains(path)) {
                        throw new MappingDuplicateException(httpMethod, path);
                    }
                    pathSet.add(path);
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

    private void dealRequestHeader(RoutingContext ctx, Object[] objects, int i, Parameter parameter, Class<?> type) {
        RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            String key = requestHeader.value();
            String headerValue = ctx.request().getHeader(key);
            objects[i] = baseTypeConvert(type, headerValue);
        }
    }

    private void dealRequestBody(RoutingContext ctx, Object[] objects, int i, Parameter parameter, Class<?> type) {
        RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
        if (requestBody != null) {
            if (Collection.class.isAssignableFrom(type)) {
                JsonArray bodyAsJsonArray = ctx.getBodyAsJsonArray();
                objects[i] = bodyAsJsonArray.getList();
            } else if (type.isArray()) {
                JsonArray bodyAsJsonArray = ctx.getBodyAsJsonArray();
                objects[i] = bodyAsJsonArray.getList().toArray();
            } else {
                Object o = ctx.getBodyAsJson().mapTo(type);
                objects[i] = o;
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
            if (Collection.class.isAssignableFrom(type)) {
                // 暂时只支持了 string
                objects[i] = params;
            } else if (type.isArray()) {
                objects[i] = params.toArray();
            } else {
                String param = params.get(0);
                objects[i] = baseTypeConvert(type, param);
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
            objects[i] = baseTypeConvert(type, param);
        }
    }

    private Object baseTypeConvert(Class<?> type, String param) {
        if (type == byte.class || type == Byte.class) {
            return Byte.valueOf(param);
        } else if (type == short.class || type == Short.class) {
            return Integer.valueOf(param);
        } else if (type == int.class || type == Integer.class) {
            return Integer.valueOf(param);
        } else if (type == long.class || type == Long.class) {
            return Long.valueOf(param);
        } else if (type == float.class || type == Float.class) {
            return Float.valueOf(param);
        } else if (type == double.class || type == Double.class) {
            return Double.valueOf(param);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.valueOf(param);
        } else if (type == char.class || type == Character.class) {
            if (param.length() > 0) {
                return param.charAt(0);
            }
        } else if (type == String.class) {
            return param;
        }
        return null;
    }

    private void dealResponse(HttpServerResponse response, Class<?> type) {
        ContentType contentType;
        if (type.isPrimitive() || ClassUtil.isPrimitiveWrapper(type) || CharSequence.class.isAssignableFrom(type)) {
            contentType = ContentType.TEXT_PLAIN;
        } else {
            contentType = ContentType.JSON;
        }
        response.putHeader("content-type", contentType.getValue());
    }

}
