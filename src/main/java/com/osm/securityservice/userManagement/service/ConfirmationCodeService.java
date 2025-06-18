package com.osm.securityservice.userManagement.service;

import com.osm.securityservice.userManagement.data.ConfirmationCodeRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.ConfirmationCodeDTO;
import com.osm.securityservice.userManagement.models.ConfirmationCode;
import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.models.enums.ConfirmationCodeType;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class ConfirmationCodeService extends BaseServiceImpl<ConfirmationCode, ConfirmationCodeDTO, ConfirmationCodeDTO> {
    private final ConfirmationCodeRepository confirmationCodeRepository;

    protected ConfirmationCodeService(BaseRepository<ConfirmationCode> repository, ModelMapper modelMapper, ConfirmationCodeRepository confirmationCodeRepository) {
        super(repository, modelMapper);
        this.confirmationCodeRepository = confirmationCodeRepository;
    }

    public ConfirmationCode getByCode(String code) {
        return confirmationCodeRepository.findByCode(code).orElse(null);
    }

    public ConfirmationCode getByConfirmationCodeTypeAndUser(ConfirmationCodeType confirmationCodeType, OSMUser user) {
        return confirmationCodeRepository.findByConfirmationCodeTypeAndUser(confirmationCodeType, user).orElse(null);
    }
}
