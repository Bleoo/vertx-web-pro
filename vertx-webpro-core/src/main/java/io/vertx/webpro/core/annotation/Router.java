package io.vertx.webpro.core.annotation;

import java.lang.annotation.*;

/**
 * @author leo
 * @date 2020/5/6 17:25
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Router {

    String name() default "";

    String value() default "/";

}
