package io.bleoo.exception;

public class EmptyMethodsException extends RuntimeException {

    @Override
    public String getMessage() {
        return "RequestMapping method 为空";
    }
}
