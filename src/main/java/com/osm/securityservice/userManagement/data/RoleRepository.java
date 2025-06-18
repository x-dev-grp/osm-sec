package com.osm.securityservice.userManagement.data;

import com.osm.securityservice.userManagement.models.Role;
import com.xdev.xdevbase.repos.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends BaseRepository<Role> {
}
