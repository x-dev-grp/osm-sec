package com.osm.securityservice.userManagement.models.enums;

public enum ConfirmationMethod {
    PHONE(0), EMAIL(1);


    private final int value;

    ConfirmationMethod(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
