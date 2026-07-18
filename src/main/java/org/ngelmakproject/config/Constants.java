package org.ngelmakproject.config;

/**
 * Application constants.
 */
public final class Constants {

    private Constants() {
    } // prevent instantiation

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$";

    public static final String SYSTEM = "system";
    public static final String DEFAULT_LANGUAGE = "fr";
    public static final String DEFAULT_ATTACHMENT_LOCAL_DIRECTORY = "attachment-repos";

    // Frontend Auth Routes
    public static final String FRONTEND_AUTH_ACTIVATION = "/activate";
    public static final String FRONTEND_AUTH_RESET_PASSWORD = "/password-reset-finish";
    public static final String FRONTEND_AUTH_VERIFY_EMAIL = "/verify-email";
    public static final String FRONTEND_AUTH_RESEND_ACTIVATION = "/resend-activation";
    public static final String FRONTEND_MODERATION_APPEAL = "/moderation/appeal";
}
