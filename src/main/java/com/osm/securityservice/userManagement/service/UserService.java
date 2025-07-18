package com.osm.securityservice.userManagement.service;

import com.osm.securityservice.userManagement.data.UserRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.ConfirmationCodeDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserOUTDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.UpdatePasswordDTO;
import com.osm.securityservice.userManagement.models.ConfirmationCode;
import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.models.enums.ConfirmationCodeType;
import com.osm.securityservice.userManagement.models.enums.ConfirmationMethod;
import com.xdev.mailSender.models.MailRequest;
import com.xdev.mailSender.services.MailService;
import com.xdev.xdevbase.config.TenantContext;
import com.xdev.xdevbase.models.Action;
import com.xdev.xdevbase.models.Action;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import com.xdev.xdevbase.utils.OSMLogger;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.CredentialExpiredException;
import java.security.SecureRandom;
import java.util.*;

@Service
public class UserService extends BaseServiceImpl<OSMUser, OSMUserDTO, OSMUserOUTDTO> implements UserDetailsService {
    private final UserRepository userRepository;
    private final MailService mailService;
    private final ConfirmationCodeService confirmationCodeService;
    private final PasswordEncoder passwordEncoder;

    protected UserService(BaseRepository<OSMUser> repository, ModelMapper modelMapper, UserRepository userRepository, MailService mailService, ConfirmationCodeService confirmationCodeService, PasswordEncoder passwordEncoder) {
        super(repository, modelMapper);

        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "UserService", "Initializing UserService");

