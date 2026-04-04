package microservice.service.auth.service;

import org.springframework.stereotype.Service;

import microservice.service.auth.dto.request.RegisterRequest;
import microservice.service.auth.dto.response.UserResponse;

@Service
public interface UserService {

    UserResponse register(RegisterRequest request);
}
