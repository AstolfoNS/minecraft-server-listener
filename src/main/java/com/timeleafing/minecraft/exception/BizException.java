package com.timeleafing.minecraft.exception;

import java.io.Serial;

public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    public BizException() {
        super();
    }

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }

    public BizException(Throwable cause) {
        super(cause);
    }
}
