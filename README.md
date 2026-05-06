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
- **Operacion:** Spring Boot Actuator esta presente para endpoints operativos,
  pero la configuracion de seguridad actual protege cualquier ruta que no sea
  publica de forma explicita.
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
| `PORT` | Puerto HTTP | Default: `8082`. |
| `SPRING_DATASOURCE_URL` | JDBC de PostgreSQL | Requerida en runtime. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de BD | Default vacio. |
| `SPRING_DATASOURCE_PASSWORD` | Password de BD | Default vacio. |
| `JWT_PRIVATE_KEY` | Clave RSA privada para firmar JWT | Requerida. Debe ser PKCS#8. |
| `JWT_PUBLIC_KEY` | Clave RSA publica para validar JWT | Requerida. Debe ser X.509. |
| `JWT_EXPIRATION_MS` | Duracion del token en milisegundos | Requerida por el placeholder actual. |
| `FRONTEND_URL` | Origenes CORS permitidos | Requerida; acepta lista separada por comas. |

Ejemplo local sin secretos reales:

```properties
PORT=8082
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth
SPRING_DATASOURCE_USERNAME=auth_user
SPRING_DATASOURCE_PASSWORD=change-me
JWT_EXPIRATION_MS=3600000
FRONTEND_URL=http://localhost:5173,http://localhost:3000
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

Swagger UI queda disponible en:

```text
http://localhost:8082/swagger-ui.html
```

El documento OpenAPI se expone en:

```text
http://localhost:8082/v3/api-docs
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
  "city": "Bogota",
  "role": "AUTHOR"
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
- `role` es opcional; si no se envia, se asigna `AUTHOR`. El endpoint publico
  acepta cualquier valor definido en el enum, por lo que los clientes no deben
  enviar roles elevados salvo que el flujo de producto lo requiera.
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
- Cualquier otro endpoint requiere `Authorization: Bearer <jwt>`. Esto incluye
  las rutas de Actuator que Spring Boot exponga, como `/actuator` o
  `/actuator/health`, porque no aparecen en la lista publica de
  `SecurityConfig`.
- `FRONTEND_URL` configura los patrones de origen permitidos por CORS. Cuando
  haya varios origenes, separalos con comas sin espacios obligatorios:
  `https://app.example.com,https://admin.example.com`.
- CORS permite credenciales, todos los headers de entrada y expone el header
  `Authorization`.

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
- `403 Forbidden`: autenticado sin permisos suficientes. La superficie actual
  solo exige autenticacion, no reglas por rol, pero existe un handler JSON para
  denegaciones de acceso futuras o de filtros de Spring Security.
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

## Operacion y pitfalls

- `spring.jpa.hibernate.ddl-auto=update` permite que Hibernate actualice el
  esquema automaticamente. Revisa esta configuracion antes de usar el servicio
  en produccion si necesitas migraciones controladas.
- `spring.jpa.show-sql=true` y `hibernate.format_sql=true` imprimen SQL en logs;
  ajustalos para ambientes donde el volumen o la sensibilidad de logs importen.
- `JWT_EXPIRATION_MS` debe resolverse a numero. Si falta, Spring intenta enlazar
  el literal `${JWT_EXPIRATION_MS}` y el arranque falla.
- `FRONTEND_URL` no tiene default; si falta, el contexto de Spring no arranca.
- Si faltan las claves JWT, `JwtTokenService` detiene el arranque. El mensaje de
  excepcion menciona `AUTH_JWT_PRIVATE_KEY` y `AUTH_JWT_PUBLIC_KEY`, pero los
  nombres que enlaza `application.properties` son `JWT_PRIVATE_KEY` y
  `JWT_PUBLIC_KEY`.
- Actuator esta en las dependencias, pero no hay propiedades `management.*` que
  cambien su exposicion. Verifica en tu ambiente que el endpoint requerido este
  expuesto y llama las rutas operativas con JWT salvo que se agreguen a la lista
  publica de seguridad.
- Las dependencias incluyen AMQP, pero no hay listeners, publishers ni colas en
  el codigo actual. No asumas integracion con RabbitMQ hasta que exista codigo
  que la use.
- El test de contexto actual define `jwt.secret`, pero el servicio enlaza
  `jwt.private-key`, `jwt.public-key` y `jwt.expiration-ms`. Al ejecutar
  `./mvnw -B test` sin esas propiedades reales, el contexto falla durante el
  arranque de Spring. Exporta valores RSA de prueba o ajusta las propiedades del
  test antes de usarlo como verificacion de CI.
