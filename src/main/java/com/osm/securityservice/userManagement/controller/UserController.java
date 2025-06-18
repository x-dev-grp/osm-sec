package com.osm.securityservice.userManagement.controller;

import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserOUTDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.UpdatePasswordDTO;
import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.service.UserService;
import com.xdev.xdevbase.controllers.impl.BaseControllerImpl;
import com.xdev.xdevbase.models.OSMModule;
import com.xdev.xdevbase.services.BaseService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.CredentialExpiredException;
import java.util.UUID;

import static com.xdev.xdevbase.models.OSMModule.HABILITATION;

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
        try {
            OSMUserOUTDTO user = userService.resetPassword(identifier);
            return ResponseEntity.ok(user);
        } catch (AccountLockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is locked");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/auth/validateResetCode/{userId}")
    public ResponseEntity<?> validateResetCode(@RequestParam String code, @PathVariable UUID userId) {
        try {
            userService.validateResetCode(code, userId);
            return ResponseEntity.ok().build();
        } catch (CredentialExpiredException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Code is expired");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/auth/updatePassword/{userId}")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordDTO dto, @PathVariable UUID userId) {
        try {
            userService.updatePassword(dto, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/addUser")
    public ResponseEntity<?> addUser(@RequestBody OSMUserOUTDTO dto) {
        try {
            OSMUserOUTDTO user = userService.addUser(dto);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @PostMapping("/updateUser/{id}")
    public ResponseEntity<?> updateUser(@RequestBody OSMUserOUTDTO dto, @PathVariable UUID id) {
        try {
            OSMUserOUTDTO user = userService.updateUser(dto, id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @Override
    protected String getResourceName() {
        return "USER".toUpperCase();
    }
}
