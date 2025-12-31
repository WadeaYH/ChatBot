package com.university.chatbotyarmouk.exception;

public class CrawlerException extends RuntimeException {
    public CrawlerException() {
        super();
    }

    public CrawlerException(String message) {
        super(message);
    }

    public CrawlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
