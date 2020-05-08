package io.bleoo.test;

import io.bleoo.annotation.RequestMapping;
import io.bleoo.annotation.Router;

/**
 * @author leo
 * @date 2020/5/6 17:14
 */

@Router
public class TestRouter {

    @RequestMapping("/helloWorld")
    public String helloWorld(){
        return "helloWorld";
    }

}
