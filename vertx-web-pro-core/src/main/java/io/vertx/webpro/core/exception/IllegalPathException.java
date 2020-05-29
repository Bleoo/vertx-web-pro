package io.vertx.webpro.core.exception;

public class IllegalPathException extends RuntimeException {

    @Override
    public String getMessage() {
        return "illegal path!";
    }
}
