package io.bleoo.test;

import io.bleoo.annotation.RequestMapping;
import io.bleoo.annotation.RequestParam;
import io.bleoo.annotation.Router;
import io.vertx.core.http.HttpMethod;

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

    @RequestMapping(value = "//post1", method = HttpMethod.POST)
    public String post1() {
        return "helloWorld";
    }

}
