package com.petclinic.petservice.exception;

public class CageUnavailableException extends RuntimeException {
    public CageUnavailableException(String message) {
        super(message);
    }
}
