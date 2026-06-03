package com.osm.securityservice.userManagement.data;

import com.osm.securityservice.userManagement.models.OSMUser;
import com.xdev.xdevbase.models.OSMModule;
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

    @Query("SELECT u FROM OSMUser u JOIN u.role r WHERE r.roleName = :roleName " +
            "AND (u.tenantId = :tenantId OR u.tenantId IS NULL)")
    List<OSMUser> findByRoleNameAndTenant(@Param("roleName") String roleName,
                                          @Param("tenantId") UUID tenantId);

    @Query("""
            SELECT DISTINCT u
            FROM OSMUser u
            JOIN u.role r
            LEFT JOIN r.permissions p
            WHERE (u.tenantId = :tenantId OR u.tenantId IS NULL)
              AND (
                   (p.module = :module
                    AND UPPER(p.entity) = UPPER(:entity)
                    AND UPPER(p.permissionName) = UPPER(:permissionName))
                   OR UPPER(r.roleName) IN ('ADMIN', 'OSMADMIN')
              )
            ORDER BY u.firstName, u.lastName, u.username
            """)
    List<OSMUser> findAssignableUsersByPermissionOrAdmin(@Param("tenantId") UUID tenantId,
                                                          @Param("module") OSMModule module,
                                                          @Param("entity") String entity,
                                                          @Param("permissionName") String permissionName);
}
