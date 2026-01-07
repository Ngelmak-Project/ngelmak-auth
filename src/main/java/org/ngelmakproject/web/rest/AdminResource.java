// package org.ngelmakproject.web.rest;

// import java.net.URI;
// import java.net.URISyntaxException;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.List;
// import java.util.Optional;
// import java.util.concurrent.TimeUnit;

// import org.ngelmakproject.config.Constants;
// import org.ngelmakproject.domain.User;
// import org.ngelmakproject.repository.UserRepository;
// import org.ngelmakproject.security.AuthoritiesConstants;
// import org.ngelmakproject.service.UserService;
// import org.ngelmakproject.service.dto.AccountCertificationRequestDTO;
// import org.ngelmakproject.service.dto.AdminUserDTO;
// import org.ngelmakproject.service.dto.PageDTO;
// import org.ngelmakproject.web.rest.errors.BadRequestAlertException;
// import org.ngelmakproject.web.rest.errors.EmailAlreadyUsedException;
// import org.ngelmakproject.web.rest.errors.LoginAlreadyUsedException;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springdoc.api.annotations.ParameterObject;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.data.domain.Pageable;
// import org.springframework.data.domain.Sort;
// import org.springframework.http.CacheControl;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.PutMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import jakarta.validation.Valid;
// import jakarta.validation.constraints.Pattern;
// import tech.jhipster.web.util.HeaderUtil;
// import tech.jhipster.web.util.ResponseUtil;

// /**
//  * REST controller for managing {@link org.ngelmakproject.domain.User}.
//  */
// @RestController
// @RequestMapping("/api/admin")
// public class AdminResource {

//     private static final List<String> ALLOWED_ORDERED_PROPERTIES = Collections.unmodifiableList(
//             Arrays.asList(
//                     "id",
//                     "login",
//                     "firstName",
//                     "lastName",
//                     "email",
//                     "activated",
//                     "langKey",
//                     "createdBy",
//                     "createdDate",
//                     "lastModifiedBy",
//                     "lastModifiedDate"));

//     private static final Logger log = LoggerFactory.getLogger(AdminResource.class);

//     @Value("${ngelmak.clientApp.name}")
//     private String applicationName;

//     private final UserService userService;

//     private final UserRepository userRepository;

//     private final MailService mailService;

//     public AdminResource(UserService userService, UserRepository userRepository, MailService mailService) {
//         this.userService = userService;
//         this.userRepository = userRepository;
//         this.mailService = mailService;
//     }

//     /**
//      * {@code POST  /admin/users} : Creates a new user.
//      * <p>
//      * Creates a new user if the login and email are not already used, and sends an
//      * mail with an activation link.
//      * The user needs to be activated on creation.
//      *
//      * @param userDTO the user to create.
//      * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
//      *         body the new user, or with status {@code 400 (Bad Request)} if the
//      *         login or email is already in use.
//      * @throws URISyntaxException       if the Location URI syntax is incorrect.
//      * @throws BadRequestAlertException {@code 400 (Bad Request)} if the login or
//      *                                  email is already in use.
//      */
//     @PostMapping("/users")
//     @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//     public ResponseEntity<User> createUser(@Valid @RequestBody UserDTO userDTO) throws URISyntaxException {
//         log.debug("REST request to save User : {}", userDTO);

//         if (userDTO.getId() != null) {
//             throw new BadRequestAlertException("A new user cannot already have an ID", "userManagement", "idexists");
//             // Lowercase the user login before comparing with database
//         } else if (userRepository.findOneByLogin(userDTO.getLogin().toLowerCase()).isPresent()) {
//             throw new LoginAlreadyUsedException();
//         } else if (userRepository.findOneByEmailIgnoreCase(userDTO.getEmail()).isPresent()) {
//             throw new EmailAlreadyUsedException();
//         } else {
//             User newUser = userService.createUser(userDTO);
//             mailService.sendCreationEmail(newUser);
//             return ResponseEntity.created(new URI("/api/admin/users/" + newUser.getLogin()))
//                     .headers(HeaderUtil.createAlert(applicationName, "userManagement.created", newUser.getLogin()))
//                     .body(newUser);
//         }
//     }

