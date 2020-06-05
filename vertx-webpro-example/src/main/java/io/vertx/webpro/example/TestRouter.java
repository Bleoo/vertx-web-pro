package io.vertx.webpro.example;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.webpro.core.annotation.*;
import io.vertx.webpro.example.dto.Query;
import io.vertx.webpro.example.dto.Result;
import io.vertx.webpro.example.dto.SomeType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leo
 * @date 2020/5/6 17:14
 */

@Tag(name = "Test", description = "测试相关接口")
@Router
public class TestRouter {

    @Operation(summary = "获取helloWorld")
    @GetMapping(value = "/get")
    public Future<String> get() {
        return Future.succeededFuture("helloWorld");
    }

    @Operation
    @RequestMapping(value = "/get1", method = HttpMethod.GET)
    public Future<String> get1(@RequestParam("id") Integer id) {
        return Future.succeededFuture(String.valueOf(id));
    }

    @Operation
    @RequestMapping(value = "/get2/:id", method = HttpMethod.GET)
    public Future<String> get2(@PathVariable("id") Integer id) {
        return Future.succeededFuture(String.valueOf(id));
    }

    @Operation
    @PostMapping(value = "//post1")
    public Future<String> post1() {
        return Future.succeededFuture("helloWorld");
    }

    @Operation
    @RequestMapping(value = "/post2", method = HttpMethod.POST)
    public Future<Result<String>> post2(@RequestParam("id") String id,
                                        @RequestBody Query query) {
        Result<String> result = new Result<>();
        result.setRequestId(query.getRequestId());
        result.setPage(query.getPage());
        result.setSize(query.getSize());
        List<String> data = new ArrayList<>();
        data.add("test1");
        data.add("test2");
        result.setData(data);
        return Future.succeededFuture(result);
    }

    @Operation
    @GetMapping(value = "/post2")
    public Future<String> post2(@RequestParam("id") String id) {
        return Future.succeededFuture(id);
    }

    @Operation
    @GetMapping(value = "/enum1")
    public Future<String> enum1(@RequestParam("code") SomeType type) {
        return Future.succeededFuture(type.name());
    }

}
