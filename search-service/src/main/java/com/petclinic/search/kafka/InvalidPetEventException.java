package com.petclinic.search.kafka;

/** The record is not a valid PetEvent. Retrying can't help: the error handler dead-letters it at once. */
public class InvalidPetEventException extends RuntimeException {

    public InvalidPetEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
