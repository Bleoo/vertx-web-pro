package io.vertx.webpro.core.annotation;

import io.vertx.core.http.HttpMethod;

import java.lang.annotation.*;

/**
 * @author leo
 * @date 2020/5/6 17:25
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Mapping
public @interface RequestMapping {

    String[] value() default {};

    HttpMethod[] method() default {};

    String[] consumes() default {};

    String[] produces() default {};

}
