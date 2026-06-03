# Auth Service

Microservicio Spring Boot para registro, login y consulta del perfil autenticado.
Expone una API REST versionada en `/api/v1/auth`, emite JWT firmados con RSA y
persiste usuarios en PostgreSQL mediante JPA.

## Arquitectura

- **Runtime:** Java 21, Spring Boot 4.
- **API:** Spring Web MVC con validacion Jakarta y documentacion OpenAPI.
- **Seguridad:** Spring Security stateless, filtro Bearer JWT y respuestas de
  error JSON.
- **Datos:** JPA/Hibernate sobre PostgreSQL en ejecucion normal; H2 se declara
  para pruebas.
- **Descubrimiento/operacion:** cliente Eureka por autoconfiguracion de Spring
  Cloud y Actuator con `health`/`info`.
- **Empaquetado:** Maven Wrapper y Docker multi-stage con Temurin 21.

Flujo principal:

1. `POST /api/v1/auth/register` crea un usuario, normaliza el correo a
   minusculas y devuelve un token.
2. `POST /api/v1/auth/login` valida correo y password contra el hash guardado y
   devuelve un token.
3. `GET /api/v1/auth/me` valida `Authorization: Bearer <token>` y carga el
   perfil desde la base de datos.

Los tokens incluyen `sub` con el UUID del usuario y los claims `email` y `role`.

## Configuracion requerida

La aplicacion lee `src/main/resources/application.properties` y, si existe, un
archivo `.env` en la raiz del proyecto:

```properties
spring.config.import=optional:file:${user.dir}/.env[.properties]
```

Variables principales:

| Variable | Uso | Notas |
| --- | --- | --- |
| `PORT` | Puerto HTTP | Default del codigo: `8081`. El ejemplo local usa `8082`. |
| `SPRING_DATASOURCE_URL` | JDBC de PostgreSQL | Requerida en runtime. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de BD | Default vacio. |
| `SPRING_DATASOURCE_PASSWORD` | Password de BD | Default vacio. |
| `JWT_PRIVATE_KEY` | Clave RSA privada para firmar JWT | Requerida. Debe ser PKCS#8. |
| `JWT_PUBLIC_KEY` | Clave RSA publica para validar JWT | Requerida. Debe ser X.509. |
| `JWT_EXPIRATION_MS` | Duracion del token en milisegundos | Requerida por el placeholder actual. |
| `FRONTEND_URL` | Origenes CORS permitidos | Requerida; acepta lista separada por comas. |
| `EUREKA_SERVER_URL` | URL `defaultZone` del servidor Eureka | Requerida por la configuracion actual. |

Ejemplo local sin secretos reales:

```properties
PORT=8082
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth
SPRING_DATASOURCE_USERNAME=auth_user
SPRING_DATASOURCE_PASSWORD=change-me
JWT_EXPIRATION_MS=3600000
FRONTEND_URL=http://localhost:5173,http://localhost:3000
EUREKA_SERVER_URL=http://localhost:8761/eureka
JWT_PRIVATE_KEY=base64:<private-key-pem-base64>
JWT_PUBLIC_KEY=base64:<public-key-pem-base64>
```

El servicio acepta claves en formato PEM con saltos de linea escapados (`\n`) o
con prefijo `base64:`. Para generar un par RSA local:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
openssl rsa -pubout -in private.pem -out public.pem
printf 'JWT_PRIVATE_KEY=base64:%s\n' "$(base64 -w0 private.pem)"
printf 'JWT_PUBLIC_KEY=base64:%s\n' "$(base64 -w0 public.pem)"
```

## Ejecutar en desarrollo

Requisitos:

- JDK 21.
- PostgreSQL accesible con las credenciales configuradas.
- Variables de entorno anteriores exportadas o guardadas en `.env`.

Comandos comunes:

```bash
./mvnw spring-boot:run
./mvnw -B test
./mvnw -B package
```

Swagger UI queda disponible en el puerto configurado por `PORT`:

```text
http://localhost:<port>/swagger-ui.html
```

El documento OpenAPI se expone en:

```text
http://localhost:<port>/v3/api-docs
```

Actuator expone probes operativas publicas:

```text
http://localhost:<port>/actuator/health
http://localhost:<port>/actuator/info
```

## API publica

### Registrar usuario

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "documentType": "CC",
  "documentNumber": "123456",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "phoneNumber": "+573001112233",
  "password": "password-seguro",
  "institution": "Example University",
  "country": "CO",
  "city": "Bogota"
}
```

Respuesta `201 Created`:

```json
{
  "accessToken": "<jwt>",
  "name": "Ada Lovelace"
}
```

Restricciones verificadas:

