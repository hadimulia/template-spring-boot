package com.template.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String redirectUrl;

    public BusinessException(String message) {
        super(message);
        this.redirectUrl = null;
    }

    public BusinessException(String message, String redirectUrl) {
        super(message);
        this.redirectUrl = redirectUrl;
    }
}
