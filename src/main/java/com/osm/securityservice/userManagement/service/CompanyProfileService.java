package com.osm.securityservice.userManagement.service;

import com.osm.securityservice.userManagement.data.PermissionRepository;
import com.osm.securityservice.userManagement.data.RoleRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.CompanyProfileDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.CompanyUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserOUTDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.RoleDTO;
import com.osm.securityservice.userManagement.models.CompanyProfile;
import com.osm.securityservice.userManagement.models.Permission;
import com.osm.securityservice.userManagement.models.Role;
import com.xdev.xdevbase.models.Action;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import com.xdev.xdevbase.services.utils.SearchSpecificationBuilder;
import com.xdev.xdevbase.utils.OSMLogger;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
public class CompanyProfileService extends BaseServiceImpl<CompanyProfile, CompanyProfileDTO, CompanyProfileDTO> {
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final SearchSpecificationBuilder<CompanyProfile> specificationBuilder;

    public CompanyProfileService(BaseRepository<CompanyProfile> repository, ModelMapper modelMapper, UserService userService, RoleRepository roleRepository, PermissionRepository permissionRepository, SearchSpecificationBuilder<CompanyProfile> specificationBuilder) {
        super(repository, modelMapper);
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.specificationBuilder = specificationBuilder;
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
        if (dto == null || dto.getCompanyUser() == null) return null;
        CompanyProfile company = new CompanyProfile();
        company.setLegalName(dto.getLegalName());
        company.setActive(true);
        CompanyProfile companyProfile = repository.save(company);

        OSMUserOUTDTO userDto = modelMapper.map(dto.getCompanyUser(), OSMUserOUTDTO.class);
        Role adminRole = roleRepository.findByRoleName("ADMIN").orElse(null);
        if (adminRole == null) {
            Role role = new Role();
            role.setRoleName("ADMIN");
            List<Permission> permissionList = permissionRepository.findAll();
            Set<Permission> permissions = new HashSet<>(permissionList);
            role.setPermissions(permissions);
            adminRole = roleRepository.save(role);
        }
        userDto.setRole(modelMapper.map(adminRole, RoleDTO.class));
        userDto.setTenantId(companyProfile.getId());
        userDto = userService.addUser(userDto);

        CompanyUserDTO companyUserDTO = new CompanyUserDTO();
        companyUserDTO.setLegalName(companyProfile.getLegalForm());
        companyUserDTO.setCompanyUser(userDto);
        return companyUserDTO;
    }