- `documentType`: `CC`, `CE` o `TI`.
- `email` se guarda en minusculas.
- `documentNumber` y `email` deben ser unicos.
- `password` debe tener entre 8 y 128 caracteres.
- Si no se envia `role`, se asigna `AUTHOR`.
- Roles definidos: `ADMIN`, `CHAIR`, `AUTHOR`, `ASISTANT`.

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "ada@example.com",
  "password": "password-seguro"
}
```

Respuesta `200 OK`:

```json
{
  "accessToken": "<jwt>",
  "name": "Ada Lovelace"
}
```

Credenciales invalidas devuelven `401 Unauthorized`.

### Perfil autenticado

```http
GET /api/v1/auth/me
Authorization: Bearer <jwt>
```

Respuesta `200 OK`:

```json
{
  "id": "5ad1a4c0-6a67-4e8f-ae18-f5b071a59e76",
  "documentType": "CC",
  "documentNumber": "123456",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "phoneNumber": "+573001112233",
  "institution": "Example University",
  "country": "CO",
  "city": "Bogota",
  "role": "AUTHOR",
  "createdAt": "2026-04-28T22:00:00Z",
  "updatedAt": "2026-04-28T22:00:00Z"
}
```

## Seguridad y CORS

- La aplicacion no usa sesiones HTTP, formulario de login ni HTTP Basic.
- Son publicos `POST /api/v1/auth/register`, `POST /api/v1/auth/login`,
  `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error` y todas las
  peticiones `OPTIONS`.
- Tambien son publicos `/actuator` y `/actuator/**`; la configuracion actual
  solo expone `health` e `info`.
- Cualquier otro endpoint requiere `Authorization: Bearer <jwt>`.
- `FRONTEND_URL` configura los patrones de origen permitidos por CORS. Cuando
  haya varios origenes, separalos con comas sin espacios obligatorios:
  `https://app.example.com,https://admin.example.com`.
- CORS permite credenciales, todos los headers de entrada y expone el header
  `Authorization`.
- El rol viaja en el JWT como autoridad `ROLE_<rol>`, pero los endpoints
  actuales no aplican reglas por rol (`@PreAuthorize` o similares). En
  particular, `RegisterRequest` acepta `role`; si se omite, el servicio asigna
  `AUTHOR`.

## Formato de errores

Las respuestas de error usan una estructura JSON comun:

```json
{
  "timestamp": "2026-04-28T22:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Datos de entrada invalidos",
  "path": "/api/v1/auth/register",
  "violations": [
    {
      "field": "email",
      "message": "El correo no tiene un formato valido"
    }
  ]
}
```

Codigos relevantes:

- `400 Bad Request`: JSON mal formado o validaciones de entrada.
- `401 Unauthorized`: token ausente/invalido o credenciales invalidas.
- `403 Forbidden`: autenticado sin permisos suficientes.
- `409 Conflict`: correo o documento duplicado.

## Docker

Construir imagen:

```bash
docker build -t auth-service .
```

Ejecutar contenedor:

```bash
docker run --rm -p 8082:8082 --env-file .env auth-service
```

La imagen compila el JAR con Maven en una etapa de build, copia
`target/auth-0.0.1-SNAPSHOT.jar` a una imagen JRE Alpine y ejecuta el proceso
como usuario no root. Puedes pasar opciones JVM con `JAVA_OPTS`.

El Dockerfile declara `EXPOSE 8082`, pero la aplicacion usa `PORT=8081` si no
se configura otra cosa. Para el comando anterior, deja `PORT=8082` en `.env`;
si omites `PORT`, publica `-p 8081:8081`.

## Descubrimiento de servicios y health checks

El proyecto incluye `spring-cloud-starter-netflix-eureka-client` y configura
`eureka.client.service-url.defaultZone` con `EUREKA_SERVER_URL`. En entornos que
usan descubrimiento, apunta esa variable al servidor Eureka, por ejemplo:
`http://localhost:8761/eureka`. Si ejecutas el servicio sin Eureka, define una
estrategia explicita para tu entorno (por ejemplo, deshabilitar el cliente con
configuracion externa) en lugar de dejar el placeholder sin resolver.

Actuator expone solamente `health` e `info`; `health` tiene probes habilitados y
`show-details=always`. Revisa esa visibilidad antes de publicar el endpoint en
redes no confiables.

## Operacion y pitfalls

- `spring.jpa.hibernate.ddl-auto=update` permite que Hibernate actualice el
  esquema automaticamente. Revisa esta configuracion antes de usar el servicio
  en produccion si necesitas migraciones controladas.
- `spring.jpa.show-sql=true` y `hibernate.format_sql=true` imprimen SQL en logs;
  ajustalos para ambientes donde el volumen o la sensibilidad de logs importen.
- `JWT_EXPIRATION_MS` debe resolverse a numero. Si falta, Spring intenta enlazar
  el literal `${JWT_EXPIRATION_MS}` y el arranque falla.
- `FRONTEND_URL` no tiene default; si falta, el contexto de Spring no arranca.
- `EUREKA_SERVER_URL` no tiene default; si falta, la configuracion de Eureka
  queda incompleta para el arranque o registro del cliente.
- Las dependencias incluyen AMQP, pero no hay listeners, publishers ni colas en
  el codigo actual. No asumas integracion con RabbitMQ hasta que exista codigo
  que la use.
- El test de contexto actual declara `jwt.secret`, pero la aplicacion usa
  `jwt.private-key`, `jwt.public-key` y `jwt.expiration-ms`; al ejecutar
  `./mvnw -B test` sin esas propiedades RSA, falla durante el arranque de
  Spring. Exporta valores JWT de prueba o ajusta las propiedades del test antes
  de usarlo como verificacion de CI.
