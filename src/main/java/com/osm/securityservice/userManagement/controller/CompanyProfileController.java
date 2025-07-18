package com.osm.securityservice.userManagement.controller;


import com.osm.securityservice.userManagement.dtos.OUTDTO.CompanyProfileDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.CompanyUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserOUTDTO;
import com.osm.securityservice.userManagement.models.CompanyProfile;
import com.osm.securityservice.userManagement.service.CompanyProfileService;
import com.xdev.xdevbase.apiDTOs.ApiSingleResponse;
import com.xdev.xdevbase.controllers.impl.BaseControllerImpl;
import com.xdev.xdevbase.services.BaseService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/security/company-profile")

public class CompanyProfileController extends BaseControllerImpl<CompanyProfile, CompanyProfileDTO, CompanyProfileDTO> {

    private final CompanyProfileService companyProfileService;
    public CompanyProfileController(BaseService<CompanyProfile, CompanyProfileDTO, CompanyProfileDTO> baseService, ModelMapper modelMapper, CompanyProfileService companyProfileService) {
        super(baseService, modelMapper);
        this.companyProfileService = companyProfileService;
    }

    @PostMapping("/save")
    public ResponseEntity<?>  saveCompany(@RequestBody CompanyUserDTO userDTO) {
        try {
            CompanyUserDTO companyUser =companyProfileService.save(userDTO);
            return ResponseEntity.ok(companyUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Message error: " + e.getMessage());
        }
    }

    @Override
    protected String getResourceName() {
        return "CompanyProfile";
    }


}
