package microservice.service.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import microservice.service.auth.config.JwtProperties;
import microservice.service.auth.enums.Role;
import microservice.service.auth.model.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final int MIN_HMAC_KEY_BYTES = 32;

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = jwtProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("jwt.secret debe estar definido (p. ej. JWT_SECRET en el entorno)");
        }
        byte[] keyBytes;
        if (secret.startsWith("base64:")) {
            keyBytes = Decoders.BASE64.decode(secret.substring("base64:".length()).trim());
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < MIN_HMAC_KEY_BYTES) {
            throw new IllegalStateException(
                    ("JWT_SECRET / jwt.secret demasiado corto (%d bytes; mínimo %d). Para HS256 hacen falta al menos 32 "
                            + "caracteres en texto plano, o `base64:` + Base64 de 32+ bytes. Si usas .env pero falla, "
                            + "revisa que no tengas JWT_SECRET o AUTH_JWT_SECRET corto en el entorno del IDE/sistema "
                            + "(tienen prioridad sobre el archivo).")
                                    .formatted(keyBytes.length, MIN_HMAC_KEY_BYTES));
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public JwtPrincipal parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID id = UUID.fromString(claims.getSubject());
        String email = claims.get(CLAIM_EMAIL, String.class);
        String roleName = claims.get(CLAIM_ROLE, String.class);
        if (email == null || roleName == null) {
            throw new JwtException("Token sin claims obligatorios");
        }
        Role role = Role.valueOf(roleName);
        return new JwtPrincipal(id, email, role);
    }

}
