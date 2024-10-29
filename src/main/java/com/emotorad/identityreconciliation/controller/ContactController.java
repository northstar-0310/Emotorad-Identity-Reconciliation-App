package com.emotorad.identityreconciliation.controller;

import com.emotorad.identityreconciliation.exception.ContactNotFoundException;
import com.emotorad.identityreconciliation.model.Contact;
import com.emotorad.identityreconciliation.model.ContactRequest;
import com.emotorad.identityreconciliation.model.ContactResponse;
import com.emotorad.identityreconciliation.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api")
public class ContactController {

    private final ContactService contactService;

    @Autowired
    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/identify")
    public ResponseEntity<ContactResponse> identifyContact(@RequestBody ContactRequest request) {
        ContactResponse response = contactService.identifyContact(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/contact/{id}")
    public ResponseEntity<Contact> getContactById(@PathVariable long id) {
        try {
            Contact contact = contactService.findContactById(id);
            return ResponseEntity.ok(contact);
        } catch (ContactNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

    @GetMapping("/contact")
    public ResponseEntity<Contact> getContactByEmailOrPhone(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber) {
        try {
            Contact contact = contactService.findContactByEmailOrPhone(email, phoneNumber);
            return ResponseEntity.ok(contact);
        } catch (ContactNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }
}
