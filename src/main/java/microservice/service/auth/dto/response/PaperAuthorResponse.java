package microservice.service.auth.dto.response;

import java.util.UUID;

import microservice.service.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperAuthorResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String displayName;
    private Role role;
}
