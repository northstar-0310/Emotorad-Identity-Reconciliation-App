package com.emotorad.identityreconciliation.service;

import com.emotorad.identityreconciliation.model.Contact;
import com.emotorad.identityreconciliation.model.ContactRequest;
import com.emotorad.identityreconciliation.model.ContactResponse;
import com.emotorad.identityreconciliation.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContactServiceImplTest {

    @InjectMocks
    private ContactServiceImpl contactService;

    @Mock
    private ContactRepository contactRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateNewPrimaryContact() {
        ContactRequest request = new ContactRequest();
        request.setEmail("NEW@EXAMPLE.com");
        request.setPhoneNumber("1234567890");

        when(contactRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(contactRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(contactRepository.save(any())).thenAnswer(invocation -> {
            Contact savedContact = invocation.getArgument(0);
            savedContact.setId(1L);
            return savedContact;
        });

        ContactResponse response = contactService.identifyContact(request);

        assertNotNull(response);
        assertEquals(1L, response.getPrimaryContactId());
        assertEquals(1, response.getEmails().size());
        assertEquals("new@example.com", response.getEmails().get(0));
        assertEquals(1, response.getPhoneNumbers().size());
        assertEquals("1234567890", response.getPhoneNumbers().get(0));
        assertTrue(response.getSecondaryContactIds().isEmpty());

        verify(contactRepository, times(1)).save(any());
    }

    @Test
    void testProcessExistingContactWithNewInfo() {
        ContactRequest request = new ContactRequest();
        request.setEmail("new@example.com");
        request.setPhoneNumber("9876543210");

        Contact existingContact = new Contact();
        existingContact.setId(1L);
        existingContact.setEmail("existing@example.com");
        existingContact.setPhoneNumber("1234567890");
        existingContact.setLinkPrecedence(Contact.LinkPrecedence.PRIMARY);

        when(contactRepository.findByEmail(anyString())).thenReturn(Optional.of(existingContact));
        when(contactRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(contactRepository.save(any())).thenAnswer(invocation -> {
            Contact savedContact = invocation.getArgument(0);
            if (savedContact.getId() == null) {
                savedContact.setId(2L);
            }
            return savedContact;
        });
        when(contactRepository.findAllByLinkedContact(any())).thenReturn(Arrays.asList(existingContact));

        ContactResponse response = contactService.identifyContact(request);

        assertNotNull(response);
        assertEquals(1L, response.getPrimaryContactId());
        assertEquals(2, response.getEmails().size());
        assertTrue(response.getEmails().contains("existing@example.com"));
        assertTrue(response.getEmails().contains("new@example.com"));
        assertEquals(2, response.getPhoneNumbers().size());
        assertTrue(response.getPhoneNumbers().contains("1234567890"));
        assertTrue(response.getPhoneNumbers().contains("9876543210"));
        assertEquals(1, response.getSecondaryContactIds().size());
        assertEquals(2L, response.getSecondaryContactIds().get(0));

        verify(contactRepository, times(1)).save(any());
    }

    @Test
    void testAvoidDuplicateSecondaryContact() {
        ContactRequest request = new ContactRequest();
        request.setEmail("duplicate@example.com");
        request.setPhoneNumber("1234567890");

        Contact existingPrimaryContact = new Contact();
        existingPrimaryContact.setId(1L);
        existingPrimaryContact.setEmail("duplicate@example.com");
        existingPrimaryContact.setPhoneNumber("1234567890");
        existingPrimaryContact.setLinkPrecedence(Contact.LinkPrecedence.PRIMARY);

        when(contactRepository.findByEmail(anyString())).thenReturn(Optional.of(existingPrimaryContact));
        when(contactRepository.findByPhoneNumber(anyString())).thenReturn(Optional.of(existingPrimaryContact));
        when(contactRepository.findAllByLinkedContact(any())).thenReturn(Arrays.asList(existingPrimaryContact));

        ContactResponse response = contactService.identifyContact(request);

        assertNotNull(response);
        assertEquals(1L, response.getPrimaryContactId());
        assertEquals(1, response.getEmails().size());
        assertEquals(1, response.getPhoneNumbers().size());
        assertTrue(response.getSecondaryContactIds().isEmpty());

        verify(contactRepository, never()).save(any());
    }

    @Test
    void testIdempotency() {
        ContactRequest request = new ContactRequest();
        request.setEmail("test@example.com");
        request.setPhoneNumber("1234567890");

        Contact existingContact = new Contact();
        existingContact.setId(1L);
        existingContact.setEmail("test@example.com");
        existingContact.setPhoneNumber("1234567890");
        existingContact.setLinkPrecedence(Contact.LinkPrecedence.PRIMARY);

        when(contactRepository.findByEmail(anyString())).thenReturn(Optional.of(existingContact));
        when(contactRepository.findByPhoneNumber(anyString())).thenReturn(Optional.of(existingContact));
        when(contactRepository.findAllByLinkedContact(any())).thenReturn(Arrays.asList(existingContact));

        ContactResponse response1 = contactService.identifyContact(request);
        ContactResponse response2 = contactService.identifyContact(request);

        assertEquals(response1, response2);
        verify(contactRepository, never()).save(any());
    }

    @Test
    void testLinkExistingContacts() {
        ContactRequest request = new ContactRequest();
        request.setEmail("john.doe@example.com");
        request.setPhoneNumber("5555666677");

        Contact existingContactByEmail = new Contact();
        existingContactByEmail.setId(1L);
        existingContactByEmail.setEmail("john.doe@example.com");
        existingContactByEmail.setPhoneNumber("1234567890");
        existingContactByEmail.setLinkPrecedence(Contact.LinkPrecedence.PRIMARY);

        Contact existingContactByPhone = new Contact();
        existingContactByPhone.setId(2L);
        existingContactByPhone.setEmail("different@example.com");
        existingContactByPhone.setPhoneNumber("5555666677");
        existingContactByPhone.setLinkPrecedence(Contact.LinkPrecedence.PRIMARY);

        when(contactRepository.findByEmail(anyString())).thenReturn(Optional.of(existingContactByEmail));
        when(contactRepository.findByPhoneNumber(anyString())).thenReturn(Optional.of(existingContactByPhone));
        when(contactRepository.findAllByLinkedContact(any())).thenReturn(Arrays.asList(existingContactByEmail, existingContactByPhone));
        when(contactRepository.save(any())).thenReturn(existingContactByPhone);

        ContactResponse response = contactService.identifyContact(request);

        assertNotNull(response);
        assertEquals(1L, response.getPrimaryContactId());
        assertEquals(2, response.getEmails().size());
        assertTrue(response.getEmails().contains("john.doe@example.com"));
        assertTrue(response.getEmails().contains("different@example.com"));
        assertEquals(2, response.getPhoneNumbers().size());
        assertTrue(response.getPhoneNumbers().contains("1234567890"));
        assertTrue(response.getPhoneNumbers().contains("5555666677"));
        assertEquals(1, response.getSecondaryContactIds().size());
        assertEquals(2L, response.getSecondaryContactIds().get(0));

        verify(contactRepository, times(1)).save(any());
    }
}