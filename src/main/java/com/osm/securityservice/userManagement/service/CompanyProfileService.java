package com.osm.securityservice.userManagement.service;

import com.osm.securityservice.userManagement.data.PermissionRepository;
import com.osm.securityservice.userManagement.data.RoleRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.*;
import com.osm.securityservice.userManagement.models.CompanyProfile;
import com.osm.securityservice.userManagement.models.Permission;
import com.osm.securityservice.userManagement.models.Role;
import com.xdev.xdevbase.models.Action;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import com.xdev.xdevbase.utils.OSMLogger;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class CompanyProfileService extends BaseServiceImpl<CompanyProfile, CompanyProfileDTO, CompanyProfileDTO> {
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    public CompanyProfileService(BaseRepository<CompanyProfile> repository, ModelMapper modelMapper, UserService userService,  RoleRepository roleRepository, PermissionRepository permissionRepository) {
        super(repository, modelMapper);
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }


    @Override
    public Set<Action> actionsMapping(CompanyProfile companyProfile) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "actionsMapping", companyProfile);

        try {
            Set<Action> actions = new HashSet<>();
            actions.addAll(Set.of(Action.UPDATE, Action.DELETE, Action.READ));

            OSMLogger.logMethodExit(this.getClass(), "actionsMapping", "Actions: " + actions);
            OSMLogger.logPerformance(this.getClass(), "actionsMapping", startTime, System.currentTimeMillis());

            return actions;

        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error mapping actions for CompanyProfile: " + companyProfile.getId(), e);
            throw e;
        }
    }

    @Transactional
    public CompanyUserDTO save(CompanyUserDTO dto) throws Exception {
        if(dto==null || dto.getCompanyUser()==null)
            return null;
       CompanyProfile company= new CompanyProfile();
       company.setLegalName(dto.getLegalName());
       CompanyProfile companyProfile = repository.save(company);

       OSMUserOUTDTO userDto = modelMapper.map(dto.getCompanyUser(), OSMUserOUTDTO.class);
       Role  adminRole = roleRepository.findByRoleName("ADMIN").orElse(null);
       if(adminRole==null){
           Role role = new Role();
           role.setRoleName("ADMIN");
           List<Permission> permissionList= permissionRepository.findAll();
           Set<Permission> permissions=new HashSet<>(permissionList);
           role.setPermissions(permissions);
           adminRole=roleRepository.save(role);
       }
       userDto.setRole(modelMapper.map(adminRole,RoleDTO.class));
       userDto.setTenantId(companyProfile.getId());
       userDto= userService.addUser(userDto);

        CompanyUserDTO companyUserDTO = new CompanyUserDTO();
        companyUserDTO.setLegalName(companyProfile.getLegalForm());
        companyUserDTO.setCompanyUser(userDto);
        return companyUserDTO;
    }
}
