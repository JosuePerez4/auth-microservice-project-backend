package microservice.service.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		// No cargar el .env local durante tests; base embebida y JWT fijo
		"spring.config.import=optional:file:./.__spring_boot_test_env_placeholder__",
		"server.port=0",
		"spring.datasource.url=jdbc:h2:mem:authtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"jwt.secret=test-jwt-secret-must-be-at-least-32-chars!!",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.show-sql=false"
})
class AuthApplicationTests {

	@Test
	void contextLoads() {
	}

}
