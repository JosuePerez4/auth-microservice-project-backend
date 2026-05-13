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

## Runbook de arranque y verificacion

1. Crea o exporta las variables de `Configuracion requerida`. No guardes claves
   reales en el repositorio; usa `.env` local o secretos del entorno.
2. Verifica que PostgreSQL acepte conexiones con `SPRING_DATASOURCE_URL`,
   `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.
3. Arranca el servicio con `./mvnw spring-boot:run` o con la imagen Docker.
4. Confirma que la documentacion publica responde:

   ```bash
   curl -i http://localhost:8082/v3/api-docs
   curl -i http://localhost:8082/swagger-ui.html
   ```

5. Registra o autentica un usuario para obtener `accessToken` y prueba una ruta
   protegida:

   ```bash
   curl -i -X POST http://localhost:8082/api/v1/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"email":"ada@example.com","password":"password-seguro"}'

   TOKEN='<accessToken devuelto por login o register>'
   curl -i http://localhost:8082/api/v1/auth/me \
     -H "Authorization: Bearer $TOKEN"
   ```

6. Si tu despliegue usa Actuator, recuerda que no existe whitelist para
   `/actuator/**` en `SecurityConfig`: cualquier endpoint de Actuator expuesto por
   Spring pasa por la regla global y requiere `Authorization: Bearer <jwt>`.

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
- Cualquier otro endpoint requiere `Authorization: Bearer <jwt>`.
- `FRONTEND_URL` configura los patrones de origen permitidos por CORS. Cuando
  haya varios origenes, separalos con comas sin espacios obligatorios:
  `https://app.example.com,https://admin.example.com`.
- CORS permite credenciales, todos los headers de entrada y expone el header
  `Authorization`.
- El preflight `OPTIONS` es publico, pero la peticion real a cualquier endpoint
  no listado como publico sigue requiriendo un Bearer valido.
- El filtro JWT solo acepta cabeceras con prefijo exacto `Bearer `. Si el token
  falta, esta vencido, usa otra clave publica o no contiene `sub`, `email` y
  `role`, la autenticacion se limpia y Spring responde con `401`.
- El claim `sub` debe ser un UUID de usuario. `GET /api/v1/auth/me` valida el
  token y luego carga ese usuario desde base de datos; si ya no existe, responde
  `401 Unauthorized`.

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

## Operacion y pitfalls

| Sintoma | Causa probable | Accion |
| --- | --- | --- |
| Fallo de arranque por `${SPRING_DATASOURCE_URL}` | No se definio la URL JDBC y no hay default. | Define `SPRING_DATASOURCE_URL` apuntando a PostgreSQL accesible. |
| Fallo de arranque por `${JWT_EXPIRATION_MS}` | El placeholder no tiene default en `application.properties`. | Define un numero en milisegundos, por ejemplo `3600000`. |
| `No se pudieron cargar las claves RSA para JWT` | Las claves no son PKCS#8/X.509, estan truncadas o el prefijo `base64:` contiene contenido invalido. | Regenera el par con los comandos de OpenSSL y carga ambas claves del mismo par. |
| Fallo de arranque por `FRONTEND_URL` | `SecurityConfig` inyecta la variable sin default. | Define al menos un origen o patron permitido, por ejemplo `http://localhost:5173`. |
| Preflight CORS funciona pero la llamada real devuelve `401` | `OPTIONS` es publico, pero el endpoint real esta protegido. | Envia `Authorization: Bearer <jwt>` y confirma que el origen esta en `FRONTEND_URL`. |
| `GET /api/v1/auth/me` devuelve `401` con token aparentemente valido | El token vencio, fue firmado con otra clave, le faltan claims obligatorios o el usuario del `sub` ya no existe. | Reautentica al usuario y verifica que el despliegue use el mismo par RSA para firmar y validar. |

Notas operativas:

- `spring.jpa.hibernate.ddl-auto=update` permite que Hibernate actualice el
  esquema automaticamente. Revisa esta configuracion antes de usar el servicio
  en produccion si necesitas migraciones controladas.
- `spring.jpa.show-sql=true` y `hibernate.format_sql=true` imprimen SQL en logs;
  ajustalos para ambientes donde el volumen o la sensibilidad de logs importen.
- Las dependencias incluyen AMQP, pero no hay listeners, publishers ni colas en
  el codigo actual. No asumas integracion con RabbitMQ hasta que exista codigo
  que la use.
- El test de contexto actual define `jwt.secret`, pero el servicio usa
  `jwt.private-key`, `jwt.public-key` y `jwt.expiration-ms`; al ejecutar
  `./mvnw -B test` sin esas propiedades RSA, falla durante el arranque de
  Spring. Exporta valores JWT de prueba o ajusta las propiedades del test antes
  de usarlo como verificacion de CI.
