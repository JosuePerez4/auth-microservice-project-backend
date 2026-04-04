package microservice.service.auth.dto.request;

import microservice.service.auth.enums.DocumentType;
import microservice.service.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotNull(message = "El tipo de documento es obligatorio")
    private DocumentType documentType;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 32, message = "El número de documento no puede superar 32 caracteres")
    private String documentNumber;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 120, message = "El apellido no puede superar 120 caracteres")
    private String lastName;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 255, message = "El correo no puede superar 255 caracteres")
    private String email;

    @Size(max = 32, message = "El teléfono no puede superar 32 caracteres")
    private String phoneNumber;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    private String password;

    @Size(max = 255, message = "La institución no puede superar 255 caracteres")
    private String institution;

    @Size(max = 120, message = "El país no puede superar 120 caracteres")
    private String country;

    @Size(max = 120, message = "La ciudad no puede superar 120 caracteres")
    private String city;

    /**
     * Si no se envía, se asigna {@link Role#AUTHOR} para evitar auto-registro con privilegios elevados.
     */
    private Role role;
}
