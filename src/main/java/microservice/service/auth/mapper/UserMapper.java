package microservice.service.auth.mapper;

import org.springframework.stereotype.Component;

import microservice.service.auth.dto.request.RegisterRequest;
import microservice.service.auth.dto.response.UserResponse;
import microservice.service.auth.enums.Role;
import microservice.service.auth.model.User;

@Component
public class UserMapper {

    public User toNewUser(RegisterRequest request, String passwordHash, Role effectiveRole, String nowIso) {
        User user = new User();
        user.setDocumentType(request.getDocumentType());
        user.setDocumentNumber(trimToNull(request.getDocumentNumber()));
        user.setFirstName(trimToNull(request.getFirstName()));
        user.setLastName(trimToNull(request.getLastName()));
        user.setEmail(trimToNull(request.getEmail()) != null ? request.getEmail().trim().toLowerCase() : null);
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        user.setPasswordHash(passwordHash);
        user.setInstitution(trimToNull(request.getInstitution()));
        user.setCountry(trimToNull(request.getCountry()));
        user.setCity(trimToNull(request.getCity()));
        user.setRole(effectiveRole);
        user.setCreatedAt(nowIso);
        user.setUpdatedAt(nowIso);
        return user;
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .documentType(user.getDocumentType())
                .documentNumber(user.getDocumentNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .institution(user.getInstitution())
                .country(user.getCountry())
                .city(user.getCity())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
