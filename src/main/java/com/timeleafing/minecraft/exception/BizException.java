package com.timeleafing.minecraft.exception;

import java.io.Serial;

public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BizException(String message) {
        super(message);
    }
}
