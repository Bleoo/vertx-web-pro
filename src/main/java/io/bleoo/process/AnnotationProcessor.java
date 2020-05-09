package io.bleoo.process;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassUtil;
import io.bleoo.annotation.RequestMapping;
import io.bleoo.annotation.Router;
import io.bleoo.exception.EmptyMethodsException;
import io.bleoo.exception.EmptyPathsException;
import io.bleoo.exception.IllegalPathException;
import io.bleoo.exception.NewInstanceException;
import io.vertx.core.http.HttpMethod;
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
            for (Method method : methods) {
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                if (requestMapping == null) continue;
                RouteMethod routeMethod = new RouteMethod();

                String[] paths = Arrays.stream(requestMapping.value())
                        .map(s -> this.handlePath(parentPath + s))
                        .distinct()
                        .toArray(String[]::new);
                if (paths.length == 0) {
                    throw new EmptyPathsException();
                }
                HttpMethod[] httpMethods = Arrays.stream(requestMapping.method()).distinct().toArray(HttpMethod[]::new);
                if (httpMethods.length == 0) {
                    throw new EmptyMethodsException();
                }
                routeMethod.setPaths(paths);
                routeMethod.setHttpMethods(httpMethods);
                routeMethod.setInstance(routeInstance);
                routeMethod.setMethod(method);
                result.getRouteMethods().add(routeMethod);
            }
        }

        return result;
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
