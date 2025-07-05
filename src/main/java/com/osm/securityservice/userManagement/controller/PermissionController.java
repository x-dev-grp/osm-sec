package com.osm.securityservice.userManagement.controller;


import com.osm.securityservice.userManagement.dtos.OUTDTO.PermissionDTO;
import com.osm.securityservice.userManagement.models.Permission;
import com.xdev.xdevbase.controllers.impl.BaseControllerImpl;
import com.xdev.xdevbase.services.BaseService;
import com.xdev.xdevbase.utils.OSMLogger;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security/permission")
public class PermissionController extends BaseControllerImpl<Permission, PermissionDTO, PermissionDTO> {
    public PermissionController(BaseService<Permission, PermissionDTO, PermissionDTO> baseService, ModelMapper modelMapper) {
        super(baseService, modelMapper);
        
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "PermissionController", "Initializing PermissionController");
        
        try {
            OSMLogger.logMethodExit(this.getClass(), "PermissionController", "PermissionController initialized successfully");
            OSMLogger.logPerformance(this.getClass(), "PermissionController", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "PERMISSION_CONTROLLER_INITIALIZED", 
                "Permission controller initialized successfully");
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error initializing PermissionController", e);
            throw e;
        }
    }

    @Override
    protected String getResourceName() {
        return "Permission".toUpperCase();
    }
}
