package com.osm.securityservice.userManagement.controller;


import com.osm.securityservice.userManagement.dtos.OUTDTO.PermissionDTO;
import com.osm.securityservice.userManagement.models.Permission;
import com.xdev.xdevbase.controllers.impl.BaseControllerImpl;
import com.xdev.xdevbase.services.BaseService;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security/permission")
public class PermissionController extends BaseControllerImpl<Permission, PermissionDTO, PermissionDTO> {
    public PermissionController(BaseService<Permission, PermissionDTO, PermissionDTO> baseService, ModelMapper modelMapper) {
        super(baseService, modelMapper);
    }

    @Override
    protected String getResourceName() {
        return "Permission".toUpperCase();
    }
}
