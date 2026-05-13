package com.example.platform.common.web;

public class AuthorizationDeniedException extends RuntimeException {

    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
