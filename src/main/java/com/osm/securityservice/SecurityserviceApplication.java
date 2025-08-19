package com.osm.securityservice;

import com.osm.securityservice.userManagement.data.RoleRepository;
import com.osm.securityservice.userManagement.data.UserRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.RoleDTO;
import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.models.Role;
import com.osm.securityservice.userManagement.service.RoleService;
import com.osm.securityservice.userManagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@SpringBootApplication
@ComponentScan(basePackages = {"com.xdev", "com.xdev.xdevbase", "com.osm.securityservice"})
@EnableJpaRepositories(basePackages = {"com.xdev", "com.xdev.xdevbase", "com.osm.securityservice"}, repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
public class SecurityserviceApplication {

    private static final Logger log = LoggerFactory.getLogger(SecurityserviceApplication.class);
    public static final String RAW_PASSWORD1 = "osmAdmin123";
    public static final String OSM_ADMIN = "osmAdmin";
    public static final String MAIL1 = "osmAdmin@example.com";
    public static final String NUMBER1 = "1234567819";
    public static final String OSMADMIN = "OSMADMIN";

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public SecurityserviceApplication(UserService userService,
                                      RoleService roleService,
                                      PasswordEncoder passwordEncoder,
                                      RoleRepository roleRepository,
                                      UserRepository userRepository) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(SecurityserviceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdminUser() {
        return args -> {
            log.info("▶ Bootstrapping default security data...");


            // ===== osmAdminRole =====
            RoleDTO osmAdminRole = ensureRole();

            // ===== Users =====
            ensureUser(osmAdminRole);

            log.info("✅ Bootstrap terminé (aucune duplication si déjà existant).");
        };
    }

    // ========================================================================
    // Helpers
    // ========================================================================


    private RoleDTO ensureRole() {
        Optional<Role> existing = roleRepository.findByRoleName(OSMADMIN);
        if (existing.isPresent()) {
            Role r = existing.get();
            log.debug("✓ Rôle '{}' existe déjà (id={})", r.getRoleName(), r.getId());
            RoleDTO dto = new RoleDTO();
            dto.setId(r.getId());
            dto.setRoleName(r.getRoleName());
            return dto;
        }

        log.info("＋ Création rôle '{}'", OSMADMIN);
        RoleDTO dto = new RoleDTO();
        dto.setRoleName(OSMADMIN);
         return roleService.save(dto);
    }

    private void ensureUser(RoleDTO role) {
        Optional<OSMUser> existing = userRepository.findByUsername(OSM_ADMIN);
        if (existing.isPresent()) {
            log.debug("✓ Utilisateur '{}' déjà existant (id={})", OSM_ADMIN, existing.get().getId());
            return;
        }

        log.info("＋ Création utilisateur '{}'", OSM_ADMIN);
        OSMUserDTO dto = new OSMUserDTO();
        dto.setUsername(OSM_ADMIN);
        dto.setPassword(passwordEncoder.encode(RAW_PASSWORD1));
        dto.setEmail(MAIL1);
        dto.setPhoneNumber(NUMBER1);
        dto.setRole(role);
        dto.setLocked(false);
        userService.save(dto);
    }
}
