package microservice.service.auth.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePaperAuthorsRequest {

    @NotEmpty(message = "Debe indicar al menos un autor")
    private List<UUID> userIds;
}
