package com.osm.securityservice.userManagement.data;

import com.osm.securityservice.userManagement.models.Permission;
import com.xdev.xdevbase.models.OSMModule;
import com.xdev.xdevbase.repos.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends BaseRepository<Permission> {
    // Derived query method (works if fields are named exactly as in the entity)
    Optional<Permission> findByModuleAndEntityAndPermissionName(
            OSMModule module, String entity, String permissionName
    );

}
