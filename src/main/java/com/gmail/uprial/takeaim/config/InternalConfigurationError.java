package com.gmail.uprial.takeaim.config;

@SuppressWarnings("ExceptionClassNameDoesntEndWithException")
class InternalConfigurationError extends RuntimeException {
    InternalConfigurationError(String message) {
        super(message);
    }
}
