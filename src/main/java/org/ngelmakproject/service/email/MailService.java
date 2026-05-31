package org.ngelmakproject.service.email;

import java.time.Year;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.ngelmakproject.config.Constants;
import org.ngelmakproject.domain.User;
import org.ngelmakproject.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending emails asynchronously.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
public class MailService {
	private static final Logger log = LoggerFactory.getLogger(AdminService.class);

	@Value("${spring.application.name}")
	private String applicationName;
	@Value("${frontend.api.url}")
	private String frontendApiUrl;
	@Value("${spring.application.support.email}")
	private String supportEmail;
	@Value("${spring.application.support.address}")
	private String address;
	@Value("${spring.application.support.facebook-url}")
	private String facebookUrl;
	@Value("${spring.application.support.twitter-url}")
	private String twitterUrl;
	@Value("${spring.application.support.instagram-url}")
	private String instagramUrl;
	@Value("${spring.mail.username}")
	private String sender;

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final MessageSource messageSource;

	public MailService(JavaMailSender mailSender, TemplateEngine templateEngine, MessageSource messageSource) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.messageSource = messageSource;
	}

	private void sendEmailSync(String to, String subject, String htmlContent) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(to);
			helper.setFrom(sender);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);

			mailSender.send(message);

		} catch (Exception e) {
			throw new IllegalStateException("Failed to send email", e);
		}
	}

	@Async
	public void sendEmail(String to, String subject, String htmlContent) {
		sendEmailSync(to, subject, htmlContent);
	}

	/**
	 * Builds a frontend URL with the given base, path, and query parameters.
	 * This is used to generate links in emails that point to the frontend
	 * application.
	 * 
	 * @param base   the base URL of the frontend application (e.g.,
	 *               https://ngelmak.org)
	 * @param path   the path of the frontend route
	 * @param params the query parameters for the URL
	 * @return the constructed frontend URL
	 */
	private String buildFrontendUrl(String path, Map<String, Object> params) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(frontendApiUrl).path(path);
		params.forEach(builder::queryParam);
		return builder.toUriString();
	}

	/**
	 * Sends an email using a Thymeleaf template.
	 *
	 * @param to           the recipient's email address
	 * @param subject      the email subject
	 * @param keyLang      the language key for localization (e.g., "en", "fr")
	 * @param bodyTemplate the name of the Thymeleaf template for the email body
	 * @param context      the context for the Thymeleaf template
	 */
	public void sendEmailFromTemplate(
			String to,
			String subjectKey,
			String keyLang,
			String bodyTemplate,
			EmailContext context) {
		log.info("📧 Preparing email to {} with subject key '{}'", to, subjectKey);

		Locale locale = Locale.forLanguageTag(keyLang); // e.g. "fr"

		Context thymeleaf = new Context(locale);

		thymeleaf.setVariable("uniqueId", UUID.randomUUID().toString());
		thymeleaf.setVariable("year", Year.now().getValue());
		thymeleaf.setVariable("headerTitle", subjectKey);
		thymeleaf.setVariable("supportEmail", supportEmail);
		thymeleaf.setVariable("address", address);
		thymeleaf.setVariable("facebookUrl", facebookUrl);
		thymeleaf.setVariable("twitterUrl", twitterUrl);
		thymeleaf.setVariable("instagramUrl", instagramUrl);
		thymeleaf.setVariable("applicationName", applicationName);
		thymeleaf.setVariable("frontendUrl", frontendApiUrl);
		thymeleaf.setVariable("bodyTemplate", "mail/" + bodyTemplate);

		context.getValues().forEach(thymeleaf::setVariable);

		// Render HTML with locale-aware message resolution
		String html = templateEngine.process("mail/layout", thymeleaf);
		log.info("Generated HTML for email to {}: {}", to, html);
		thymeleaf.getVariableNames().forEach(
				name -> log.debug("{} = {}", name, thymeleaf.getVariable(name)));

		// Resolve subject using MessageSource + locale
		String subject = messageSource.getMessage(subjectKey, null, locale);

		sendEmail(to, subject, html);
	}

	/**
	 * Sends an account activation email to the user with a link to activate their
	 * account.
	 *
	 * @param user the user to send the activation email to
	 */
	public void sendActivationEmail(User user) {
		String activationUrl = buildFrontendUrl(Constants.FRONTEND_AUTH_ACTIVATION,
				Map.of("key", user.getActivationKey()));
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName())
				.set("activationUrl", activationUrl);

		sendEmailFromTemplate(
				user.getEmail(),
				"email.activation.subject",
				user.getLangKey(),
				"activationEmail",
				ctx);
	}

	/**
	 * Sends an email verification email to the user with a link to verify their new
	 * email address.
	 *
	 * @param user     the user to send the email verification email to
	 * @param newEmail the new email address to verify
	 */
	public void sendEmailVerificationEmail(User user) {
		String verificationUrl = buildFrontendUrl(Constants.FRONTEND_AUTH_ACTIVATION,
				Map.of("key", user.getActivationKey()));
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName())
				.set("verificationUrl", verificationUrl);

		sendEmailFromTemplate(
				user.getEmail(),
				"email.verification.subject",
				user.getLangKey(),
				"emailVerification",
				ctx);
	}

	/**
	 * Sends a confirmation email to the user after they have successfully activated
	 * their account.
	 *
	 * @param user the user to send the activation confirmation email to
	 */
	public void sendWelcomeEmail(User user) {
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName());

		sendEmailFromTemplate(
				user.getEmail(),
				"email.welcome.subject",
				user.getLangKey(),
				"welcomeEmail",
				ctx);
	}

	// @PostConstruct
	public void sendTestEmail() {
		log.info("📧 Sending test email to verify configuration...");
		User testUser = new User();
		testUser.setFirstName("Youssouph");
		testUser.setLastName("Faye");
		testUser.setLangKey("en");
		testUser.setEmail("youssouph.faye@gmail.com");
		testUser.setResetKey(UUID.randomUUID().toString());
		sendWelcomeEmail(testUser);
		// sendPasswordResetEmail(testUser);
	}

	/**
	 * Sends a password reset email to the user with a link to reset their
	 * password.
	 *
	 * @param user the user to send the password reset email to
	 */
	public void sendPasswordResetEmail(User user) {
		String resetUrl = buildFrontendUrl(Constants.FRONTEND_AUTH_RESET_PASSWORD,
				Map.of("key", user.getResetKey()));
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName())
				.set("resetUrl", resetUrl);

		sendEmailFromTemplate(
				user.getEmail(),
				"email.password.reset.subject",
				user.getLangKey(),
				"passwordResetEmail",
				ctx);
	}

	public void sendModerationDecisionEmail(User user) {
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName())
				.set("contentType", "Poste")
				.set("decisionReason", "Non-respect des règles de civilité")
				.set("decisionAction", "Retrait du contenu")
				.set("additionalNotes",
						"Cette mesure vise uniquement à préserver la qualité de l'espace communautaire.")
				.set("appealUrl", "https://nglmk.org/moderation/appeal/123");

		sendEmailFromTemplate(
				user.getEmail(),
				"email.moderation.decision.subject",
				user.getLangKey(),
				"moderationDecision",
				ctx);
	}

	/**
	 * Sends an acknowledgment email to a user who has requested moderator access.
	 *
	 * @param user the user to send the acknowledgment email to
	 */
	public void sendModeratorRequestAcknowledgmentEmail(User user) {
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName());

		sendEmailFromTemplate(
				user.getEmail(),
				"email.moderation.request.subject",
				user.getLangKey(),
				"moderatorRequestAck",
				ctx);
	}

	/**
	 * Sends an email to a user notifying them that their request for moderator
	 * access has been accepted.
	 *
	 * @param user the user to send the acceptance email to
	 */
	public void sendModeratorAcceptanceEmail(User user) {
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName());

		sendEmailFromTemplate(
				user.getEmail(),
				"email.moderation.acceptance.subject",
				user.getLangKey(),
				"moderationAcceptance",
				ctx);
	}

	/**
	 * Sends an email to a user notifying them that they have been suspended from
	 * the platform, including the reason for the suspension and the duration.
	 *
	 * @param user        the user to send the suspension notice email to
	 * @param duration    the duration of the suspension (e.g., "7 days",
	 *                    "permanent")
	 * @param reason      the reason for the suspension
	 * @param contentType the type of content that led to the suspension (e.g.,
	 *                    "Post", "Comment")
	 * @param appealId    the ID of the moderation appeal that the user can submit
	 *                    if they wish to appeal the suspension
	 */
	public void sendSuspensionNoticeEmail(User user, String duration, String reason, String contentType,
			Long appealId) {
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName())
				.set("suspensionDuration", duration)
				.set("suspensionReason", reason)
				.set("contentType", contentType)
				.set("additionalNotes",
						"Cette mesure vise à protéger la communauté et à préserver un espace d'échange sain.")
				.set("appealUrl",
						buildFrontendUrl(Constants.FRONTEND_MODERATION_APPEAL, Map.of("id", appealId)));

		sendEmailFromTemplate(
				user.getEmail(),
				"email.suspension.subject",
				user.getLangKey(),
				"suspensionNotice",
				ctx);
	}

	public void sendCommunityAnnouncementEmail(User user) {
		EmailContext ctx = new EmailContext()
				.set("firstName", user.getFirstName())
				.set("announcementIntro", "Une nouvelle étape importante vient d'être franchie.")
				.set("announcementTitle", "Lancement d'une nouvelle fonctionnalité")
				.set("announcementBody",
						"Nous introduisons aujourd'hui un nouvel espace dédié aux contributions longues...")
				.set("announcementDetails",
						"Cette évolution s'inscrit dans notre volonté de renforcer l'autonomie et la participation collective.")
				.set("actionLabel", "Voir l'annonce");

		sendEmailFromTemplate(
				user.getEmail(),
				"email.community.announcement.subject",
				user.getLangKey(),
				"communityAnnouncement",
				ctx);
	}
}