//     /**
//      * {@code PUT /admin/users} : Updates an existing User.
//      *
//      * @param userDTO the user to update.
//      * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
//      *         the updated user.
//      * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is
//      *                                   already in use.
//      * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is
//      *                                   already in use.
//      */
//     @PutMapping({ "/users", "/users/{login}" })
//     @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//     public ResponseEntity<UserDTO> updateUser(
//             @PathVariable(name = "login", required = false) @Pattern(regexp = Constants.LOGIN_REGEX) String login,
//             @Valid @RequestBody UserDTO userDTO) {
//         log.debug("REST request to update User : {}", userDTO);
//         Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
//         if (existingUser.isPresent() && (!existingUser.orElseThrow().getId().equals(userDTO.getId()))) {
//             throw new EmailAlreadyUsedException();
//         }
//         existingUser = userRepository.findOneByLogin(userDTO.getLogin().toLowerCase());
//         if (existingUser.isPresent() && (!existingUser.orElseThrow().getId().equals(userDTO.getId()))) {
//             throw new LoginAlreadyUsedException();
//         }
//         Optional<UserDTO> updatedUser = userService.updateUser(userDTO);

//         return ResponseUtil.wrapOrNotFound(
//                 updatedUser,
//                 HeaderUtil.createAlert(applicationName, "userManagement.updated", userDTO.getLogin()));
//     }

//     /**
//      * {@code GET /admin/users} : get all users with all the details - calling this
//      * are only allowed for the administrators.
//      *
//      * @param pageable the pagination information.
//      * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
//      *         all users.
//      */
//     @GetMapping("/users")
//     @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//     public ResponseEntity<PageDTO<UserDTO>> getAllUsers(@ParameterObject Pageable pageable) {
//         log.debug("REST request to get all User for an admin");
//         if (!onlyContainsAllowedProperties(pageable)) {
//             return ResponseEntity.badRequest().build();
//         }

//         return ResponseEntity.ok().cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS)).body(new PageDTO<>(userService.getAllManagedUsers(pageable)));
//     }

//     private boolean onlyContainsAllowedProperties(Pageable pageable) {
//         return pageable.getSort().stream().map(Sort.Order::getProperty).allMatch(ALLOWED_ORDERED_PROPERTIES::contains);
//     }

//     /**
//      * {@code GET /admin/users/:login} : get the "login" user.
//      *
//      * @param login the login of the user to find.
//      * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
//      *         the "login" user, or with status {@code 404 (Not Found)}.
//      */
//     @GetMapping("/users/{login}")
//     @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//     public ResponseEntity<UserDTO> getUser(
//             @PathVariable("login") @Pattern(regexp = Constants.LOGIN_REGEX) String login) {
//         log.debug("REST request to get User : {}", login);
//         return ResponseUtil.wrapOrNotFound(userService.getUserWithAuthoritiesByLogin(login).map(UserDTO::new));
//     }

//     /**
//      * {@code DELETE /admin/users/:login} : delete the "login" User.
//      *
//      * @param login the login of the user to delete.
//      * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
//      */
//     @DeleteMapping("/users/{login}")
//     @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//     public ResponseEntity<Void> deleteUser(
//             @PathVariable("login") @Pattern(regexp = Constants.LOGIN_REGEX) String login) {
//         log.debug("REST request to delete User: {}", login);
//         userService.deleteUser(login);
//         return ResponseEntity.noContent()
//                 .headers(HeaderUtil.createAlert(applicationName, "userManagement.deleted", login)).build();
//     }

//     /**
//      * {@code Put /admin/users/certification} : requestion
//      * certification for a user account.
//      *
//      * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the account just certified.
//      */
//     @PutMapping("/users/certification")
//     @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//     public ResponseEntity<UserDTO> certification(
//             @Valid @RequestBody AccountCertificationRequestDTO requestDTO) {
//         log.debug("REST request to certify User : {}", requestDTO);
//         return ResponseUtil.wrapOrNotFound(userService.certificate(requestDTO));
//     }

//     /**
//      * {@code Put /admin/users/certification} : requestion
//      * certification for a user account.
//      *
//      * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the account just certified.
//      */
//     @PutMapping("/users/certification-withdrawal/{login}")
//     @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//     public ResponseEntity<UserDTO> certificationWithdrawal(
//             @PathVariable("login") String login) {
//         log.debug("REST request to withdraw certification User : {}", login);
//         return ResponseUtil.wrapOrNotFound(userService.certificationWithdrawal(login));
//     }

//     /**
//      * {@code Put /admin/users/certification/:login} : requestion
//      * certification for a user account.
//      *
//      * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body user certification details.
//      */
//     @GetMapping("/users/certification/{login}")
//     @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
//     public ResponseEntity<AccountCertificationRequestDTO> getAccountCertification(
//             @PathVariable("login") String login) {
//         log.debug("REST request to get account certification of User : {}", login);
//         return ResponseUtil.wrapOrNotFound(userService.getAccountCertification(login));
//     }

// }
