package com.osm.securityservice.userManagement.models;

import com.xdev.xdevbase.entities.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;


@Entity
@Audited
public class Role extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String roleName;
    private String description;

    @ManyToMany(fetch = FetchType.EAGER) // pas de cascade
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "permissions_id", referencedColumnName = "id") // ← nom DB
    )
    private Set<Permission> permissions = new HashSet<>();
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions.clear();
        if (permissions != null) this.permissions.addAll(permissions);
    }
}
