package org.ngelmakproject.service.email;

import org.ngelmakproject.domain.User;
import org.ngelmakproject.web.rest.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/email")
public class EmailFastTestController {
    private static final Logger log = LoggerFactory.getLogger(EmailFastTestController.class);

    private static final User user = new User();

    @Value("${spring.application.name}")
    private String applicationName;

    private final MailService mailService;

    public EmailFastTestController(MailService mailService) {
        this.mailService = mailService;
        user.setFirstName("Youssouph");
        user.setLastName("Faye");
        user.setEmail("youssouph.faye@gmail.com");
        user.setActivationKey(RandomUtil.generateKey());
    }

    @PostMapping("/activation")
    public String sendActivationMail() {
        mailService.sendActivationEmail(user);
        log.info("Activation email sent to {}", user);
        return "Activation email sent successfully.";
    }

    @PostMapping("/activation-email")
    public String sendMail() {
        mailService.sendActivationEmail(user);
        log.info("Activation email sent to {}", user);
        return "Activation email sent successfully.";
    }
}
