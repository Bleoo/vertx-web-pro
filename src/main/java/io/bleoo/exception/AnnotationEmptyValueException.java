package io.bleoo.exception;

public class AnnotationEmptyValueException extends RuntimeException {

    @Override
    public String getMessage() {
        return "注解里有空值";
    }
}
