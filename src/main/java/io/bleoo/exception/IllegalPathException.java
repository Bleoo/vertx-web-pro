package io.bleoo.exception;

public class IllegalPathException extends RuntimeException {

    @Override
    public String getMessage() {
        return "illegal path!";
    }
}
