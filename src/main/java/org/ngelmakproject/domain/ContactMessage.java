package org.ngelmakproject.domain;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * The ContactMessage entity. Represents a message sent by a user through the
 * contact form.
 * It can be used for support requests, feedback, or general inquiries.
 */
@Entity
@Table(name = "nk_contact_message")
public class ContactMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name; // optional if anonymous allowed

    @Column(name = "email")
    private String email; // optional if anonymous allowed, but useful for support to reply

    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    @Column(name = "message", length = 1000, nullable = false)
    private String message;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private ContactStatus status; // NEW, IN_PROGRESS, CLOSED

    /**
     * The ContactStatus enumeration.
     */
    public enum ContactStatus {
        NEW,
        IN_PROGRESS,
        CLOSED
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public void setStatus(ContactStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ContactMessage [id=" + id + ", name=" + name + ", email=" + email + ", subject=" + subject
                + ", message=" + message + ", createdAt=" + createdAt + ", status=" + status + "]";
    }

}
