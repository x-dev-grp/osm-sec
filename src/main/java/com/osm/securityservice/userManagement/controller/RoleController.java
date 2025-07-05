package com.osm.securityservice.userManagement.controller;


import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.RoleDTO;
import com.osm.securityservice.userManagement.models.Role;
import com.osm.securityservice.userManagement.service.UserService;
import com.xdev.xdevbase.controllers.impl.BaseControllerImpl;
import com.xdev.xdevbase.services.BaseService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/security/role")
public class RoleController extends BaseControllerImpl<Role, RoleDTO, RoleDTO> {
    private final UserService userService;

    public RoleController(BaseService<Role, RoleDTO, RoleDTO> baseService, ModelMapper modelMapper, UserService userService) {
        super(baseService, modelMapper);
        this.userService = userService;
    }

    @GetMapping("/all-with-user-count")
    public ResponseEntity<?> getAllRolesWithUserCount() {
        try {
            List<RoleDTO> roles = baseService.findAll();
            if (roles != null && !roles.isEmpty()) {
                List<RoleDTO> roleDTOs = roles.stream()
                        .filter(Objects::nonNull)
                        .peek(r -> {
                            List<OSMUserDTO> users = userService.findByRoleName(r.getRoleName());
                            if (users != null && !users.isEmpty()) {
                                r.setUsersCount(users.size());
                            }
                        })
                        .toList();
                return ResponseEntity.ok(roleDTOs);
            }
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @Override
    protected String getResourceName() {
        return "Role".toUpperCase();
    }
}
