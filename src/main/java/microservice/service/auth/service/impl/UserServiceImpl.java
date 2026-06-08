package microservice.service.auth.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import microservice.service.auth.dto.request.LoginRequest;
import microservice.service.auth.dto.request.RegisterRequest;
import microservice.service.auth.dto.response.AuthResponse;
import microservice.service.auth.dto.response.PaperAuthorResponse;
import microservice.service.auth.dto.response.UserResponse;
import microservice.service.auth.exception.BadRequestException;
import microservice.service.auth.security.JwtTokenService;
import microservice.service.auth.enums.Role;
import microservice.service.auth.exception.ConflictException;
import microservice.service.auth.exception.UnauthorizedException;
import microservice.service.auth.mapper.UserMapper;
import microservice.service.auth.model.User;
import microservice.service.auth.repository.UserRepository;
import microservice.service.auth.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<Role> PAPER_AUTHOR_ROLES = Set.of(Role.AUTHOR, Role.GUEST_SPOKER);
    private static final int SEARCH_MIN_QUERY_LENGTH = 2;
    private static final int SEARCH_MAX_RESULTS = 20;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        return toAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new UnauthorizedException("Su cuenta de CHAIR aún no ha sido aprobada por el administrador.");
        }

        return toAuthResponse(user);
    }

    @Override
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        return userMapper.toResponse(user);
    }

    @Override
    public List<PaperAuthorResponse> validatePaperAuthors(List<UUID> userIds) {
        List<UUID> distinctIds = normalizeAuthorIds(userIds);
        List<User> found = userRepository.findByIdInAndRoleIn(distinctIds, PAPER_AUTHOR_ROLES);

        if (found.size() != distinctIds.size()) {
            Set<UUID> validIds = found.stream().map(User::getId).collect(Collectors.toSet());
            List<UUID> invalid = distinctIds.stream()
                    .filter(id -> !validIds.contains(id))
                    .toList();
            throw new BadRequestException(
                    "Usuarios no encontrados o sin rol AUTHOR/GUEST_SPOKER: " + invalid);
        }

        Map<UUID, User> byId = found.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return distinctIds.stream()
                .map(id -> toPaperAuthorResponse(byId.get(id)))
                .toList();
    }

    @Override
    public List<PaperAuthorResponse> searchPaperAuthorCandidates(String query) {
        String normalized = query != null ? query.trim() : "";
        if (normalized.length() < SEARCH_MIN_QUERY_LENGTH) {
            throw new BadRequestException(
                    "La búsqueda debe tener al menos " + SEARCH_MIN_QUERY_LENGTH + " caracteres");
        }

        return userRepository.searchPaperAuthorCandidates(normalized, PAPER_AUTHOR_ROLES).stream()
                .limit(SEARCH_MAX_RESULTS)
                .map(this::toPaperAuthorResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getChairs() {
        return userRepository.findByRole(Role.CHAIR).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void activateChair(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (user.getRole() != Role.CHAIR) {
            throw new BadRequestException("El usuario no tiene rol CHAIR");
        }
        user.setActive(true);
        userRepository.save(user);
    }

    private static List<UUID> normalizeAuthorIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BadRequestException("Debe indicar al menos un autor");
        }
        return new ArrayList<>(new LinkedHashSet<>(userIds));
    }

    private PaperAuthorResponse toPaperAuthorResponse(User user) {
        return PaperAuthorResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .displayName(displayName(user))
                .role(user.getRole())
                .build();
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtTokenService.generateAccessToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .name(displayName(user))
                .build();
    }

    private static String displayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? first : combined;
    }
}
