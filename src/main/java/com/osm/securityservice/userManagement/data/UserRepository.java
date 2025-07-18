package com.osm.securityservice.userManagement.data;

import com.osm.securityservice.userManagement.models.OSMUser;
import com.xdev.xdevbase.repos.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends BaseRepository<OSMUser> {
    Optional<OSMUser> findByUsername(String username);

    @Query("SELECT u FROM OSMUser u WHERE u.phoneNumber = :input OR LOWER(u.email) = LOWER(:input)")
    Optional<OSMUser> findByPhoneOrEmailIgnoreCase(@Param("input") String input);

    Optional<OSMUser> findByEmailIgnoreCase(String email);

    Optional<OSMUser> findByPhoneNumber(String phoneNumber);

    List<OSMUser> findByRoleRoleNameAndTenantId(String roleName, UUID tenantId);
}
