package com.emotorad.identityreconciliation.repository;

import com.emotorad.identityreconciliation.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    Optional<Contact> findByEmail(String email);
    Optional<Contact> findByPhoneNumber(String phoneNumber);
    List<Contact> findAllByLinkedContact(Contact linkedContact);
}