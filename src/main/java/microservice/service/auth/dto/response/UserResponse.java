package microservice.service.auth.dto.response;

import java.util.UUID;

import microservice.service.auth.enums.DocumentType;
import microservice.service.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String institution;
    private String country;
    private String city;
    private Role role;
    private Boolean active;
    private String createdAt;
    private String updatedAt;
}
