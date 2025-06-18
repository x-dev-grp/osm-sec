package com.osm.securityservice.userManagement.service;

import com.osm.securityservice.userManagement.data.UserRepository;
import com.osm.securityservice.userManagement.dtos.OUTDTO.ConfirmationCodeDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.OSMUserOUTDTO;
import com.osm.securityservice.userManagement.dtos.OUTDTO.UpdatePasswordDTO;
import com.osm.securityservice.userManagement.models.ConfirmationCode;
import com.osm.securityservice.userManagement.models.OSMUser;
import com.osm.securityservice.userManagement.models.enums.ConfirmationCodeType;
import com.osm.securityservice.userManagement.models.enums.ConfirmationMethod;
import com.xdev.mailSender.models.MailRequest;
import com.xdev.mailSender.services.MailService;
import com.xdev.xdevbase.repos.BaseRepository;
import com.xdev.xdevbase.services.impl.BaseServiceImpl;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.CredentialExpiredException;
import java.security.SecureRandom;
import java.util.*;

@Service
public class UserService extends BaseServiceImpl<OSMUser, OSMUserDTO, OSMUserOUTDTO> implements UserDetailsService {
    private final UserRepository userRepository;
    private final MailService mailService;
    private final ConfirmationCodeService confirmationCodeService;
    private final PasswordEncoder passwordEncoder;

    protected UserService(BaseRepository<OSMUser> repository, ModelMapper modelMapper, UserRepository userRepository, MailService mailService, ConfirmationCodeService confirmationCodeService, PasswordEncoder passwordEncoder) {
        super(repository, modelMapper);
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.confirmationCodeService = confirmationCodeService;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Transactional
    public OSMUserOUTDTO addUser(OSMUserOUTDTO userDTO) throws Exception {
        validateUserDTO(userDTO);
        checkExistUser(userDTO.getUsername(), userDTO.getEmail(), userDTO.getPhoneNumber());
        String rawPassword = generateSecureCode(8);
        String hashedPassword = passwordEncoder.encode(rawPassword);
        OSMUser user = modelMapper.map(userDTO, OSMUser.class);
        user.setPassword(hashedPassword);
        user.setNewUser(true);
        sendConfirmation(userDTO, rawPassword);
        OSMUser savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, OSMUserOUTDTO.class);
    }

    @Transactional
    public OSMUserOUTDTO updateUser(OSMUserOUTDTO userDTO, UUID id) throws Exception {
        if (id == null) throw new IllegalArgumentException("User ID must not be null");

        validateUserDTO(userDTO);

        OSMUser user = repository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(id.toString()));

        checkUserToUpdate(user, userDTO.getUsername(), userDTO.getEmail(), userDTO.getPhoneNumber());

