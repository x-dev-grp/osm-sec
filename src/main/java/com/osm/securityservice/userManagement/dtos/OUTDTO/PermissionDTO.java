package com.osm.securityservice.userManagement.dtos.OUTDTO;

import com.osm.securityservice.userManagement.models.Permission;
import com.xdev.xdevbase.dtos.BaseDto;
import com.xdev.xdevbase.models.OSMModule;


public class PermissionDTO extends BaseDto<Permission> {
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
