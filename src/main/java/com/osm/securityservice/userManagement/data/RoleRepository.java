package com.osm.securityservice.userManagement.data;

import com.osm.securityservice.userManagement.models.Role;
import com.xdev.xdevbase.repos.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends BaseRepository<Role> {
     Optional<Role> findByRoleName(String roleName);
}
