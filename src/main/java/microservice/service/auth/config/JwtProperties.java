package microservice.service.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String privateKey;
    private String publicKey;
    private long expirationMs = 3600;
}
