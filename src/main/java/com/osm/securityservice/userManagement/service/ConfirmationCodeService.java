package com.osm.securityservice.userManagement.service;

import com.osm.securityservice.userManagement.data.ConfirmationCodeRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.ConfirmationCodeDTO;
import com.osm.securityservice.userManagement.models.ConfirmationCode;
import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.models.enums.ConfirmationCodeType;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import com.xdev.xdevbase.utils.OSMLogger;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class ConfirmationCodeService extends BaseServiceImpl<ConfirmationCode, ConfirmationCodeDTO, ConfirmationCodeDTO> {
    private final ConfirmationCodeRepository confirmationCodeRepository;

    protected ConfirmationCodeService(BaseRepository<ConfirmationCode> repository, ModelMapper modelMapper, ConfirmationCodeRepository confirmationCodeRepository) {
        super(repository, modelMapper);
        
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "ConfirmationCodeService", "Initializing ConfirmationCodeService");
        
        try {
            this.confirmationCodeRepository = confirmationCodeRepository;
            
            OSMLogger.logMethodExit(this.getClass(), "ConfirmationCodeService", "ConfirmationCodeService initialized successfully");
            OSMLogger.logPerformance(this.getClass(), "ConfirmationCodeService", startTime, System.currentTimeMillis());
            OSMLogger.logSecurityEvent(this.getClass(), "CONFIRMATION_CODE_SERVICE_INITIALIZED", 
                "Confirmation code service initialized successfully");
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error initializing ConfirmationCodeService", e);
            throw e;
        }
    }

    public ConfirmationCode getByCode(String code) {
        long startTime = System.currentTimeMillis();
        String maskedCode = code != null ? code.substring(0, Math.min(3, code.length())) + "..." : "null";
        OSMLogger.logMethodEntry(this.getClass(), "getByCode", "Getting confirmation code: " + maskedCode);
        
        try {
            ConfirmationCode confirmationCode = confirmationCodeRepository.findByCode(code).orElse(null);
            
            if (confirmationCode != null) {
                OSMLogger.logMethodExit(this.getClass(), "getByCode", "Confirmation code found: " + maskedCode);
                OSMLogger.logPerformance(this.getClass(), "getByCode", startTime, System.currentTimeMillis());
                OSMLogger.logDataAccess(this.getClass(), "READ_BY_CODE", "ConfirmationCode");
            } else {
                OSMLogger.logMethodExit(this.getClass(), "getByCode", "Confirmation code not found: " + maskedCode);
                OSMLogger.logPerformance(this.getClass(), "getByCode", startTime, System.currentTimeMillis());
                OSMLogger.logSecurityEvent(this.getClass(), "CONFIRMATION_CODE_NOT_FOUND", 
                    "Confirmation code not found: " + maskedCode);
            }
            
            return confirmationCode;
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), 
                "Error getting confirmation code: " + maskedCode, e);
            throw e;
        }
    }

    public ConfirmationCode getByConfirmationCodeTypeAndUser(ConfirmationCodeType confirmationCodeType, OSMUser user) {
        long startTime = System.currentTimeMillis();
        String username = user != null ? user.getUsername() : "null";
        OSMLogger.logMethodEntry(this.getClass(), "getByConfirmationCodeTypeAndUser", 
            "Getting confirmation code for type: " + confirmationCodeType + ", user: " + username);
        
        try {
            ConfirmationCode confirmationCode = confirmationCodeRepository.findByConfirmationCodeTypeAndUser(confirmationCodeType, user).orElse(null);
            
            if (confirmationCode != null) {
                OSMLogger.logMethodExit(this.getClass(), "getByConfirmationCodeTypeAndUser", 
                    "Confirmation code found for type: " + confirmationCodeType + ", user: " + username);
                OSMLogger.logPerformance(this.getClass(), "getByConfirmationCodeTypeAndUser", startTime, System.currentTimeMillis());
                OSMLogger.logDataAccess(this.getClass(), "READ_BY_TYPE_AND_USER", "ConfirmationCode");
            } else {
                OSMLogger.logMethodExit(this.getClass(), "getByConfirmationCodeTypeAndUser", 
                    "Confirmation code not found for type: " + confirmationCodeType + ", user: " + username);
                OSMLogger.logPerformance(this.getClass(), "getByConfirmationCodeTypeAndUser", startTime, System.currentTimeMillis());
                OSMLogger.logSecurityEvent(this.getClass(), "CONFIRMATION_CODE_NOT_FOUND_BY_TYPE_USER", 
                    "Confirmation code not found for type: " + confirmationCodeType + ", user: " + username);
            }
            
            return confirmationCode;
            
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), 
                "Error getting confirmation code for type: " + confirmationCodeType + ", user: " + username, e);
            throw e;
        }
    }
}
