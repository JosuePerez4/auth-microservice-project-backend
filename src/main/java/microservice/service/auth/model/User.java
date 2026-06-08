package microservice.service.auth.model;

import java.util.UUID;

import microservice.service.auth.enums.DocumentType;
import microservice.service.auth.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;
    @Column(name = "document_number", unique = true)
    private String documentNumber;
    private String firstName;
    private String lastName;
    @Column(name = "email", unique = true)
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private String institution;
    private String country;
    private String city;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;
    @Column(name = "active")
    private Boolean active = true;
    private String createdAt;
    private String updatedAt;
}

