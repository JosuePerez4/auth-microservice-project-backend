package microservice.service.auth.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePaperAuthorsResponse {

    private List<PaperAuthorResponse> authors;
}
