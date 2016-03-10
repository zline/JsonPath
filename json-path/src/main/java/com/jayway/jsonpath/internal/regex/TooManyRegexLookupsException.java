package com.jayway.jsonpath.internal.regex;


public class TooManyRegexLookupsException extends RuntimeException {
    public static final long serialVersionUID = 1L;

    public TooManyRegexLookupsException() {
    }

    public TooManyRegexLookupsException(String message) {
        super(message);
    }

    public TooManyRegexLookupsException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
