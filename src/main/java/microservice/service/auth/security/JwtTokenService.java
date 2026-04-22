package microservice.service.auth.security;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
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
    private final JwtProperties jwtProperties;
    private PrivateKey signingKey;
    private PublicKey verificationKey;

    @PostConstruct
    void init() {
        String privateKeyRaw = jwtProperties.getPrivateKey();
        String publicKeyRaw = jwtProperties.getPublicKey();
        if (!StringUtils.hasText(privateKeyRaw) || !StringUtils.hasText(publicKeyRaw)) {
            throw new IllegalStateException(
                    "jwt.private-key y jwt.public-key deben estar definidos (p. ej. AUTH_JWT_PRIVATE_KEY y AUTH_JWT_PUBLIC_KEY)");
        }
        try {
            this.signingKey = parsePrivateKey(privateKeyRaw);
            this.verificationKey = parsePublicKey(publicKeyRaw);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron cargar las claves RSA para JWT", e);
        }
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
                .verifyWith(verificationKey)
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

    private static PrivateKey parsePrivateKey(String rawKey) throws Exception {
        String normalized = normalizePemOrBase64(rawKey)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .trim();
        byte[] keyBytes = Decoders.BASE64.decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private static PublicKey parsePublicKey(String rawKey) throws Exception {
        String normalized = normalizePemOrBase64(rawKey)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .trim();
        byte[] keyBytes = Decoders.BASE64.decode(normalized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private static String normalizePemOrBase64(String value) {
        String normalized = value.trim().replace("\\n", "\n");
        if (normalized.startsWith("base64:")) {
            byte[] decoded = Base64.getDecoder().decode(normalized.substring("base64:".length()).trim());
            return new String(decoded, StandardCharsets.UTF_8);
        }
        return normalized;
    }

}
