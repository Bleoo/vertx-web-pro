package io.bleoo.annotation;

import java.lang.annotation.*;

/**
 * @author leo
 * @date 2020/5/6 17:25
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface RequestParam {

    String value() default "";

    String name() default "";

    String defaultValue() default ValueConstants.DEFAULT_NONE;

}
