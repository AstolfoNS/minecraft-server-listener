package com.timeleafing.minecraft.exception;

import java.io.Serial;

public class RateLimitedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    public RateLimitedException() {
        super();
    }

    public RateLimitedException(String message) {
        super(message);
    }

    public RateLimitedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RateLimitedException(Throwable cause) {
        super(cause);
    }
}
