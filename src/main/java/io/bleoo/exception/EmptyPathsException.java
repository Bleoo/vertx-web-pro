package io.bleoo.exception;

public class EmptyPathsException extends RuntimeException {

    @Override
    public String getMessage() {
        return "RequestMapping path is null!";
    }
}
