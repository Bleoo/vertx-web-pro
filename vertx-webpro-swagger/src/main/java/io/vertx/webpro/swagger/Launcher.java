package io.vertx.webpro.swagger;

import cn.hutool.core.convert.BasicType;
import cn.hutool.core.util.EnumUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.webpro.core.annotation.PathVariable;
import io.vertx.webpro.core.annotation.RequestBody;
import io.vertx.webpro.core.annotation.RequestHeader;
import io.vertx.webpro.core.annotation.RequestParam;
import io.vertx.webpro.core.process.MethodDescriptor;
import io.vertx.webpro.core.process.ProcessResult;
import io.vertx.webpro.core.process.RouterDescriptor;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class Launcher {

    @Getter
    OpenAPI openAPI = new OpenAPI();

    Map<String, PathItem> pathRegisterCache = new HashMap<>();

    public Launcher() {
        Info info = new Info();
        info.setTitle("Vert.x Web Pro APIs");
        info.setDescription("This specification was generated from Vert.x Web Pro.");
        openAPI.setInfo(info);
    }

    public void start(ProcessResult result, Router router) {
        Paths paths = new Paths();
        openAPI.setPaths(paths);
        Components components = new Components();
        openAPI.setComponents(components);
        for (RouterDescriptor routerDescriptor : result.getRouterDescriptors()) {
            Tag tag = getTag(routerDescriptor);
            if (tag == null) {
                continue;
            }
            openAPI.addTagsItem(tag);
            for (MethodDescriptor methodDescriptor : routerDescriptor.getMethodDescriptors()) {
                for (HttpMethod httpMethod : methodDescriptor.getHttpMethods()) {
                    for (String path : methodDescriptor.getPaths()) {
                        handle(methodDescriptor, httpMethod, path, tag, paths);
                    }
                }
            }
        }
        router.route("/*").handler(StaticHandler.create());
        router.get("/swagger/openapi.yaml").handler(ctx -> {
            try {
                ctx.end(Yaml.pretty().writeValueAsString(openAPI));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        pathRegisterCache.clear();
    }

    private Tag getTag(RouterDescriptor routerDescriptor) {
        String name;
        String description = null;
        Class<?> routerClass = routerDescriptor.getClazz();
        io.swagger.v3.oas.annotations.Hidden hiddenAnnotation =
                routerClass.getAnnotation(io.swagger.v3.oas.annotations.Hidden.class);
        if (hiddenAnnotation != null) {
            return null;
        }
        io.swagger.v3.oas.annotations.tags.Tag tagAnnotation =
                routerClass.getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);
        if (tagAnnotation != null) {
            name = tagAnnotation.name();
            description = tagAnnotation.description();
        } else {
            name = routerClass.getSimpleName();
        }
        return new Tag().name(name).description(description);
    }

    private void handle(MethodDescriptor methodDescriptor, HttpMethod httpMethod, String path, Tag tag, Paths paths) {
        Operation operation = getOperation(methodDescriptor);
        if(operation == null){
            return;
        }
        PathItem pathItem = pathRegisterCache.computeIfAbsent(path, k -> new PathItem());
        operation.addTagsItem(tag.getName());
        switch (httpMethod) {
            case OPTIONS:
                pathItem.setOptions(operation);
                break;
            case GET:
                pathItem.setGet(operation);
                break;
            case POST:
                pathItem.setPost(operation);
                break;
            case PUT:
                pathItem.setPut(operation);
                break;
            case PATCH:
                pathItem.setPatch(operation);
                break;
            case DELETE:
                pathItem.setDelete(operation);
                break;
            case HEAD:
                pathItem.setHead(operation);
                break;
            case TRACE:
                pathItem.setTrace(operation);
                break;
            case CONNECT:
                break;
            case OTHER:
                break;
        }
        Parameter[] parameters = methodDescriptor.getParameters();
        for (Parameter parameter : parameters) {
            io.swagger.v3.oas.annotations.Parameter parameterAnnotation
                    = parameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
            if (parameterAnnotation != null && parameterAnnotation.hidden()) {
                continue;
            }
            Schema schema = getSchema(parameter.getType());
            RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
            if (requestHeader != null) {
                pathItem.addParametersItem(new HeaderParameter()
                        .name(requestHeader.value())
                        .schema(schema)
                        .required(requestHeader.required()));
            }
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                pathItem.addParametersItem(new QueryParameter()
                        .name(requestParam.value())
                        .schema(schema)
                        .required(requestParam.required()));
            }
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                pathItem.addParametersItem(new PathParameter()
                        .name(pathVariable.value())
                        .schema(schema)
                        .required(pathVariable.required()));
            }
            RequestBody requestBodyAnnotation = parameter.getAnnotation(RequestBody.class);
            if (requestBodyAnnotation != null) {
                io.swagger.v3.oas.models.parameters.RequestBody requestBody
                        = new io.swagger.v3.oas.models.parameters.RequestBody();
                requestBody.content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
                operation.requestBody(requestBody);
            }
        }
        // TODO 返回类型
//        Class<?> actualClass = methodDescriptor.getActualType().getClass();
//        Schema schema = getSchema(actualClass);
        ApiResponses apiResponses = new ApiResponses();
        operation.setResponses(apiResponses);
        apiResponses.addApiResponse("200", new ApiResponse().description("fake resp"));
        path = conventPah(path);
        paths.addPathItem(path, pathItem);
    }

    private Operation getOperation(MethodDescriptor methodDescriptor) {
        io.swagger.v3.oas.annotations.Operation operationAnnotation
                = methodDescriptor.getMethod().getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
        if (operationAnnotation == null || operationAnnotation.hidden()) {
            return null;
        }
        Operation operation = new Operation();
        return operation.summary(operationAnnotation.summary());
    }

    private Schema getSchema(Class<?> type) {
        Class<?> wrapType = BasicType.wrap(type);
        Schema schema;
        if (Number.class.isAssignableFrom(wrapType)) {
            schema = new NumberSchema();
        } else if (CharSequence.class.isAssignableFrom(wrapType)) {
            schema = new StringSchema();
        } else if (wrapType.isEnum()) {
            List<String> names = EnumUtil.getNames((Class<? extends Enum<?>>) wrapType);
            schema = new StringSchema();
            schema.setEnum(names);
        } else if (wrapType.isArray()) {
            Class<?> componentType = wrapType.getComponentType();
            schema = new ArraySchema().items(getSchema(componentType));
        } else if (wrapType == List.class) {
            Type[] actualTypeArguments = ((ParameterizedType) wrapType.getGenericSuperclass()).getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                Type actualType = actualTypeArguments[0];
                schema = new ArraySchema().items(getSchema(actualType.getClass()));
            } else {
                schema = new ArraySchema();
            }
        } else {
            schema = new ObjectSchema();
            Field[] fields = wrapType.getDeclaredFields();
            for (Field field : fields) {
                schema.addProperties(field.getName(), getSchema(field.getType()));
            }
            openAPI.getComponents().addSchemas(wrapType.getSimpleName(), schema);
        }
        return schema;
    }

    /**
     * /:id => /{id}
     *
     * @param path
     * @return
     */
    private String conventPah(String path) {
        String[] split = path.split("/");
        return Arrays.stream(split).map(s -> {
            if (s.startsWith(":")) {
                return "{" + s.substring(1) + "}";
            } else {
                return s;
            }
        }).collect(Collectors.joining("/"));
    }

}
