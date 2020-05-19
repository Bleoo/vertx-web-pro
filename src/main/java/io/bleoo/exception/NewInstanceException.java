package io.bleoo.exception;

public class NewInstanceException extends RuntimeException {

    public NewInstanceException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return "new instance failed!";
    }
}
