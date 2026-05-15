package org.ngelmakproject.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.BatchSize;
import org.ngelmakproject.config.Constants;
import org.ngelmakproject.domain.enumeration.CertificationStatus;
import org.ngelmakproject.domain.enumeration.DocType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A user.
 */
@Entity
@Table(name = "user_ngelmak")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_ngelmak_seq")
    @SequenceGenerator(name = "user_ngelmak_seq", sequenceName = "user_ngelmak_seq", allocationSize = 50)
    private Long id;

    // The username/login of the user, which must be unique.
    @NotNull
    @Pattern(regexp = Constants.LOGIN_REGEX)
    @Size(min = 1, max = 50)
    @Column(length = 50, unique = true, nullable = false)
    private String login;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column(name = "password_hash", length = 60, nullable = false)
    private String password;

    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    /**
     * The user's email address, which can be used for account activation, password
     * reset, and notifications.
     */
    @Email
    @Size(min = 5, max = 254)
    @Column(length = 254, nullable = true)
    private String email;

    /* If the user has activated their account via email confirmation. */
    @NotNull
    @Column(nullable = false)
    private boolean activated = false;

    /* Token used for account activation and email verification. */
    @JsonIgnore
    @Size(max = 20)
    @Column(name = "activation_key", length = 20)
    private String activationKey;

    /* Timestamp marking when the activation key was generated. */
    @JsonIgnore
    @Column(name = "activation_date")
    private Instant activationDate = null;

    /* Token used to initiate a password reset process. */
    @JsonIgnore
    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    private String resetKey;

    /* Timestamp marking when the password reset was requested. */
    @JsonIgnore
    @Column(name = "reset_date")
    private Instant resetDate = null;

    /*
     * If the user account is blocked due to too many failed login attempts or
     * admin.
     */
    @NotNull
    @Column(nullable = false)
    private boolean blocked = false;

    /*
     * For storing the user's preferred language, e.g. "en", "fr", "es"
     */
    @Size(min = 2, max = 5)
    @Column(name = "lang_key", length = 5)
    private String langKey;

    /* URL of the user's profile image. */
    @Size(max = 256)
    @Column(name = "image_url", length = 256)
    private String imageUrl;

    /* Timestamp of the last modification made to the user record. */
    @JsonIgnore
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate = null;

    /* Timestamp indicating when the user record was initially created. */
    @Column(name = "created_date")
    private Instant createdDate = null;

    /* Timestamp marking when the user account was soft‑deleted. */
    @Column(name = "deleted_date")
    private Instant deletedDate = null;

    /* Timestamp indicating when the user's certification was granted. */
    @JsonIgnore
    @Column(name = "certified_date")
    private Instant certifiedDate = null;

    /* Current certification status of the user. */
    @Enumerated(EnumType.STRING)
    @Column(name = "certification_status", length = 20)
    private CertificationStatus certificationStatus = CertificationStatus.NOT_REQUESTED;

    /*
     * Type of document provided for certification, e.g. PASSPORT, ID_CARD,
     * DRIVER_LICENSE.
     */
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", length = 20)
    private DocType docType;

    /*
     * A hash of the document ID provided for certification. This is stored as a
     * hash for security reasons, so that even if the database is compromised, the
     * actual document IDs cannot be easily retrieved.
     */
    @JsonIgnore
    @Column(name = "doc_id_hash", length = 60)
    private String docId;

    /*
     * User's preferred timezone, e.g. "America/New_York", "Europe/Paris". This can
     * be used to display dates/times in the user's local time and for scheduling
     * notifications or events at the correct time for the user.
     */
    @Column(name = "timezone")
    @Size(max = 50)
    private String timezone;

    /*
     * Whether the user has enabled dark mode in their preferences. This can be used
     * to customize the UI theme for the user.
     */
    @Column(name = "dark_mode_enabled")
    private Boolean darkModeEnabled = false;

    /*
     * The authorities/roles that are assigned to the user. This is a many-to-many
     * relationship because a user can have multiple roles and a role can be
     * assigned
     * to multiple users.
     */
    @JsonIgnore
    @ManyToMany(cascade = CascadeType.REMOVE)
    @JoinTable(name = "user_authority", joinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "id") }, inverseJoinColumns = {
                    @JoinColumn(name = "authority_name", referencedColumnName = "name") })
    @BatchSize(size = 20)
    private Set<Authority> authorities = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    // Lowercase the login before saving it in database
    public void setLogin(String login) {
        this.login = StringUtils.lowerCase(login, Locale.ENGLISH);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CertificationStatus getCertificationStatus() {
        return certificationStatus;
    }

    public void setCertificationStatus(CertificationStatus certificationStatus) {
        this.certificationStatus = certificationStatus;
    }

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Instant getCertifiedDate() {
        return certifiedDate;
    }

    public void setCertifiedDate(Instant certifiedDate) {
        this.certifiedDate = certifiedDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModofiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Instant deletedDate) {
        this.deletedDate = deletedDate;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public Instant getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(Instant activationDate) {
        this.activationDate = activationDate;
    }

    public String getResetKey() {
        return resetKey;
    }

    public void setResetKey(String resetKey) {
        this.resetKey = resetKey;
    }

    public Instant getResetDate() {
        return resetDate;
    }

    public void setResetDate(Instant resetDate) {
        this.resetDate = resetDate;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkModeEnabled(boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public Set<Authority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        return id != null && id.equals(((User) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "login='" + login + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", activated='" + activated + '\'' +
                ", langKey='" + langKey + '\'' +
                "}";
    }
}
