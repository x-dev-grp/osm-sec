package com.osm.securityservice.userManagement.models;

import com.osm.securityservice.userManagement.models.enums.ConfirmationMethod;
import com.xdev.xdevbase.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import org.hibernate.envers.Audited;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Entity
@Audited
public class OSMUser extends BaseEntity implements UserDetails {
    @Column(unique = true, nullable = false)
    private String username;
    private String firstName;
    private String lastName;
    private String password;
    @Column(unique = true)
    private String email;
    @Column(unique = true)
    private String phoneNumber;
    @ManyToOne(fetch = FetchType.EAGER)
    private Role role;
    private boolean isLocked;
    private ConfirmationMethod confirmationMethod;
    private boolean isNewUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRole().getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(String.format("%s:%s:%s", permission.getModule().toString().toUpperCase(), permission.getEntity().toUpperCase(), permission.getPermissionName().toUpperCase())))
                .toList();
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public void setNewUser(boolean newUser) {
        isNewUser = newUser;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public ConfirmationMethod getConfirmationMethod() {
        return confirmationMethod;
    }

    public void setConfirmationMethod(ConfirmationMethod confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
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
