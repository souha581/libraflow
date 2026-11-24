package com.libraflow.service;

/**
 * Domain exception thrown when a business rule is violated.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
