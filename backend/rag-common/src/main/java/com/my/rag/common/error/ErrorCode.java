package com.my.rag.common.error;

public enum ErrorCode {
    BAD_REQUEST(400, "bad request"),
    INTERNAL_ERROR(500, "internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}

