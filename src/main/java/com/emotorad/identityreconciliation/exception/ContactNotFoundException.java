package com.emotorad.identityreconciliation.exception;

public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException(String message) {
        super(message);
    }

    public ContactNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContactNotFoundException(long contactId) {
        super("Contact not found with id: " + contactId);
    }

    public ContactNotFoundException(String email, String phoneNumber) {
        super("Contact not found with email: " + email + " or phone number: " + phoneNumber);
    }
}