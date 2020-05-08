package io.bleoo.process;

import io.vertx.core.http.HttpMethod;
import lombok.Data;

import java.util.*;

@Data
public class ProcessResult {

    private Map<HttpMethod, Set<String>> routeMap = new HashMap<>();

    private List<RouteMethod> routeMethods = new ArrayList<>();

}
