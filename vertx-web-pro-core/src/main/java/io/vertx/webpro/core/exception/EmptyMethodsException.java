package io.vertx.webpro.core.exception;

public class EmptyMethodsException extends RuntimeException {

    @Override
    public String getMessage() {
        return "RequestMapping method is null!";
    }
}