        boolean usernameChanged = !Objects.equals(userDTO.getUsername(), user.getUsername());
        boolean emailChanged = userDTO.getEmail() != null && !Objects.equals(userDTO.getEmail(), user.getEmail());
        boolean phoneChanged = userDTO.getPhoneNumber() != null && !Objects.equals(userDTO.getPhoneNumber(), user.getPhoneNumber());
        user.setLocked(userDTO.isLocked());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setUsername(userDTO.getUsername());
        user.setConfirmationMethod(userDTO.getConfirmationMethod());

        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }

        if (userDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(userDTO.getPhoneNumber());
        }

        userRepository.save(user);
        if (usernameChanged || emailChanged || phoneChanged) {
            String rawPassword = generateSecureCode(8);
            sendConfirmation(userDTO, rawPassword);
        }

        return modelMapper.map(user, OSMUserOUTDTO.class);
    }

    private void checkExistUser(String username, String email, String phoneNumber) {
        if (username != null && userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already in use");
        }
        if (email != null && userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (phoneNumber != null && userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new IllegalArgumentException("Phone number is already in use");
        }
    }

    private void checkUserToUpdate(OSMUser user, String username, String email, String phoneNumber) {
        if (((user.getUsername() != null && username != null && !user.getUsername().equals(username)) || (user.getUsername() == null && username != null)) && userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already in use");
        }
        if (((user.getEmail() != null && email != null && !user.getEmail().equals(email)) || (user.getEmail() == null && email != null)) && userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (((user.getPhoneNumber() != null && phoneNumber != null && !user.getPhoneNumber().equals(phoneNumber)) || (user.getPhoneNumber() == null && phoneNumber != null)) && userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new IllegalArgumentException("Phone number is already in use");
        }
    }

    public OSMUser getByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    private void sendConfirmation(OSMUserOUTDTO userDTO, String rawPassword) throws Exception {
        switch (userDTO.getConfirmationMethod()) {
            case EMAIL -> {
                MailRequest mailRequest = new MailRequest();
                mailRequest.setTo(userDTO.getEmail());
                mailRequest.setSubject("Compte OSM");
                mailRequest.setBody(String.format("Username: %s\nPassword: %s", userDTO.getUsername(), rawPassword));
                mailService.sendEmail(mailRequest);
            }
            case PHONE -> {
                //TODO send sms message
            }
            default -> throw new IllegalArgumentException("Unsupported confirmation method");
        }
    }

    public OSMUserOUTDTO resetPassword(String identifier) throws Exception {
        OSMUser user = userRepository.findByPhoneOrEmailIgnoreCase(identifier).orElse(null);
        if (user != null) {
            if (user.isLocked())
                throw new AccountLockedException("Invalid input");
            if (user.getEmail().toLowerCase().equals(identifier)) {
                String code = generateRandomCode();
                ConfirmationCode confirmationCode = new ConfirmationCode();
                confirmationCode.setCode(code);
                confirmationCode.setUser(user);
                confirmationCode.setConfirmationCodeType(ConfirmationCodeType.RESETPASSWORD);
                saveConfirmationCode(confirmationCode);
                MailRequest mailrequest = new MailRequest();
                mailrequest.setTo(user.getEmail());
                mailrequest.setSubject("Password reset");
                mailrequest.setBody("Code: " + code);
                mailService.sendEmail(mailrequest);
            } else {
                //TODO send to phone number
            }
            return modelMapper.map(user, OSMUserOUTDTO.class);

        } else {
            throw new IllegalArgumentException("Invalid input");
        }
    }

    private ConfirmationCode saveConfirmationCode(ConfirmationCode code) {
        ConfirmationCode existedCode = confirmationCodeService.getByConfirmationCodeTypeAndUser(ConfirmationCodeType.RESETPASSWORD, code.getUser());
        if (existedCode != null) {
            existedCode.setCode(code.getCode());
            ConfirmationCodeDTO codeDTO = confirmationCodeService.save(modelMapper.map(existedCode, ConfirmationCodeDTO.class));
            return modelMapper.map(codeDTO, ConfirmationCode.class);
        }
        ConfirmationCodeDTO codeDTO = confirmationCodeService.save(modelMapper.map(code, ConfirmationCodeDTO.class));
        return modelMapper.map(codeDTO, ConfirmationCode.class);
    }


    public boolean validateResetCode(String code, UUID userId) throws Exception {
        OSMUser user = userRepository.findById(userId).orElse(null);
        if (user == null)
            throw new IllegalArgumentException("Invalid code");

        ConfirmationCode existedCode = confirmationCodeService.getByConfirmationCodeTypeAndUser(ConfirmationCodeType.RESETPASSWORD, user);
        if (existedCode == null || !existedCode.getCode().equals(code)) {
            throw new IllegalArgumentException("Invalid code");
        }
        if (existedCode.isExpired()) {
            throw new CredentialExpiredException("Expired code");
        }
        return true;
    }

    public void updatePassword(UpdatePasswordDTO dto, UUID userId) {
        OSMUser user = userRepository.findById(userId).orElse(null);
        if (user == null)
            throw new IllegalArgumentException("Invalid user");
        if (!dto.getNewPassword().equals(dto.getOldPassword())) {
            if (!dto.getNewPassword().equals(dto.getNewPasswordConfirmation())) {
                throw new IllegalArgumentException("Invalid password");
            }
            String hashedPassword = passwordEncoder.encode(dto.getNewPassword());
            user.setPassword(hashedPassword);
            user.setNewUser(false);
            userRepository.save(user);
        }
    }

    private void validateUserDTO(OSMUserOUTDTO userDTO) {
        if (userDTO == null) {
            throw new IllegalArgumentException("User data must not be null");
        }
        if (userDTO.getUsername() == null || userDTO.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (userDTO.getConfirmationMethod() == null) {
            throw new IllegalArgumentException("Confirmation method is required");
        }
        if (userDTO.getConfirmationMethod() == ConfirmationMethod.EMAIL && (userDTO.getEmail() == null || userDTO.getEmail().isBlank())) {
            throw new IllegalArgumentException("Email is required for email confirmation");
        }
        if (userDTO.getConfirmationMethod() == ConfirmationMethod.PHONE && (userDTO.getPhoneNumber() == null || userDTO.getPhoneNumber().isBlank())) {
            throw new IllegalArgumentException("Phone number is required for phone confirmation");
        }
    }

    public String generateSecureCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String generateRandomCode() {
        int code = (int) (Math.random() * 900_000) + 100_000; // range: 100000–999999
        return String.valueOf(code);
    }

    public List<OSMUserDTO> findByRoleName(String roleName) {
        return userRepository.findByRoleRoleName(roleName).stream().map(
                user -> modelMapper.map(user, OSMUserDTO.class)
        ).toList();
    }


    @Override
    public Set<String> actionsMapping(OSMUser user) {
        Set<String> actions = new HashSet<>();
        actions.add("READ");
        if (user.getRole().getRoleName().equals("ADMIN")) {
            actions.addAll(Set.of("UPDATE", "DELETE", "CREATE"));
        }
        return actions;
    }


}
