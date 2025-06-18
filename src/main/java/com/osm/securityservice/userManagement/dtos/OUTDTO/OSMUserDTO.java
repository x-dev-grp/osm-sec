package com.osm.securityservice.userManagement.dtos.OUTDTO;

import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.models.enums.ConfirmationMethod;
import com.xdev.xdevbase.dtos.BaseDto;

public class OSMUserDTO extends BaseDto<OSMUser> {
    private String username;
    private String firstName;
    private String lastName;
    private String password;
    private String email;
    private String phoneNumber;
    private RoleDTO role;
    private boolean isLocked;
    private ConfirmationMethod confirmationMethod;
    private boolean isNewUser;

    public boolean isNewUser() {
        return isNewUser;
    }

    public void setNewUser(boolean newUser) {
        isNewUser = newUser;
    }

    public ConfirmationMethod getConfirmationMethod() {
        return confirmationMethod;
    }

    public void setConfirmationMethod(ConfirmationMethod confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public RoleDTO getRole() {
        return role;
    }

    public void setRole(RoleDTO role) {
        this.role = role;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
