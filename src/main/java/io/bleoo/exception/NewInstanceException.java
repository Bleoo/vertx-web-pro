package io.bleoo.exception;

public class NewInstanceException extends RuntimeException {

    public NewInstanceException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return "实例创建失败";
    }
}
