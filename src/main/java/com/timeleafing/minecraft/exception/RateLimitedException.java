package com.timeleafing.minecraft.exception;

import java.io.Serial;

public class RateLimitedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RateLimitedException(String message) {
        super(message);
    }
}
