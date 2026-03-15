package com.osm.securityservice.userManagement.models;

import com.osm.securityservice.userManagement.models.enums.ConfirmationCodeType;
import com.xdev.xdevbase.entities.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Audited
public class ConfirmationCode extends BaseEntity {
    private String code;
    @Enumerated(EnumType.STRING)
    private ConfirmationCodeType confirmationCodeType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private OSMUser user;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isExpired() {
        return this.getLastModifiedDate().plusMinutes(15).isBefore(LocalDateTime.now());
    }

    public ConfirmationCodeType getConfirmationCodeType() {
        return confirmationCodeType;
    }

    public void setConfirmationCodeType(ConfirmationCodeType confirmationCodeType) {
        this.confirmationCodeType = confirmationCodeType;
    }

    public OSMUser getUser() {
        return user;
    }

    public void setUser(OSMUser user) {
        this.user = user;
    }
}
