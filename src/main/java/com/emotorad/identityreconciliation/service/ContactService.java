package com.emotorad.identityreconciliation.service;

import com.emotorad.identityreconciliation.model.Contact;
import com.emotorad.identityreconciliation.model.ContactRequest;
import com.emotorad.identityreconciliation.model.ContactResponse;



public interface ContactService {
    ContactResponse identifyContact(ContactRequest request);
    Contact findContactById(long id);
    Contact findContactByEmailOrPhone(String email, String phoneNumber);
}