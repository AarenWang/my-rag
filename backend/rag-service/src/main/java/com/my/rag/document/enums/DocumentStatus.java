package com.my.rag.document.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum DocumentStatus {
    UPLOADED("UPLOADED"),
    PARSING("PARSING"),
    PARSED("PARSED"),
    CHUNKING("CHUNKING"),
    CHUNKED("CHUNKED"),
    EMBEDDING("EMBEDDING"),
    READY("READY"),
    FAILED("FAILED");

    @EnumValue private final String value;

    DocumentStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

