package com.osm.securityservice.userManagement.data;

import com.osm.securityservice.userManagement.models.ConfirmationCode;
import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.models.enums.ConfirmationCodeType;
import com.xdev.xdevbase.repos.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfirmationCodeRepository extends BaseRepository<ConfirmationCode> {
    Optional<ConfirmationCode> findByCode(String code);

    Optional<ConfirmationCode> findByConfirmationCodeTypeAndUser(ConfirmationCodeType confirmationCodeType, OSMUser user);
}
