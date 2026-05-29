package microservice.service.auth.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import microservice.service.auth.enums.Role;
import microservice.service.auth.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByDocumentNumber(String documentNumber);
    boolean existsByEmail(String email);
    boolean existsByEmailAndDocumentNumber(String email, String documentNumber);
    Optional<User> findByEmail(String email);

    List<User> findByIdInAndRoleIn(Collection<UUID> ids, Collection<Role> roles);

    @Query("""
            SELECT u FROM User u
            WHERE u.role IN :roles
            AND (
                LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, '')))
                    LIKE LOWER(CONCAT('%', :query, '%'))
            )
            ORDER BY u.lastName, u.firstName
            """)
    List<User> searchPaperAuthorCandidates(@Param("query") String query, @Param("roles") Collection<Role> roles);
}