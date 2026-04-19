package com.osm.securityservice.userManagement.controller;

import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserOUTDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.UpdatePasswordDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.VerifyOtpAndSetPasswordRequest;
import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.service.UserService;
import com.xdev.xdevbase.controllers.impl.BaseControllerImpl;
import com.xdev.xdevbase.services.BaseService;
import com.xdev.xdevbase.utils.OSMLogger;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.CredentialExpiredException;
import java.util.UUID;

@RestController
@RequestMapping("/api/security/user")
public class UserController extends BaseControllerImpl<OSMUser, OSMUserDTO, OSMUserOUTDTO> {
    private final UserService userService;

    public UserController(BaseService<OSMUser, OSMUserDTO, OSMUserOUTDTO> baseService, ModelMapper modelMapper, UserService userService) {
        super(baseService, modelMapper);
        this.userService = userService;
    }

    @PostMapping("/auth/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestParam String identifier) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "resetPassword", "Password reset request for identifier: " + identifier);
        
        try {
            OSMUserOUTDTO user = userService.resetPassword(identifier);
            
            OSMLogger.logMethodExit(this.getClass(), "resetPassword", "Password reset successful for identifier: " + identifier);
            OSMLogger.logPerformance(this.getClass(), "resetPassword", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_RESET_REQUESTED", 
                "Password reset requested successfully for identifier: " + identifier);
            
            return ResponseEntity.ok(user);
            
        } catch (AccountLockedException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_RESET_ACCOUNT_LOCKED", 
                "Password reset failed - Account locked for identifier: " + identifier);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is locked");
            
        } catch (IllegalArgumentException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_RESET_INVALID_INPUT", 
                "Password reset failed - Invalid input for identifier: " + identifier + ", Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input");
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), 
                "Unexpected error during password reset for identifier: " + identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/auth/validateResetCode/{userId}")
    public ResponseEntity<?> validateResetCode(@RequestParam String code, @PathVariable UUID userId) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "validateResetCode",
                "Validating reset code for user: " + userId);

        try {
            userService.validateResetCode(code, userId);

            OSMLogger.logMethodExit(this.getClass(), "validateResetCode",
                    "Reset code validated successfully for user: " + userId);
            OSMLogger.logPerformance(this.getClass(), "validateResetCode", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "RESET_CODE_VALIDATED",
                    "Reset code validated successfully for user: " + userId);

            return ResponseEntity.ok().build();

        } catch (CredentialExpiredException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "RESET_CODE_EXPIRED",
                    "Reset code validation failed - Code expired for user: " + userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Code is expired");

        } catch (IllegalArgumentException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "RESET_CODE_INVALID",
                    "Reset code validation failed - Invalid input for user: " + userId + ", Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input");

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                    "Unexpected error during reset code validation for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/auth/updatePassword/{userId}")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordDTO dto, @PathVariable UUID userId) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "updatePassword",
                "Updating password for user: " + userId);

        try {
            userService.updatePassword(dto, userId);

            OSMLogger.logMethodExit(this.getClass(), "updatePassword",
                    "Password updated successfully for user: " + userId);
            OSMLogger.logPerformance(this.getClass(), "updatePassword", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_UPDATED",
                    "Password updated successfully for user: " + userId);

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "PASSWORD_UPDATE_INVALID",
                    "Password update failed - Invalid input for user: " + userId + ", Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                    "Unexpected error during password update for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/addUser")
    public ResponseEntity<?> addUser(@RequestBody OSMUserOUTDTO dto) {
        long startTime = System.currentTimeMillis();
        String username = dto != null ? dto.getUsername() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "addUser", "Adding new user: " + username);

        try {
            OSMUserOUTDTO user = userService.addUser(dto);

            OSMLogger.logMethodExit(this.getClass(), "addUser",
                    "User added successfully: " + username);
            OSMLogger.logPerformance(this.getClass(), "addUser", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "USER_ADDED",
                    "New user added successfully: " + username);

            return ResponseEntity.ok(user);

        } catch (IllegalArgumentException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_ADD_INVALID",
                    "User addition failed - Invalid input for username: " + username + ", Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                    "Unexpected error during user addition for username: " + username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/updateUser/{id}")
    public ResponseEntity<?> updateUser(@RequestBody OSMUserOUTDTO dto, @PathVariable UUID id) {
        long startTime = System.currentTimeMillis();
        String username = dto != null ? dto.getUsername() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "updateUser",
                "Updating user: " + username + " with ID: " + id);

        try {
            OSMUserOUTDTO user = userService.updateUser(dto, id);

            OSMLogger.logMethodExit(this.getClass(), "updateUser",
                    "User updated successfully: " + username + " with ID: " + id);
            OSMLogger.logPerformance(this.getClass(), "updateUser", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "USER_UPDATED",
                    "User updated successfully: " + username + " with ID: " + id);

            return ResponseEntity.ok(user);

        } catch (IllegalArgumentException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_UPDATE_INVALID",
                    "User update failed - Invalid input for user: " + username + " with ID: " + id + ", Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                    "Unexpected error during user update for username: " + username + " with ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/auth/register-by-email-otp")
    public ResponseEntity<?> registerByEmailOtp(@RequestBody OSMUserOUTDTO dto) {
        long startTime = System.currentTimeMillis();
        String username = dto != null ? dto.getUsername() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "registerByEmailOtp",
                "Registering user with email OTP: " + username);

        try {
            userService.createUserPendingConfirmation(dto);

            OSMLogger.logMethodExit(this.getClass(), "registerByEmailOtp",
                    "Pending user created successfully: " + username);
            OSMLogger.logPerformance(this.getClass(), "registerByEmailOtp", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "USER_PENDING_CONFIRMATION_CREATED",
                    "Pending user created successfully: " + username);

            return ResponseEntity.ok("Compte créé. Un code OTP et un lien d’activation ont été envoyés par email.");

        } catch (IllegalArgumentException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "USER_PENDING_CONFIRMATION_INVALID",
                    "Pending user creation failed for username: " + username + ", Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                    "Unexpected error during pending user creation for username: " + username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création du compte : " + e.getMessage());
        }
    }
    @PostMapping("/auth/verify-otp-and-set-password")
    public ResponseEntity<?> verifyOtpAndSetPassword(@RequestBody VerifyOtpAndSetPasswordRequest request) {
        long startTime = System.currentTimeMillis();
        String email = request != null ? request.getEmail() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "verifyOtpAndSetPassword",
                "Activating account for email: " + email);

        try {
            userService.verifyOtpAndSetPassword(request);

            OSMLogger.logMethodExit(this.getClass(), "verifyOtpAndSetPassword",
                    "Account activated successfully for email: " + email);
            OSMLogger.logPerformance(this.getClass(), "verifyOtpAndSetPassword", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "ACCOUNT_ACTIVATED",
                    "Account activated successfully for email: " + email);

            return ResponseEntity.ok("Compte activé avec succès");

        } catch (CredentialExpiredException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "ACCOUNT_ACTIVATION_OTP_EXPIRED",
                    "Activation failed - OTP expired for email: " + email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Le code OTP a expiré");

        } catch (IllegalArgumentException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "ACCOUNT_ACTIVATION_INVALID",
                    "Activation failed for email: " + email + ", Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                    "Unexpected error during account activation for email: " + email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l’activation du compte");
        }
    }
    @PostMapping("/auth/resend-activation-otp")
    public ResponseEntity<?> resendActivationOtp(@RequestParam String email) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "resendActivationOtp",
                "Resending activation OTP for email: " + email);

        try {
            userService.resendActivationOtp(email);

            OSMLogger.logMethodExit(this.getClass(), "resendActivationOtp",
                    "Activation OTP resent successfully for email: " + email);
            OSMLogger.logPerformance(this.getClass(), "resendActivationOtp", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "ACCOUNT_ACTIVATION_OTP_RESENT",
                    "Activation OTP resent successfully for email: " + email);

            return ResponseEntity.ok("Un nouveau code OTP a été envoyé");

        } catch (IllegalArgumentException e) {
            OSMLogger.logSecurityEvent(this.getClass(), "ACCOUNT_ACTIVATION_OTP_RESEND_INVALID",
                    "Resend activation OTP failed for email: " + email + ", Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(),
                    "Unexpected error while resending activation OTP for email: " + email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du renvoi du code OTP");
        }
    }
    //oublié mot de passe
    @PostMapping("/auth/resetPassword/confirm")
    public ResponseEntity<?> resetPasswordConfirm(
            @RequestBody VerifyOtpAndSetPasswordRequest request) {

        try {
            userService.resetPasswordConfirm(request);
            return ResponseEntity.ok("Mot de passe modifié avec succès");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Override
    protected String getResourceName() {
        return "USER";
    }

    @Override
    public ResponseEntity<?> resolve(String publicCode) {
        return null;
    }
}
