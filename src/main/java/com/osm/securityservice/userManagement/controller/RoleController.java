package com.osm.securityservice.userManagement.controller;


import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.RoleDTO;
import com.osm.securityservice.userManagement.models.Role;
import com.osm.securityservice.userManagement.service.UserService;
import com.xdev.xdevbase.controllers.impl.BaseControllerImpl;
import com.xdev.xdevbase.services.BaseService;
import com.xdev.xdevbase.utils.OSMLogger;
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
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "getAllRolesWithUserCount", "Retrieving all roles with user count");
        
        try {
            List<RoleDTO> roles = baseService.findAll();
            
            if (roles != null && !roles.isEmpty()) {
                OSMLogger.log(this.getClass(), OSMLogger.LogLevel.DEBUG, "Found {} roles, calculating user counts", roles.size());
                
                List<RoleDTO> roleDTOs = roles.stream()
                        .filter(Objects::nonNull)
                        .peek(r -> {
                            List<OSMUserDTO> users = userService.findByRoleName(r.getRoleName());
                            if (users != null && !users.isEmpty()) {
                                r.setUsersCount(users.size());
                                OSMLogger.log(this.getClass(), OSMLogger.LogLevel.DEBUG, 
                                    "Role {} has {} users", r.getRoleName(), users.size());
                            } else {
                                r.setUsersCount(0);
                                OSMLogger.log(this.getClass(), OSMLogger.LogLevel.DEBUG, 
                                    "Role {} has 0 users", r.getRoleName());
                            }
                        })
                        .toList();
                
                OSMLogger.logMethodExit(this.getClass(), "getAllRolesWithUserCount", 
                    "Retrieved " + roleDTOs.size() + " roles with user counts");
                OSMLogger.logPerformance(this.getClass(), "getAllRolesWithUserCount", startTime, System.currentTimeMillis());
                OSMLogger.logSecurityEvent(this.getClass(), "ROLES_RETRIEVED_WITH_COUNTS", 
                    "All roles retrieved with user counts successfully");
                
                return ResponseEntity.ok(roleDTOs);
            }
            
            OSMLogger.logMethodExit(this.getClass(), "getAllRolesWithUserCount", "No roles found");
            OSMLogger.logPerformance(this.getClass(), "getAllRolesWithUserCount", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "NO_ROLES_FOUND", 
                "No roles found in the system");
            
            return ResponseEntity.ok(roles);
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), 
                "Unexpected error while retrieving roles with user counts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @Override
    protected String getResourceName() {
        return "Role".toUpperCase();
    }
}