        try {
            this.userRepository = userRepository;
            this.mailService = mailService;
            this.confirmationCodeService = confirmationCodeService;
            this.passwordEncoder = passwordEncoder;

            OSMLogger.logMethodExit(this.getClass(), "UserService", "UserService initialized successfully");
            OSMLogger.logPerformance(this.getClass(), "UserService", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "USER_SERVICE_INITIALIZED",
                "User service initialized successfully");

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error initializing UserService", e);
            throw e;
        }
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "loadUserByUsername", "Loading user details for username: " + username);

        try {
            UserDetails userDetails = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

            OSMLogger.logMethodExit(this.getClass(), "loadUserByUsername", "User details loaded successfully for: " + username);
            OSMLogger.logPerformance(this.getClass(), "loadUserByUsername", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "USER_DETAILS_LOADED",
                "User details loaded successfully for username: " + username);

            return userDetails;

        } catch (UsernameNotFoundException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_NOT_FOUND",
                "User not found during authentication: " + username);
            throw e;
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Unexpected error loading user details for username: " + username, e);
            throw e;
        }
    }

    @Transactional
    public OSMUserOUTDTO addUser(OSMUserOUTDTO userDTO) throws Exception {
        long startTime = System.currentTimeMillis();
        String username = userDTO != null ? userDTO.getUsername() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "addUser", "Adding new user: " + username);

        try {
            validateUserDTO(userDTO);
            checkExistUser(userDTO.getUsername(), userDTO.getEmail(), userDTO.getPhoneNumber());

            String rawPassword = generateSecureCode(8);
            String hashedPassword = passwordEncoder.encode(rawPassword);

            OSMUser user = modelMapper.map(userDTO, OSMUser.class);
            user.setPassword(hashedPassword);
            user.setNewUser(true);

            sendConfirmation(userDTO, rawPassword);
            OSMUser savedUser = userRepository.save(user);

            OSMLogger.logMethodExit(this.getClass(), "addUser", "User added successfully: " + username);
            OSMLogger.logPerformance(this.getClass(), "addUser", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "USER_ADDED",
                "New user added successfully: " + username);

            return modelMapper.map(savedUser, OSMUserOUTDTO.class);

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error adding user: " + username, e);
            throw e;
        }
    }

    @Transactional
    public OSMUserOUTDTO updateUser(OSMUserOUTDTO userDTO, UUID id) throws Exception {
        long startTime = System.currentTimeMillis();
        String username = userDTO != null ? userDTO.getUsername() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "updateUser", "Updating user: " + username + " with ID: " + id);

        try {
            if (id == null) throw new IllegalArgumentException("User ID must not be null");

            validateUserDTO(userDTO);

            OSMUser user = repository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException(id.toString()));

            checkUserToUpdate(user, userDTO.getUsername(), userDTO.getEmail(), userDTO.getPhoneNumber());

            boolean usernameChanged = !Objects.equals(userDTO.getUsername(), user.getUsername());
            boolean emailChanged = userDTO.getEmail() != null && !Objects.equals(userDTO.getEmail(), user.getEmail());
            boolean phoneChanged = userDTO.getPhoneNumber() != null && !Objects.equals(userDTO.getPhoneNumber(), user.getPhoneNumber());

            user.setLocked(userDTO.isLocked());
            user.setFirstName(userDTO.getFirstName());
            user.setLastName(userDTO.getLastName());
            user.setUsername(userDTO.getUsername());
            user.setConfirmationMethod(userDTO.getConfirmationMethod());

            if (userDTO.getEmail() != null) {
                user.setEmail(userDTO.getEmail());
            }

            if (userDTO.getPhoneNumber() != null) {
                user.setPhoneNumber(userDTO.getPhoneNumber());
            }

            userRepository.save(user);

            if (usernameChanged || emailChanged || phoneChanged) {
                String rawPassword = generateSecureCode(8);
                sendConfirmation(userDTO, rawPassword);
                OSMLogger.logSecurityEvent(this.getClass(), "USER_CREDENTIALS_UPDATED",
                    "User credentials updated and new password sent: " + username);
            }

            OSMLogger.logMethodExit(this.getClass(), "updateUser", "User updated successfully: " + username + " with ID: " + id);
            OSMLogger.logPerformance(this.getClass(), "updateUser", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "USER_UPDATED",
                "User updated successfully: " + username + " with ID: " + id);

            return modelMapper.map(user, OSMUserOUTDTO.class);

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error updating user: " + username + " with ID: " + id, e);
            throw e;
        }
    }

    private void checkExistUser(String username, String email, String phoneNumber) {
        if (username != null && userRepository.findByUsername(username).isPresent()) {
            OSMLogger.logSecurityEvent(this.getClass(), "USERNAME_ALREADY_EXISTS",
                "Username already exists: " + username);
            throw new IllegalArgumentException("Username is already in use");
        }
        if (email != null && userRepository.findByEmailIgnoreCase(email).isPresent()) {
            OSMLogger.logSecurityEvent(this.getClass(), "EMAIL_ALREADY_EXISTS",
                "Email already exists: " + email);
            throw new IllegalArgumentException("Email is already in use");
        }
        if (phoneNumber != null && userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            OSMLogger.logSecurityEvent(this.getClass(), "PHONE_ALREADY_EXISTS",
                "Phone number already exists: " + phoneNumber);
            throw new IllegalArgumentException("Phone number is already in use");
        }
    }

    private void checkUserToUpdate(OSMUser user, String username, String email, String phoneNumber) {
        if (((user.getUsername() != null && username != null && !user.getUsername().equals(username)) || (user.getUsername() == null && username != null)) && userRepository.findByUsername(username).isPresent()) {
            OSMLogger.logSecurityEvent(this.getClass(), "USERNAME_ALREADY_EXISTS_UPDATE",
                "Username already exists during update: " + username);
            throw new IllegalArgumentException("Username is already in use");
        }
        if (((user.getEmail() != null && email != null && !user.getEmail().equals(email)) || (user.getEmail() == null && email != null)) && userRepository.findByEmailIgnoreCase(email).isPresent()) {
            OSMLogger.logSecurityEvent(this.getClass(), "EMAIL_ALREADY_EXISTS_UPDATE",
                "Email already exists during update: " + email);
            throw new IllegalArgumentException("Email is already in use");
        }
        if (((user.getPhoneNumber() != null && phoneNumber != null && !user.getPhoneNumber().equals(phoneNumber)) || (user.getPhoneNumber() == null && phoneNumber != null)) && userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            OSMLogger.logSecurityEvent(this.getClass(), "PHONE_ALREADY_EXISTS_UPDATE",
                "Phone number already exists during update: " + phoneNumber);
            throw new IllegalArgumentException("Phone number is already in use");
        }
    }

    public OSMUser getByUsername(String username) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "getByUsername", "Getting user by username: " + username);

        try {
            OSMUser user = userRepository.findByUsername(username).orElse(null);

            if (user != null) {
                OSMLogger.logMethodExit(this.getClass(), "getByUsername", "User found: " + username);
                OSMLogger.logPerformance(this.getClass(), "getByUsername", startTime, System.currentTimeMillis());
            } else {
                OSMLogger.logMethodExit(this.getClass(), "getByUsername", "User not found: " + username);
                OSMLogger.logPerformance(this.getClass(), "getByUsername", startTime, System.currentTimeMillis());
                OSMLogger.logSecurityEvent(this.getClass(), "USER_NOT_FOUND_BY_USERNAME",
                    "User not found by username: " + username);
            }

            return user;

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error getting user by username: " + username, e);
            throw e;
        }
    }

    private void sendConfirmation(OSMUserOUTDTO userDTO, String rawPassword) throws Exception {
        long startTime = System.currentTimeMillis();
        String username = userDTO != null ? userDTO.getUsername() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "sendConfirmation",
            "Sending confirmation for user: " + username + ", Method: " + userDTO.getConfirmationMethod());

        try {
            switch (userDTO.getConfirmationMethod()) {
                case EMAIL -> {
                    MailRequest mailRequest = new MailRequest();
                    mailRequest.setTo(userDTO.getEmail());
                    mailRequest.setSubject("Compte OSM");
                    mailRequest.setBody(String.format("Username: %s\nPassword: %s", userDTO.getUsername(), rawPassword));
                    mailService.sendEmail(mailRequest);

                    OSMLogger.logSecurityEvent(this.getClass(), "CONFIRMATION_EMAIL_SENT",
                        "Confirmation email sent to: " + userDTO.getEmail());
                }
                case PHONE -> {
                    //TODO send sms message
                    OSMLogger.logSecurityEvent(this.getClass(), "CONFIRMATION_SMS_PENDING",
                        "SMS confirmation pending for phone: " + userDTO.getPhoneNumber());
                }
                default -> {
                    OSMLogger.logSecurityEvent(this.getClass(), "UNSUPPORTED_CONFIRMATION_METHOD",
                        "Unsupported confirmation method: " + userDTO.getConfirmationMethod());
                    throw new IllegalArgumentException("Unsupported confirmation method");
                }
            }

            OSMLogger.logMethodExit(this.getClass(), "sendConfirmation",
                "Confirmation sent successfully for user: " + username);
            OSMLogger.logPerformance(this.getClass(), "sendConfirmation", startTime, System.currentTimeMillis());

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error sending confirmation for user: " + username, e);
            throw e;
        }
    }

    public OSMUserOUTDTO resetPassword(String identifier) throws Exception {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "resetPassword", "Password reset request for identifier: " + identifier);

        try {
            OSMUser user = userRepository.findByPhoneOrEmailIgnoreCase(identifier).orElse(null);
            if (user != null) {
                if (user.isLocked()) {
                    OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_RESET_ACCOUNT_LOCKED",
                        "Password reset failed - Account locked for identifier: " + identifier);
                    throw new AccountLockedException("Invalid input");
                }

                if (user.getEmail().toLowerCase().equals(identifier)) {
                    String code = generateRandomCode();
                    ConfirmationCode confirmationCode = new ConfirmationCode();
                    confirmationCode.setCode(code);
                    confirmationCode.setUser(user);
                    confirmationCode.setConfirmationCodeType(ConfirmationCodeType.RESETPASSWORD);
                    saveConfirmationCode(confirmationCode);

                    MailRequest mailrequest = new MailRequest();
                    mailrequest.setTo(user.getEmail());
                    mailrequest.setSubject("Password reset");
                    mailrequest.setBody("Code: " + code);
                    mailService.sendEmail(mailrequest);

                    OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_RESET_CODE_SENT",
                        "Password reset code sent to email: " + user.getEmail());
                } else {
                    //TODO send to phone number
                    OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_RESET_SMS_PENDING",
                        "Password reset SMS pending for phone: " + identifier);
                }

                OSMLogger.logMethodExit(this.getClass(), "resetPassword",
                    "Password reset initiated successfully for identifier: " + identifier);
                OSMLogger.logPerformance(this.getClass(), "resetPassword", startTime, System.currentTimeMillis());
                OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_RESET_INITIATED",
                    "Password reset initiated successfully for identifier: " + identifier);

                return modelMapper.map(user, OSMUserOUTDTO.class);

            } else {
                OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_RESET_INVALID_IDENTIFIER",
                    "Password reset failed - Invalid identifier: " + identifier);
                throw new IllegalArgumentException("Invalid input");
            }

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error during password reset for identifier: " + identifier, e);
            throw e;
        }
    }

    private ConfirmationCode saveConfirmationCode(ConfirmationCode code) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "saveConfirmationCode",
            "Saving confirmation code for user: " + (code.getUser() != null ? code.getUser().getUsername() : "null"));

        try {
            ConfirmationCode existedCode = confirmationCodeService.getByConfirmationCodeTypeAndUser(ConfirmationCodeType.RESETPASSWORD, code.getUser());
            if (existedCode != null) {
                existedCode.setCode(code.getCode());
                ConfirmationCodeDTO codeDTO = confirmationCodeService.save(modelMapper.map(existedCode, ConfirmationCodeDTO.class));

                OSMLogger.logMethodExit(this.getClass(), "saveConfirmationCode", "Existing confirmation code updated");
                OSMLogger.logPerformance(this.getClass(), "saveConfirmationCode", startTime, System.currentTimeMillis());

                return modelMapper.map(codeDTO, ConfirmationCode.class);
            }

            ConfirmationCodeDTO codeDTO = confirmationCodeService.save(modelMapper.map(code, ConfirmationCodeDTO.class));

            OSMLogger.logMethodExit(this.getClass(), "saveConfirmationCode", "New confirmation code saved");
            OSMLogger.logPerformance(this.getClass(), "saveConfirmationCode", startTime, System.currentTimeMillis());

            return modelMapper.map(codeDTO, ConfirmationCode.class);

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error saving confirmation code for user: " + (code.getUser() != null ? code.getUser().getUsername() : "null"), e);
            throw e;
        }
    }


    public boolean validateResetCode(String code, UUID userId) throws Exception {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "validateResetCode",
            "Validating reset code for user: " + userId + ", Code: " + (code != null ? code.substring(0, Math.min(3, code.length())) + "..." : "null"));

        try {
            OSMUser user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                OSMLogger.logSecurityEvent(this.getClass(), "RESET_CODE_VALIDATION_USER_NOT_FOUND",
                    "Reset code validation failed - User not found: " + userId);
                throw new IllegalArgumentException("Invalid code");
            }

            ConfirmationCode existedCode = confirmationCodeService.getByConfirmationCodeTypeAndUser(ConfirmationCodeType.RESETPASSWORD, user);
            if (existedCode == null || !existedCode.getCode().equals(code)) {
                OSMLogger.logSecurityEvent(this.getClass(), "RESET_CODE_VALIDATION_INVALID",
                    "Reset code validation failed - Invalid code for user: " + userId);
                throw new IllegalArgumentException("Invalid code");
            }
            if (existedCode.isExpired()) {
                OSMLogger.logSecurityEvent(this.getClass(), "RESET_CODE_VALIDATION_EXPIRED",
                    "Reset code validation failed - Code expired for user: " + userId);
                throw new CredentialExpiredException("Expired code");
            }

            OSMLogger.logMethodExit(this.getClass(), "validateResetCode",
                "Reset code validated successfully for user: " + userId);
            OSMLogger.logPerformance(this.getClass(), "validateResetCode", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "RESET_CODE_VALIDATED",
                "Reset code validated successfully for user: " + userId);

            return true;

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error validating reset code for user: " + userId, e);
            throw e;
        }
    }

    public void updatePassword(UpdatePasswordDTO dto, UUID userId) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "updatePassword", "Updating password for user: " + userId);

        try {
            OSMUser user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_UPDATE_USER_NOT_FOUND",
                    "Password update failed - User not found: " + userId);
                throw new IllegalArgumentException("Invalid user");
            }

            if (!dto.getNewPassword().equals(dto.getOldPassword())) {
                if (!dto.getNewPassword().equals(dto.getNewPasswordConfirmation())) {
                    OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_UPDATE_CONFIRMATION_MISMATCH",
                        "Password update failed - Confirmation mismatch for user: " + userId);
                    throw new IllegalArgumentException("Invalid password");
                }

                String hashedPassword = passwordEncoder.encode(dto.getNewPassword());
                user.setPassword(hashedPassword);
                user.setNewUser(false);
                userRepository.save(user);

                OSMLogger.logMethodExit(this.getClass(), "updatePassword",
                    "Password updated successfully for user: " + userId);
                OSMLogger.logPerformance(this.getClass(), "updatePassword", startTime, System.currentTimeMillis());
                OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_UPDATED",
                    "Password updated successfully for user: " + userId);
            } else {
                OSMLogger.logMethodExit(this.getClass(), "updatePassword",
                    "Password update skipped - Same password for user: " + userId);
                OSMLogger.logPerformance(this.getClass(), "updatePassword", startTime, System.currentTimeMillis());
            }

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error updating password for user: " + userId, e);
            throw e;
        }
    }

    private void validateUserDTO(OSMUserOUTDTO userDTO) {
        if (userDTO == null) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_VALIDATION_NULL_DTO",
                "User validation failed - DTO is null");
            throw new IllegalArgumentException("User data must not be null");
        }
        if (userDTO.getUsername() == null || userDTO.getUsername().isBlank()) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_VALIDATION_MISSING_USERNAME",
                "User validation failed - Username is missing");
            throw new IllegalArgumentException("Username is required");
        }
        if (userDTO.getConfirmationMethod() == null) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_VALIDATION_MISSING_CONFIRMATION_METHOD",
                "User validation failed - Confirmation method is missing");
            throw new IllegalArgumentException("Confirmation method is required");
        }
        if (userDTO.getConfirmationMethod() == ConfirmationMethod.EMAIL && (userDTO.getEmail() == null || userDTO.getEmail().isBlank())) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_VALIDATION_MISSING_EMAIL",
                "User validation failed - Email is missing for email confirmation");
            throw new IllegalArgumentException("Email is required for email confirmation");
        }
        if (userDTO.getConfirmationMethod() == ConfirmationMethod.PHONE && (userDTO.getPhoneNumber() == null || userDTO.getPhoneNumber().isBlank())) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_VALIDATION_MISSING_PHONE",
                "User validation failed - Phone number is missing for phone confirmation");
            throw new IllegalArgumentException("Phone number is required for phone confirmation");
        }
    }

    public String generateSecureCode(int length) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "generateSecureCode", "Generating secure code with length: " + length);

        try {
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            SecureRandom random = new SecureRandom();
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String code = sb.toString();

            OSMLogger.logMethodExit(this.getClass(), "generateSecureCode", "Secure code generated successfully");
            OSMLogger.logPerformance(this.getClass(), "generateSecureCode", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "SECURE_CODE_GENERATED",
                "Secure code generated with length: " + length);

            return code;

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error generating secure code with length: " + length, e);
            throw e;
        }
    }

    public String generateRandomCode() {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "generateRandomCode", "Generating random 6-digit code");

        try {
            int code = (int) (Math.random() * 900_000) + 100_000; // range: 100000–999999
            String codeString = String.valueOf(code);

            OSMLogger.logMethodExit(this.getClass(), "generateRandomCode", "Random code generated successfully");
            OSMLogger.logPerformance(this.getClass(), "generateRandomCode", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "RANDOM_CODE_GENERATED",
                "Random 6-digit code generated");

            return codeString;

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error generating random code", e);
            throw e;
        }
    }

    public List<OSMUserDTO> findByRoleName(String roleName) {

        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "findByRoleName", "Finding users by role name: " + roleName);

        try {
            List<OSMUserDTO> users = userRepository.findByRoleRoleNameAndTenantId(roleName).stream().map(
                    user -> modelMapper.map(user, OSMUserDTO.class)
            ).toList();

            OSMLogger.logMethodExit(this.getClass(), "findByRoleName",
                "Found " + users.size() + " users with role: " + roleName);
            OSMLogger.logPerformance(this.getClass(), "findByRoleName", startTime, System.currentTimeMillis());
            OSMLogger.logDataAccess(this.getClass(), "READ_BY_ROLE", "OSMUser");

            return users;

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                "Error finding users by role name: " + roleName, e);
            throw e;
        }
    }
    @Override
    public Set<Action> actionsMapping(OSMUser user) {
        Set<Action> actions = new HashSet<>();
        actions.add(Action.READ);
        if (user.getRole().getRoleName().equals("ADMIN")) {
            actions.addAll(Set.of(Action.UPDATE, Action.CREATE,Action.DELETE));
        }
        return actions;
    }

}
