package io.vertx.webpro.core.exception;

public class ReturnTypeWrongException extends RuntimeException {

    @Override
    public String getMessage() {
        return "return type wrong!";
    }
}
