package com.osm.securityservice;

import com.osm.securityservice.userManagement.data.PermissionRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.PermissionDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.RoleDTO;
import com.osm.securityservice.userManagement.service.PermissionService;
import com.osm.securityservice.userManagement.service.RoleService;
import com.osm.securityservice.userManagement.service.UserService;
import com.xdev.xdevbase.models.OSMModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@SpringBootApplication
@ComponentScan(basePackages = {"com.xdev", "com.xdev.xdevbase", "com.osm.securityservice"})
@EnableJpaRepositories(basePackages = {"com.xdev", "com.xdev.xdevbase", "com.osm.securityservice"}, repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
public class SecurityserviceApplication {
    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    public SecurityserviceApplication(UserService userService, RoleService roleService, PermissionRepository permissionRepository, PermissionService permissionService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
    }

    public static void main(String[] args) {
        SpringApplication.run(SecurityserviceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdminUser() {
        return args -> {
            // Check if admin user already exists
            if (userService.getByUsername("admin") != null) {
                return;
            }
            // Create permissions
            PermissionDTO readUsers = new PermissionDTO();
            readUsers.setPermissionName("READ");
            readUsers.setEntity("USER");
            readUsers.setModule(OSMModule.HABILITATION); // Enum or Entity
            PermissionDTO read = permissionService.save(readUsers);

            PermissionDTO writeUsers = new PermissionDTO();
            writeUsers.setPermissionName("WRITE");
            writeUsers.setEntity("USER");
            writeUsers.setModule(OSMModule.HABILITATION);
            PermissionDTO write = permissionService.save(writeUsers);

            PermissionDTO updateUsers = new PermissionDTO();
            updateUsers.setPermissionName("UPDATE");
            updateUsers.setEntity("USER");
            updateUsers.setModule(OSMModule.HABILITATION);
            PermissionDTO update = permissionService.save(updateUsers);

            PermissionDTO deleteUsers = new PermissionDTO();
            deleteUsers.setPermissionName("DELETE");
            deleteUsers.setEntity("USER");
            deleteUsers.setModule(OSMModule.HABILITATION);
            PermissionDTO delete = permissionService.save(deleteUsers);


            if (userService.getByUsername("user") != null) {
                return;
            }
            // Create permissions
            PermissionDTO readReceptions = new PermissionDTO();
            readReceptions.setPermissionName("READ");
            readReceptions.setEntity("RECEPTION");
            readReceptions.setModule(OSMModule.RECEPTION); // Enum or Entity
            PermissionDTO readReception = permissionService.save(readReceptions);

            PermissionDTO writeReceptions = new PermissionDTO();
            writeReceptions.setPermissionName("WRITE");
            writeReceptions.setEntity("RECEPTION");
            writeReceptions.setModule(OSMModule.RECEPTION);
            PermissionDTO writeReception = permissionService.save(writeReceptions);

            RoleDTO userRoleDto = new RoleDTO();
            userRoleDto.setRoleName("OSMUSER");
            userRoleDto.setPermissions(Set.of(readReception, writeReception));
            RoleDTO userRole = roleService.save(userRoleDto);


            // Create user user
            OSMUserDTO user = new OSMUserDTO();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@example.com");
            user.setPhoneNumber("123456789");
            user.setRole(userRole);
            user.setLocked(false); // not locked
            userService.save(user);


            RoleDTO osmAdminRoleDto = new RoleDTO();
            osmAdminRoleDto.setRoleName("OSMADMIN");
            osmAdminRoleDto.setPermissions(Set.of(readReception, writeReception));
            RoleDTO osmAdminRole = roleService.save(osmAdminRoleDto);


            OSMUserDTO osmAdmin = new OSMUserDTO();
            osmAdmin.setUsername("osmAdmin");
            osmAdmin.setPassword(passwordEncoder.encode("osmAdmin123"));
            osmAdmin.setEmail("osmAdmin@example.com");
            osmAdmin.setPhoneNumber("1234567819");
            osmAdmin.setRole(osmAdminRole);
            osmAdmin.setLocked(false);
            userService.save(osmAdmin);
        };
    }

}
