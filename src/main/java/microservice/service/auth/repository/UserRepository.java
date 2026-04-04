package microservice.service.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import microservice.service.auth.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByDocumentNumber(String documentNumber);
    boolean existsByEmail(String email);
    boolean existsByEmailAndDocumentNumber(String email, String documentNumber);
}