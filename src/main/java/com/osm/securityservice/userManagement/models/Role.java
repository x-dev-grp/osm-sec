package com.osm.securityservice.userManagement.models;

import com.xdev.xdevbase.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import org.hibernate.envers.Audited;

import java.util.HashSet;
import java.util.Set;


@Entity
@Audited
public class Role extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String roleName;
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Permission> permissions = new HashSet<Permission>();

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
        this.permissions = permissions;
    }
}
