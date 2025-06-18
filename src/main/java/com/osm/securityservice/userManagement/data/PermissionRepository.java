package com.osm.securityservice.userManagement.data;

import com.osm.securityservice.userManagement.models.Permission;
import com.xdev.xdevbase.repos.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends BaseRepository<Permission> {
}
