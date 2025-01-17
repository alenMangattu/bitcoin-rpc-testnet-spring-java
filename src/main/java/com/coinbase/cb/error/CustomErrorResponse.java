package com.coinbase.cb.error;

import org.springframework.http.HttpStatus;

public class CustomErrorResponse extends RuntimeException {
    private final HttpStatus status;

    public CustomErrorResponse(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
