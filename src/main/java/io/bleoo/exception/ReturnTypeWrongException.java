package io.bleoo.exception;

public class ReturnTypeWrongException extends RuntimeException {

    @Override
    public String getMessage() {
        return "return type wrong!";
    }
}
