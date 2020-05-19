package io.bleoo.exception;

public class AnnotationEmptyValueException extends RuntimeException {

    @Override
    public String getMessage() {
        return "annotation value is empty!";
    }
}
