package microservice.service.auth.service;

import java.util.List;
import java.util.UUID;

import microservice.service.auth.dto.request.LoginRequest;
import microservice.service.auth.dto.request.RegisterRequest;
import microservice.service.auth.dto.response.AuthResponse;
import microservice.service.auth.dto.response.PaperAuthorResponse;
import microservice.service.auth.dto.response.UserResponse;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getProfile(UUID userId);

    List<PaperAuthorResponse> validatePaperAuthors(List<UUID> userIds);

    List<PaperAuthorResponse> searchPaperAuthorCandidates(String query);
}
