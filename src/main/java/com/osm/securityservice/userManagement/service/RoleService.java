package com.osm.securityservice.userManagement.service;


import com.osm.securityservice.userManagement.data.PermissionRepository;
import com.osm.securityservice.userManagement.data.RoleRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.RoleDTO;
import com.osm.securityservice.userManagement.models.Permission;
import com.osm.securityservice.userManagement.models.Role;
import com.xdev.xdevbase.config.TenantContext;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import com.xdev.xdevbase.utils.OSMLogger;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.*;
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
    public List<RoleDTO> findAll() {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "findAll");

        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            Role adminRole = getRoleByName("ADMIN");
            List<Role> data = repository.findAllByTenantIdAndIsDeletedFalse(tenantId);
            if(adminRole != null && !data.contains(adminRole)) {
                data.add(adminRole);
            }
            List<RoleDTO> result = data.stream().map(item -> modelMapper.map(item, outDTOClass)).toList();
            OSMLogger.logMethodExit(this.getClass(), "findAll", "Found " + result.size() + " entities");
            OSMLogger.logPerformance(this.getClass(), "findAll", startTime, System.currentTimeMillis());
            OSMLogger.logDataAccess(this.getClass(), "READ_ALL", entityClass.getSimpleName());
            return result;
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error finding all entities", e);
            throw e;
        }
    }

    Role getRoleByName(String roleName) {
        return  roleRepository.findByRoleName(roleName).orElse(null);
    }
    @Override
    public void resolveEntityRelations(Role entity) {
        long startTime = System.currentTimeMillis();
        String roleName = entity != null ? entity.getRoleName() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "resolveEntityRelations", 
            "Resolving entity relations for role: " + roleName);
        
        try {
            if (entity.getPermissions() != null && !entity.getPermissions().isEmpty()) {
                OSMLogger.log(this.getClass(), OSMLogger.LogLevel.DEBUG, 
                    "Resolving {} permissions for role: {}", entity.getPermissions().size(), roleName);
                
                Set<Permission> resolved = entity.getPermissions().stream()
                        .map(p -> permissionRepository.findById(p.getId()).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                
                entity.getPermissions().clear();
                entity.setPermissions(resolved);
                
                OSMLogger.logMethodExit(this.getClass(), "resolveEntityRelations", 
                    "Resolved " + resolved.size() + " permissions for role: " + roleName);
                OSMLogger.logPerformance(this.getClass(), "resolveEntityRelations", startTime, System.currentTimeMillis());
                OSMLogger.logDataAccess(this.getClass(), "RESOLVE_PERMISSIONS", "Role");
                
            } else {
                OSMLogger.logMethodExit(this.getClass(), "resolveEntityRelations", 
                    "No permissions to resolve for role: " + roleName);
                OSMLogger.logPerformance(this.getClass(), "resolveEntityRelations", startTime, System.currentTimeMillis());
            }
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), 
                "Error resolving entity relations for role: " + roleName, e);
            throw e;
        }
    }
}
