package com.osm.securityservice.userManagement.service;


import com.osm.securityservice.userManagement.data.PermissionRepository;
import com.osm.securityservice.userManagement.data.RoleRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.RoleDTO;
import com.osm.securityservice.userManagement.models.Permission;
import com.osm.securityservice.userManagement.models.Role;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService extends BaseServiceImpl<Role, RoleDTO, RoleDTO> {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    protected RoleService(BaseRepository<Role> repository, ModelMapper modelMapper, RoleRepository roleRepository, PermissionRepository permissionRepository) {
        super(repository, modelMapper);
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void resolveEntityRelations(Role entity) {
        if (entity.getPermissions() != null && !entity.getPermissions().isEmpty()) {
            Set<Permission> resolved = entity.getPermissions().stream()
                    .map(p -> permissionRepository.findById(p.getId()).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            entity.getPermissions().clear();
            entity.setPermissions(resolved);
        }
    }
}
