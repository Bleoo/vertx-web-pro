package io.bleoo.annotation;

import io.vertx.core.http.HttpMethod;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = HttpMethod.PUT)
public @interface PutMapping {

    String name() default "";

    String[] value() default {};

    String[] path() default {};

}
