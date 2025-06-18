package com.osm.securityservice.userManagement.service;

import com.osm.securityservice.userManagement.data.PermissionRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.PermissionDTO;
import com.osm.securityservice.userManagement.models.Permission;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class PermissionService extends BaseServiceImpl<Permission, PermissionDTO, PermissionDTO> {
    private final PermissionRepository permissionRepository;

    protected PermissionService(BaseRepository<Permission> repository, ModelMapper modelMapper, PermissionRepository permissionRepository) {
        super(repository, modelMapper);
        this.permissionRepository = permissionRepository;
    }
}
