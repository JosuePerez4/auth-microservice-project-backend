package microservice.service.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import microservice.service.auth.dto.request.ValidatePaperAuthorsRequest;
import microservice.service.auth.dto.response.PaperAuthorResponse;
import microservice.service.auth.dto.response.UserResponse;
import microservice.service.auth.dto.response.ValidatePaperAuthorsResponse;
import microservice.service.auth.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Consulta y validación de usuarios para papers")
public class UserController {

        private final UserService userService;

        @PostMapping("/paper-authors/validate")
        @Operation(summary = "Validar autores de un paper", description = "Comprueba que los IDs existen y tienen rol AUTHOR o GUEST_SPOKER", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<ValidatePaperAuthorsResponse> validatePaperAuthors(
                        @Valid @RequestBody ValidatePaperAuthorsRequest request) {
                List<PaperAuthorResponse> authors = userService.validatePaperAuthors(request.getUserIds());
                return ResponseEntity.ok(new ValidatePaperAuthorsResponse(authors));
        }

        @GetMapping("/paper-authors/search")
        @Operation(summary = "Buscar candidatos a coautor", description = "Busca por email o nombre entre usuarios AUTHOR o GUEST_SPOKER", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<List<PaperAuthorResponse>> searchPaperAuthorCandidates(
                        @RequestParam("q") String query) {
                return ResponseEntity.ok(userService.searchPaperAuthorCandidates(query));
        }

        @GetMapping("/chairs")
        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Listar usuarios registrados como CHAIR", description = "Requiere rol ADMIN", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<List<UserResponse>> getChairs() {
                return ResponseEntity.ok(userService.getChairs());
        }

        @PostMapping("/chairs/{id}/activate")
        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Activar/Aceptar usuario CHAIR", description = "Activa a un usuario CHAIR para permitirle el acceso al sistema. Requiere rol ADMIN", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<Void> activateChair(
                        @org.springframework.web.bind.annotation.PathVariable("id") java.util.UUID id) {
                userService.activateChair(id);
                return ResponseEntity.ok().build();
        }

        @PostMapping("/chairs/{id}/deactivate")
        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Desactivar/Rechazar usuario CHAIR", description = "Desactiva a un usuario CHAIR. Requiere rol ADMIN", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<Void> deactivateChair(
                        @org.springframework.web.bind.annotation.PathVariable("id") java.util.UUID id) {
                userService.deactivateChair(id);
                return ResponseEntity.ok().build();
        }

}