    @Override
    public CompanyProfileDTO findById(UUID id) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "findById", id);

        try {
            Optional<CompanyProfile> data = repository.findByIdAndIsDeletedFalse(id);
            if (data.isEmpty()) {
                OSMLogger.log(this.getClass(), OSMLogger.LogLevel.WARN, "Entity not found with ID: {}", id);
                throw new EntityNotFoundException("Entity not found with this id " + id);
            } else {
                CompanyProfileDTO result = modelMapper.map(data.get(), outDTOClass);
                OSMLogger.logMethodExit(this.getClass(), "findById", result);
                OSMLogger.logPerformance(this.getClass(), "findById", startTime, System.currentTimeMillis());
                OSMLogger.logDataAccess(this.getClass(), "READ", entityClass.getSimpleName());
                return result;
            }
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error finding entity by ID: " + id, e);
            throw e;
        }
    }

    @Override
    public List<CompanyProfileDTO> findAll() {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "findAll");

        try {
            Collection<CompanyProfile> data = repository.findAllByIsDeletedFalse();
            List<CompanyProfileDTO> result = data.stream().map(item -> modelMapper.map(item, outDTOClass)).toList();
            OSMLogger.logMethodExit(this.getClass(), "findAll", "Found " + result.size() + " entities");
            OSMLogger.logPerformance(this.getClass(), "findAll", startTime, System.currentTimeMillis());
            OSMLogger.logDataAccess(this.getClass(), "READ_ALL", entityClass.getSimpleName());
            return result;
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error finding all entities", e);
            throw e;
        }
    }

    @Override
    public Page<CompanyProfileDTO> findAll(int page, int size, String sort, String direction) {
        long startTime = System.currentTimeMillis();
        OSMLogger.logMethodEntry(this.getClass(), "findAll", page, size, sort, direction);

        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);  // "ASC" or "DESC"
            Sort sortObject = Sort.by(sortDirection, sort);  // Sort by the field and direction
            Pageable pageable = PageRequest.of(page, size, sortObject);
            Page<CompanyProfile> data = repository.findAllByIsDeletedFalse(pageable);

            Page<CompanyProfileDTO> result = data.map(item -> modelMapper.map(item, outDTOClass));
            OSMLogger.logMethodExit(this.getClass(), "findAll", "Page " + page + " with " + result.getContent().size() + " entities");
            OSMLogger.logPerformance(this.getClass(), "findAll", startTime, System.currentTimeMillis());
            OSMLogger.logDataAccess(this.getClass(), "READ_PAGEABLE", entityClass.getSimpleName());
            return result;
        } catch (Exception e) {
            OSMLogger.logException(this.getClass(), "Error finding entities with pagination", e);
            throw e;
        }
    }

    /*

        @Override
        public SearchResponse<CompanyProfile, CompanyProfileDTO> search(SearchData searchData) {
            long startTime = System.currentTimeMillis();
            OSMLogger.logMethodEntry(this.getClass(), "search", searchData);

            try {
                int page = searchData.getPage() != null ? searchData.getPage() : 0;
                int size = searchData.getSize() != null ? searchData.getSize() : 10;
                Sort.Direction direction = (searchData.getOrder() != null && searchData.getOrder().equalsIgnoreCase("DESC")) ? Sort.Direction.DESC : Sort.Direction.ASC;
                String sort = searchData.getSort() != null ? searchData.getSort() : "createdDate";
                Pageable pageable = PageRequest.of(page, size, direction, sort);

                Specification<CompanyProfile> spec = null;
                if (searchData.getSearchData() != null) {
                    spec = specificationBuilder.buildSpecification(searchData.getSearchData());
                }

                Page<CompanyProfile> result;
                if (spec != null) {
                    result = repository.findAll(spec, pageable);
                } else {
                    result = repository.findAll(pageable);
                }
                List<CompanyProfileDTO> dtos = result.getContent().stream().map(
                        element -> modelMapper.map(element, outDTOClass)
                ).toList();

                SearchResponse<CompanyProfile, CompanyProfileDTO> response = new SearchResponse<>(
                        result.getTotalElements(),
                        dtos,
                        result.getTotalPages(),
                        result.getNumber() + 1
                );

                OSMLogger.logMethodExit(this.getClass(), "search", "Found " + dtos.size() + " entities out of " + result.getTotalElements());
                OSMLogger.logPerformance(this.getClass(), "search", startTime, System.currentTimeMillis());
                OSMLogger.logDataAccess(this.getClass(), "SEARCH", entityClass.getSimpleName());

                return response;
            } catch (Exception e) {
                OSMLogger.logException(this.getClass(), "Error during search operation", e);
                return new SearchResponse<>(
                        0,
                        null,
                        0,
                        0
                );
            }
        }

    */
    @Override
    public CompanyProfileDTO update(CompanyProfileDTO dto) {
        Optional<CompanyProfile> existingOpt = repository.findById(dto.getId());
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Company profile not found for tenantId: " + dto.getId());
        }
        CompanyProfile company = existingOpt.get();
        UUID originalId = company.getExternalId();
        modelMapper.map(dto, company);
        company.setExternalId(originalId); // Restore the correct ID after mapping
        CompanyProfile updated = repository.save(company);
        return modelMapper.map(updated, CompanyProfileDTO.class);
    }
}
