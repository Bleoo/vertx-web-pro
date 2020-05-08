package io.bleoo.process;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassUtil;
import io.bleoo.annotation.RequestMapping;
import io.bleoo.annotation.Router;
import io.bleoo.exception.IllegalPathException;
import io.bleoo.exception.NewInstanceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
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
            String mainPath = handlePath(routerAnnotation.value());

            Method[] methods = ClassUtil.getPublicMethods(clz);
            Object routeInstance = null;
            try {
                routeInstance = clz.newInstance();
            } catch (Exception e) {
                throw new NewInstanceException(e);
            }
            for (Method method : methods) {
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                if (requestMapping == null) continue;
                RouteMethod routeMethod = new RouteMethod();
                String subPath = handlePath(requestMapping.value());
                routeMethod.setPath(handlePath(mainPath + subPath));
                routeMethod.setHttpMethod(requestMapping.method());
                routeMethod.setInstance(routeInstance);
                routeMethod.setMethod(method);
                result.getRouteMethods().add(routeMethod);
                log.debug("Find http path: {} {}", routeMethod.getHttpMethod().name(), routeMethod.getPath());
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
