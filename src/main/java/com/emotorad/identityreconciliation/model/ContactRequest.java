package com.emotorad.identityreconciliation.model;

import lombok.Data;

@Data
public class ContactRequest {
    private String email;
    private String phoneNumber;
}