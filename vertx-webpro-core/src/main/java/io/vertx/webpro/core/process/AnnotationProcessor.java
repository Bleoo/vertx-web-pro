package io.vertx.webpro.core.process;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassUtil;
import io.vertx.core.http.HttpMethod;
import io.vertx.webpro.core.annotation.*;
import io.vertx.webpro.core.exception.EmptyMethodsException;
import io.vertx.webpro.core.exception.EmptyPathsException;
import io.vertx.webpro.core.exception.IllegalPathException;
import io.vertx.webpro.core.exception.NewInstanceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * @author leo
 * @date 2020/5/6 17:35
 */
@Slf4j
public class AnnotationProcessor {

    public ProcessResult process(Class<?> clazz) {
        ProcessResult result = new ProcessResult();
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation(ClassUtil.getPackage(clazz), Router.class);

        for (Class<?> clz : classes) {
            Router routerAnnotation = AnnotationUtil.getAnnotation(clz, Router.class);
            String parentPath = handlePath(routerAnnotation.value());

            Method[] methods = ClassUtil.getPublicMethods(clz);
            Object routeInstance;
            try {
                routeInstance = clz.newInstance();
            } catch (Exception e) {
                throw new NewInstanceException(e);
            }
            RouterDescriptor routerDescriptor = new RouterDescriptor(clz, routeInstance);
            result.addRouterDescriptor(routerDescriptor);

            for (Method method : methods) {
                String[] value = getValues(method);
                if (value == null) continue;
                HttpMethod[] httpMethod = getHttpMethods(method);
                String[] paths = Arrays.stream(value)
                        .map(s -> this.handlePath(parentPath + s))
                        .distinct()
                        .toArray(String[]::new);
                if (paths.length == 0) {
                    throw new EmptyPathsException();
                }
                HttpMethod[] httpMethods = Arrays.stream(httpMethod).distinct().toArray(HttpMethod[]::new);
                if (httpMethods.length == 0) {
                    throw new EmptyMethodsException();
                }

                MethodDescriptor methodDescriptor = new MethodDescriptor(routerDescriptor);
                methodDescriptor.setPaths(paths);
                methodDescriptor.setHttpMethods(httpMethods);
                methodDescriptor.setMethod(method);
                routerDescriptor.addMethodDescriptor(methodDescriptor);
            }
        }

        return result;
    }

    private String[] getValues(Method method) {
        String name = "value";
        String[] value = AnnotationUtil.getAnnotationValue(method, GetMapping.class, name);
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, PostMapping.class, name);
        }
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, PutMapping.class, name);
        }
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, PatchMapping.class, name);
        }
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, DeleteMapping.class, name);
        }
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, RequestMapping.class, name);
        }
        return value;
    }

    private HttpMethod[] getHttpMethods(Method method) {
        String name = "method";
        HttpMethod[] value = AnnotationUtil.getAnnotationValue(method, GetMapping.class, name);
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, PostMapping.class, name);
        }
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, PutMapping.class, name);
        }
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, PatchMapping.class, name);
        }
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, DeleteMapping.class, name);
        }
        if (value == null) {
            value = AnnotationUtil.getAnnotationValue(method, RequestMapping.class, name);
        }
        return value;
    }

    private String handlePath(String path) {
        path = path.trim();
        if (StringUtils.isBlank(path) || !path.startsWith("/")) {
            throw new IllegalPathException();
        }
        path = StringUtils.replaceAll(path, "[/]+", "/");
        if (path.length() > 1) {
            path = StringUtils.stripEnd(path, "/");
        }
        return path;
    }


}
