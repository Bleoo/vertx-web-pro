package io.bleoo.process;

import io.vertx.core.http.HttpMethod;
import lombok.Data;

import java.lang.reflect.Method;

@Data
public class RouteMethod {

    private String[] paths;
    private HttpMethod[] httpMethods;
    private Object instance;
    private Method method;

}
