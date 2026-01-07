package org.ngelmakproject.service;

import org.ngelmakproject.domain.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails asynchronously.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
public class MailService {

    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
    }

    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
    }

    public void sendActivationEmail(User user) {
    }

    public void sendCreationEmail(User user) {
    }

    public void sendPasswordResetMail(User user) {
    }
}
