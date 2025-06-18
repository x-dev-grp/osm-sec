package com.osm.securityservice.userManagement.dtos.OUTDTO;

import com.osm.securityservice.userManagement.models.ConfirmationCode;
import com.osm.securityservice.userManagement.models.enums.ConfirmationCodeType;
import com.xdev.xdevbase.dtos.BaseDto;

public class ConfirmationCodeDTO extends BaseDto<ConfirmationCode> {
    private String code;
    private ConfirmationCodeType confirmationCodeType;
    private OSMUserDTO user;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ConfirmationCodeType getConfirmationCodeType() {
        return confirmationCodeType;
    }

    public void setConfirmationCodeType(ConfirmationCodeType confirmationCodeType) {
        this.confirmationCodeType = confirmationCodeType;
    }

    public OSMUserDTO getUser() {
        return user;
    }

    public void setUser(OSMUserDTO user) {
        this.user = user;
    }
}
