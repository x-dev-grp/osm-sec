package com.osm.securityservice.userManagement.models;

import com.xdev.xdevbase.entities.BaseEntity;
import com.xdev.xdevbase.models.OSMModule;
import jakarta.persistence.Entity;
import org.hibernate.envers.Audited;

@Entity
@Audited
public class Permission extends BaseEntity {
    private String permissionName;
    private OSMModule module;
    private String entity;

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public OSMModule getModule() {
        return module;
    }

    public void setModule(OSMModule module) {
        this.module = module;
    }

}
