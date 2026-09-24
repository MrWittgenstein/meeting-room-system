package com.uestcfir.exception;

public class BusinessException extends RuntimeException{
    private final int status;
    public BusinessException(String message) {
        this(200, message);
    }
    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
