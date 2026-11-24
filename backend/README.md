# AquaSample · Backend (Spring Boot)

Microservicio de muestras para la app AquaSample. Guarda las muestras en una base de datos H2 (archivo local), aplica las reglas de revisión por rol y se documenta con Swagger.

**Tecnologías:** Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, H2, Bean Validation, springdoc-openapi (Swagger UI), JUnit 5 + Mockito.

## Cómo ejecutarlo

Requisito: **JDK 17**. No hace falta instalar Maven: el proyecto trae el Maven Wrapper (`mvnw`), que lo descarga la primera vez.

**Con IntelliJ IDEA:** File → Open → carpeta `backend` → elegir SDK 17 en Project Structure → esperar que Maven descargue las dependencias → ejecutar `AquaSampleBackendApplication`.

**En Windows (PowerShell), dentro de la carpeta `backend`:**

El wrapper usa el JDK de `JAVA_HOME`, no el `java` de la terminal. Por eso funciona aunque `java -version` muestre Java 8, siempre que `JAVA_HOME` apunte a un JDK 17.

```powershell
# 1. Ver qué Java usará el wrapper (debe decir "Java version: 17")
.\mvnw.cmd -v

# 2. Solo si no dice 17: apuntar JAVA_HOME al JDK 17 en esta terminal (Eclipse Adoptium)
$env:JAVA_HOME = (Get-ChildItem "$env:LOCALAPPDATA\Programs\Eclipse Adoptium", "$env:ProgramFiles\Eclipse Adoptium" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName

# 3. Levantar el servidor (Ctrl+C para detenerlo)
.\mvnw.cmd spring-boot:run
```

En CMD los comandos son los mismos, pero `JAVA_HOME` se define con `set "JAVA_HOME=C:\ruta\al\jdk-17"`. En Linux, macOS o Git Bash se usa `./mvnw`.

Al iniciar queda en `http://localhost:8080` con 5 muestras ficticias (las mismas de la app).

| Qué | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Consola H2 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/aquasample`, usuario `sa`, sin clave) |

Desde el emulador Android el backend se llama con `http://10.0.2.2:8080`.

## Pruebas

```powershell
.\mvnw.cmd test
```

- `MuestraServiceTest`: reglas de negocio con el repositorio simulado (Mockito): permisos por rol, cambios de estado, fechas, sincronización sin duplicados y conflicto entre operadores.
- `FotoServiceTest`: formato real de la foto, reemplazo y borrado, en una carpeta temporal.
- `MuestraControllerTest`: validaciones y formato único de error por HTTP (MockMvc).

## Límites conocidos

- **Ids repetidos:** la app genera el id como "el mayor + 1" en cada teléfono. Si dos operadores distintos usan el mismo id, el PUT responde 409. **El mismo operador en dos teléfonos no se detecta**: el segundo envío reemplaza al primero. La solución real sería un UUID generado por la app.
- **Roles:** `X-Rol` no es autenticación; cualquiera puede enviar el rol que quiera. En producción se usaría Spring Security.

## Estructura

```
backend/src/main/java/com/aquasample/backend/
├── model/        Muestra (tabla), EstadoRevision, Rol
├── dto/          JSON de entrada y salida, errores, resumen
├── repository/   MuestraRepository (JPA)
├── service/      MuestraService (reglas de negocio), FotoService
├── controller/   MuestraController (/api/muestras), ReporteController (/api/reportes)
├── exception/    Excepciones y manejador de errores
└── config/       Swagger y datos iniciales
```

El contrato completo de la API está en [`docs/API_CONTRATO.md`](../docs/API_CONTRATO.md).
