package microservice.service.auth.service.impl;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import microservice.service.auth.dto.request.RegisterRequest;
import microservice.service.auth.dto.response.UserResponse;
import microservice.service.auth.enums.Role;
import microservice.service.auth.exception.ConflictException;
import microservice.service.auth.mapper.UserMapper;
import microservice.service.auth.model.User;
import microservice.service.auth.repository.UserRepository;
import microservice.service.auth.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String documentNumber = request.getDocumentNumber().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("El correo ya está registrado");
        }
        if (userRepository.existsByDocumentNumber(documentNumber)) {
            throw new ConflictException("El número de documento ya está registrado");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.AUTHOR;
        String now = Instant.now().toString();
        String hash = passwordEncoder.encode(request.getPassword());

        RegisterRequest normalized = RegisterRequest.builder()
                .documentType(request.getDocumentType())
                .documentNumber(documentNumber)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(email)
                .phoneNumber(request.getPhoneNumber())
                .password(request.getPassword())
                .institution(request.getInstitution())
                .country(request.getCountry())
                .city(request.getCity())
                .role(role)
                .build();

        User entity = userMapper.toNewUser(normalized, hash, role, now);
        User saved = userRepository.save(entity);
        return userMapper.toResponse(saved);
    }
}
