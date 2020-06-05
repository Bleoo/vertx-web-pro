package io.vertx.webpro.core.exception;

public class AnnotationEmptyValueException extends RuntimeException {

    @Override
    public String getMessage() {
        return "annotation value is empty!";
    }
}
