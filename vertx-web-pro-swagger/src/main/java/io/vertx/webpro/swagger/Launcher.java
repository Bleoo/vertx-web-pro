package io.vertx.webpro.swagger;

import cn.hutool.core.util.ClassUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
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
import io.vertx.webpro.core.annotation.RequestHeader;
import io.vertx.webpro.core.annotation.RequestParam;
import io.vertx.webpro.core.process.ProcessResult;
import io.vertx.webpro.core.process.MethodDescriptor;
import io.vertx.webpro.core.process.RouterDescriptor;
import lombok.Getter;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Launcher {

    @Getter
    OpenAPI openAPI = new OpenAPI();

    public Launcher() {
        Info info = new Info();
        info.setTitle("Vert.x Web Pro APIs");
        info.setDescription("This specification was generated from Vert.x Web Pro.");
        openAPI.setInfo(info);
    }

    public void start(ProcessResult result, Router router) {
        Paths paths = new Paths();
        openAPI.setPaths(paths);
        for (RouterDescriptor routerDescriptor : result.getRouterDescriptors()) {
            Tag tag = new Tag().name(routerDescriptor.getClazz().getSimpleName());
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
    }

    private void handle(MethodDescriptor methodDescriptor, HttpMethod httpMethod, String path, Tag tag, Paths paths) {
        PathItem pathItem = new PathItem();
        Operation operation = new Operation();
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
            Class<?> type = parameter.getType();
            Schema schema;
            if (type.isPrimitive() || ClassUtil.isPrimitiveWrapper(type) || CharSequence.class.isAssignableFrom(type)) {
                schema = new StringSchema();
            } else {
                schema = new ObjectSchema();
            }
            RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
            if (requestHeader != null) {
                pathItem.addParametersItem(new HeaderParameter().name(requestHeader.value()).schema(schema));
            }
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                pathItem.addParametersItem(new QueryParameter().name(requestParam.value()).schema(schema));
            }
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                pathItem.addParametersItem(new PathParameter().name(pathVariable.value()).schema(schema));
            }
        }
        ApiResponses apiResponses = new ApiResponses();
        operation.setResponses(apiResponses);
        apiResponses.addApiResponse("200", new ApiResponse().description("fake resp"));
        path = conventPah(path);
        paths.put(path, pathItem);
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
