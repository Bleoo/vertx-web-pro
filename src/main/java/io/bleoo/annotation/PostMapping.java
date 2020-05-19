package io.bleoo.annotation;

import io.vertx.core.http.HttpMethod;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = HttpMethod.POST)
public @interface PostMapping {

    String name() default "";

    String[] value() default {};

    String[] path() default {};

}
