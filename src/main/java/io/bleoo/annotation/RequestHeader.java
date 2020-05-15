package io.bleoo.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestHeader {

	String value() default "";

	String name() default "";

	String defaultValue() default ValueConstants.DEFAULT_NONE;

}