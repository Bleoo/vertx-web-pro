package io.bleoo.test;

import io.bleoo.annotation.*;
import io.bleoo.test.dto.Query;
import io.bleoo.test.dto.Result;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author leo
 * @date 2020/5/6 17:14
 */

@Router
public class TestRouter {

    @RequestMapping(value = "/get", method = HttpMethod.GET)
    public String get() {
        return "helloWorld";
    }

    @RequestMapping(value = "/get1", method = HttpMethod.GET)
    public String get1(@RequestParam("id") Integer id) {
        return String.valueOf(id);
    }

    @RequestMapping(value = "/get2/:id", method = HttpMethod.GET)
    public String get2(@PathVariable("id") Integer id) {
        return String.valueOf(id);
    }

    @RequestMapping(value = "//post1", method = HttpMethod.POST)
    public String post1() {
        return "helloWorld";
    }

    @RequestMapping(value = "/post2", method = HttpMethod.POST)
    public Result<String> post2(@RequestBody Query query) {
        Result<String> result = new Result<>();
        result.setRequestId(query.getRequestId());
        result.setPage(query.getPage());
        result.setSize(query.getSize());
        List<String> data = new ArrayList<>();
        data.add("test1");
        data.add("test2");
        result.setData(data);
        return result;
    }

}
