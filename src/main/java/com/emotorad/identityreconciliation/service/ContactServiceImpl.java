package com.emotorad.identityreconciliation.service;

import com.emotorad.identityreconciliation.exception.ContactNotFoundException;
import com.emotorad.identityreconciliation.model.Contact;
import com.emotorad.identityreconciliation.model.ContactRequest;
import com.emotorad.identityreconciliation.model.ContactResponse;
import com.emotorad.identityreconciliation.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Override
    @Transactional
    public ContactResponse identifyContact(ContactRequest request) {
        Optional<Contact> existingContactByEmail = contactRepository.findByEmail(request.getEmail().toLowerCase());
        Optional<Contact> existingContactByPhone = contactRepository.findByPhoneNumber(request.getPhoneNumber());

        if (existingContactByEmail.isPresent() && existingContactByPhone.isPresent() 
            && !existingContactByEmail.get().equals(existingContactByPhone.get())) {
            return linkExistingContacts(existingContactByEmail.get(), existingContactByPhone.get());
        } else if (existingContactByEmail.isPresent()) {
            return processExistingContact(existingContactByEmail.get(), request);
        } else if (existingContactByPhone.isPresent()) {
            return processExistingContact(existingContactByPhone.get(), request);
        } else {
            return createNewPrimaryContact(request);
        }
    }

    private ContactResponse linkExistingContacts(Contact contact1, Contact contact2) {
        Contact primaryContact = contact1.getLinkPrecedence() == Contact.LinkPrecedence.PRIMARY ? contact1 : contact2;
        Contact secondaryContact = primaryContact.equals(contact1) ? contact2 : contact1;

        secondaryContact.setLinkPrecedence(Contact.LinkPrecedence.SECONDARY);
        secondaryContact.setLinkedContact(primaryContact);
        contactRepository.save(secondaryContact);

        return createContactResponse(primaryContact, secondaryContact);
    }

    private ContactResponse createNewPrimaryContact(ContactRequest request) {
        Contact newContact = new Contact();
        newContact.setEmail(request.getEmail().toLowerCase());
        newContact.setPhoneNumber(request.getPhoneNumber());
        newContact.setLinkPrecedence(Contact.LinkPrecedence.PRIMARY);
        contactRepository.save(newContact);

        return createContactResponse(newContact, null);
    }

    private ContactResponse processExistingContact(Contact existingContact, ContactRequest request) {
        Contact primaryContact = (existingContact.getLinkPrecedence() == Contact.LinkPrecedence.PRIMARY) ?
                existingContact : existingContact.getLinkedContact();

        if (primaryContact == null) {
            primaryContact = existingContact;
            primaryContact.setLinkPrecedence(Contact.LinkPrecedence.PRIMARY);
            contactRepository.save(primaryContact);
        }

        Contact newSecondaryContact = null;
        if (!request.getEmail().equalsIgnoreCase(existingContact.getEmail()) || !request.getPhoneNumber().equals(existingContact.getPhoneNumber())) {
            newSecondaryContact = new Contact();
            newSecondaryContact.setEmail(request.getEmail().toLowerCase());
            newSecondaryContact.setPhoneNumber(request.getPhoneNumber());
            newSecondaryContact.setLinkPrecedence(Contact.LinkPrecedence.SECONDARY);
            newSecondaryContact.setLinkedContact(primaryContact);
            contactRepository.save(newSecondaryContact);
        }

        return createContactResponse(primaryContact, newSecondaryContact);
    }

    private ContactResponse createContactResponse(Contact primaryContact, Contact newSecondaryContact) {
        List<Contact> allLinkedContacts = new ArrayList<>(contactRepository.findAllByLinkedContact(primaryContact));
        allLinkedContacts.add(primaryContact);
        if (newSecondaryContact != null && !allLinkedContacts.contains(newSecondaryContact)) {
            allLinkedContacts.add(newSecondaryContact);
        }

        ContactResponse response = new ContactResponse();
        response.setPrimaryContactId(primaryContact.getId());

        Set<String> emails = new HashSet<>();
        Set<String> phoneNumbers = new HashSet<>();
        List<Long> secondaryContactIds = new ArrayList<>();

        for (Contact contact : allLinkedContacts) {
            if (contact.getEmail() != null) emails.add(contact.getEmail());
            if (contact.getPhoneNumber() != null) phoneNumbers.add(contact.getPhoneNumber());
            if (contact.getLinkPrecedence() == Contact.LinkPrecedence.SECONDARY) {
                secondaryContactIds.add(contact.getId());
            }
        }

        response.setEmails(new ArrayList<>(emails));
        response.setPhoneNumbers(new ArrayList<>(phoneNumbers));
        response.setSecondaryContactIds(secondaryContactIds);

        return response;
    }

    @Override
    public Contact findContactById(long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));
    }

    @Override
    public Contact findContactByEmailOrPhone(String email, String phoneNumber) {
        return contactRepository.findByEmail(email.toLowerCase())
                .orElse(contactRepository.findByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new ContactNotFoundException(email, phoneNumber)));
    }
}