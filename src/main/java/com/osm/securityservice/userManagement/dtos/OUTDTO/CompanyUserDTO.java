package com.osm.securityservice.userManagement.dtos.OUTDTO;

public class CompanyUserDTO {
    private String legalName;
    private OSMUserOUTDTO companyUser;

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public OSMUserOUTDTO getCompanyUser() {
        return companyUser;
    }

    public void setCompanyUser(OSMUserOUTDTO companyUser) {
        this.companyUser = companyUser;
    }
}
