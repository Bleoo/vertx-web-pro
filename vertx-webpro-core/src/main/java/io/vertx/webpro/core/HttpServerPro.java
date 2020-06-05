package io.vertx.webpro.core;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.TypeUtil;
import cn.hutool.http.ContentType;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.webpro.core.annotation.PathVariable;
import io.vertx.webpro.core.annotation.RequestBody;
import io.vertx.webpro.core.annotation.RequestHeader;
import io.vertx.webpro.core.annotation.RequestParam;
import io.vertx.webpro.core.exception.AnnotationEmptyValueException;
import io.vertx.webpro.core.exception.MappingDuplicateException;
import io.vertx.webpro.core.exception.ReturnTypeWrongException;
import io.vertx.webpro.core.process.AnnotationProcessor;
import io.vertx.webpro.core.process.ProcessResult;
import io.vertx.webpro.core.process.MethodDescriptor;
import io.vertx.webpro.core.process.RouterDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
public class HttpServerPro {

    private final Vertx vertx;
    private HttpServer httpServer;
    private Router router;

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
        httpServer = vertx.createHttpServer();
        router = Router.router(vertx);
        for (RouterDescriptor routerDescriptor : processResult.getRouterDescriptors()) {
            for (MethodDescriptor methodDescriptor : routerDescriptor.getMethodDescriptors()) {
                for (HttpMethod httpMethod : methodDescriptor.getHttpMethods()) {
                    for (String path : methodDescriptor.getPaths()) {
                        router.route(httpMethod, path).handler(BodyHandler.create()).handler(ctx -> {
                            try {
                                handleRequest(methodDescriptor, ctx);
                            } catch (Exception e) {
                                handleError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
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
        }
        // swagger ext
        try {
            Class<?> launcherClass = Class.forName("io.vertx.webpro.swagger.Launcher");
            Method start = launcherClass.getDeclaredMethod("start", ProcessResult.class, Router.class);
            Object launcher = launcherClass.newInstance();
            start.invoke(launcher, processResult, router);
        } catch (Exception e) {
            e.printStackTrace();
        }

        httpServer.requestHandler(router).listen(8081, event -> {
            if (event.succeeded()) {
                log.info("Http listen on {}", 8081);
            } else {
                event.cause().printStackTrace();
                vertx.close();
            }
        });
    }

    private void handleError(RoutingContext ctx, HttpResponseStatus status) {
        ctx.response().setStatusCode(status.code()).end(status.reasonPhrase());
    }

    private void handleRequest(MethodDescriptor methodDescriptor, RoutingContext ctx) throws Exception {
        HttpServerResponse response = ctx.response();
        Method method = methodDescriptor.getMethod();
        Parameter[] parameters = methodDescriptor.getParameters();
        Object[] objects = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();
            dealRequestHeader(ctx, objects, i, parameter, type);
            dealRequestParam(ctx, objects, i, parameter, type);
            dealPathVariable(ctx, objects, i, parameter, type);
            dealRequestBody(ctx, objects, i, parameter, type);
        }

        if (ClassUtil.isAssignable(Future.class, methodDescriptor.getReturnType().getClass())) {
            throw new ReturnTypeWrongException();
        }
        // Write to the response and end it
        Future<?> result = (Future<?>) method.invoke(methodDescriptor.getRouterDescriptor().getInstance(), objects);
        dealResponse(response, methodDescriptor.getActualType(), result);
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
                if (!params.isEmpty()) {
                    String param = params.get(0);
                    objects[i] = baseTypeConvert(type, param);
                }
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

    private void dealResponse(HttpServerResponse response, Type type, Future<?> result) {
        ContentType contentType;
        Class<?> clz = TypeUtil.getClass(type);
        if (clz == null || clz.isPrimitive() || ClassUtil.isPrimitiveWrapper(clz) || CharSequence.class.isAssignableFrom(clz)) {
            contentType = ContentType.TEXT_PLAIN;
        } else {
            contentType = ContentType.JSON;
        }
        response.putHeader("content-type", contentType.getValue());
        result.onComplete(ar -> {
            if (ar.succeeded()) {
                switch (contentType) {
                    case JSON:
                        response.end(Json.encode(ar.result()));
                        break;
                    case TEXT_PLAIN:
                        response.end(ar.result().toString());
                        break;
                    default:
                        response.end();
                }
            } else {
                response.setStatusCode(500);
                response.end();
            }
        });
    }

}
