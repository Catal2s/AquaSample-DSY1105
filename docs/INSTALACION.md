# Instalación en Windows

Guía para clonar el repositorio y ejecutar el backend y la app. Los comandos son para **PowerShell**.

## 1. Requisitos

| Programa | Para qué |
|---|---|
| Git | Clonar el repositorio |
| JDK 17 | Ejecutar el backend y Gradle. `JAVA_HOME` debe apuntar a él |
| Android Studio compatible con Android Gradle Plugin 9.4.1 | Compilar la app. Necesita el SDK de Android 37 |
| Emulador o teléfono con Android 7.0 (API 24) o superior | Ejecutar la app |

No hace falta instalar Maven ni Gradle, el repositorio trae sus wrappers. La primera vez se necesita internet.
El proyecto pide que Gradle use un JDK 25 (`gradle/gradle-daemon-jvm.properties`). Si no está instalado, Gradle lo descarga.

```powershell
git clone https://github.com/Catal2s/AquaSample-DSY1105.git
cd AquaSample-DSY1105
```

## 2. Levantar el backend

Hay que entrar a la carpeta `backend`. Desde la raíz no funciona, porque Maven busca ahí el `pom.xml`.

```powershell
cd backend
.\mvnw.cmd -v                # debe decir Java version: 17
.\mvnw.cmd spring-boot:run   # Ctrl+C para detenerlo
```

Si no dice 17, en esa misma terminal (con la ruta de tu JDK):

```powershell
$env:JAVA_HOME = "C:\ruta\al\jdk-17"
```

| Qué | URL |
|---|---|
| Muestras | http://localhost:8080/api/muestras |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Consola H2 | http://localhost:8080/h2-console |

En la consola H2: JDBC URL `jdbc:h2:file:./data/aquasample`, usuario `sa`, sin clave.
Si la base está vacía, el backend carga 5 muestras ficticias. La base queda en `backend/data` y las fotos en `backend/uploads`; ninguna se sube a Git.

## 3. Configurar la URL del backend en la app

La URL se lee de `local.properties`, en la raíz del proyecto. Android Studio crea ese archivo al abrir el proyecto y no se sube a Git.

### Emulador

No hay que configurar nada: se usa `http://10.0.2.2:8080/`, que es el PC visto desde el emulador.

### Teléfono físico

1. Conectar el teléfono y el PC a la misma red WiFi.
2. Ejecutar `ipconfig` en el PC y copiar la **Dirección IPv4** del adaptador WiFi.
3. Agregar en `local.properties` (con tu IP):
   ```properties
   backend.url=http://192.168.1.50:8080/
   ```
4. Levantar el backend. Si Windows pregunta, permitir Java en el Firewall para **redes privadas**. La red WiFi debe estar marcada como privada.
5. Comprobar en el navegador del teléfono: `http://192.168.1.50:8080/api/muestras`.
6. **Recompilar e instalar de nuevo la app.** La IP queda dentro del APK: si cambia, hay que repetir estos pasos.

Al compilar se genera `network_security_config.xml`, que permite HTTP solo hacia esa dirección. Para volver al emulador, borrar la línea `backend.url` y recompilar.

## 4. Ejecutar la app

Abrir la carpeta raíz en Android Studio, esperar el Sync de Gradle y presionar **Run**. También se puede compilar por terminal, desde la raíz:

```powershell
.\gradlew.bat assembleDebug
```

El APK queda en `app\build\outputs\apk\debug\app-debug.apk`. Usuarios ficticios, todos con clave `1234`: `operador1`, `operador2` y `supervisor1`.

### Permiso de red local (Android 17)

Desde Android 17 (API 37) la app pide el permiso `ACCESS_LOCAL_NETWORK` al abrirse. Hay que **permitirlo** para llegar al backend, también en el emulador.
Si se rechaza, la app sigue funcionando, pero las muestras quedan pendientes. Se puede activar después en los permisos de la app, en Ajustes de Android.
En versiones anteriores no se pide.

## 5. Pruebas

Backend (41 pruebas), desde la carpeta `backend`:

```powershell
.\mvnw.cmd test
```

App, desde la raíz:

```powershell
.\gradlew.bat testDebugUnitTest          # unitarias, no necesita emulador
.\gradlew.bat connectedDebugAndroidTest  # instrumentadas, con emulador encendido
```

> Las pruebas instrumentadas pueden desinstalar la app al terminar y borrar sus datos en ese dispositivo.

### Cobertura

```powershell
.\gradlew.bat :app:reporteCobertura
.\gradlew.bat :app:verificarCobertura    # falla si es menor a 90 %
```

> **Estas dos tareas todavía no están en `main`.** Vienen en la rama `integracion/ea4`, junto con las pruebas de Kotest y de interfaz. Cuando se mergee, el reporte quedará en `app/build/reports/jacoco/reporteCobertura/html/index.html`.

## 6. Problemas comunes

### El teléfono no conecta

Revisar en este orden:

1. ¿El backend está levantado? Abrir `http://localhost:8080/api/muestras` en el PC.
2. ¿Abre `http://IP_DEL_PC:8080/api/muestras` en el navegador del teléfono? Si no, es la red o el Firewall. Algunas WiFi no dejan que los equipos se vean; se puede usar la zona WiFi de un teléfono.
3. ¿Cambió la IP? Repetir `ipconfig`, actualizar `local.properties`, recompilar e instalar.
4. ¿Se permitió el permiso de red local? (Android 17)

### El puerto 8080 está ocupado

Ver qué proceso lo usa (el PID es el último número) y qué programa es:

```powershell
netstat -ano | findstr :8080
tasklist /FI "PID eq NUMERO"
```

Si es un backend anterior, cerrarlo con Ctrl+C en su terminal, o con `taskkill /PID NUMERO /F`.
También se puede usar otro puerto. En ese caso `backend.url` debe llevarlo y hay que recompilar la app:

```powershell
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

### La muestra queda en "Pendiente de sincronización"

No se pudo llegar al servidor o este falló. No se pierde nada: levantar el backend y usar **Sincronizar** en el Historial. Todavía no hay reintento automático.

### La muestra queda en "Error de sincronización"

El servidor la rechazó o falló su foto. El motivo aparece en el detalle de la muestra, en **Motivo**. No se reintenta sola.

| Motivo | Qué significa |
|---|---|
| "ya pertenece a otro operador" | Dos teléfonos usaron el mismo id (límite conocido). Hay que registrarla de nuevo en la app |
| "ya fue validada y no se puede editar" | El servidor tiene otra versión ya validada |
| "no pueden superar 500 caracteres" o "no puede superar 500 caracteres" | La app limita las observaciones y el comentario del supervisor a 500 caracteres desde el PR de `fix/observaciones-y-contrato`. Solo puede pasar con una versión anterior de la app: si fueron las observaciones, hay que registrarla de nuevo, más corta; si fue el comentario, sincronizar y revisarla otra vez |
| "No existe la muestra" | Ya no está en el servidor, por ejemplo porque se reinició su base |
| Mensaje sobre la foto | Los datos llegaron, pero la foto no |

Si los datos ya habían llegado y la muestra sigue en el servidor, **Sincronizar** la reemplaza por esa versión. Si no, hay que registrarla de nuevo; el supervisor puede eliminar la que quedó en error.

### La base del backend se reinicia

La base persiste al reiniciar el servidor. Si aparece vacía o con las 5 muestras iniciales:

- El backend se ejecutó desde otra carpeta. La ruta `./data` es relativa: en IntelliJ hay que abrir la carpeta `backend`, no la raíz.
- Se borró `backend/data`. Para volver a los datos iniciales a propósito, se detiene el servidor y se borra esa carpeta.

> **Cuidado:** con la base reiniciada, **Sincronizar** borra del teléfono las muestras ya sincronizadas que el servidor no tiene. Las pendientes no se borran. Conviene respaldar `backend/data` antes de una demostración.

---

## APK release firmada

1. Crear el keystore una sola vez, fuera del repositorio (Build > Generate Signed App Bundle / APK > Create new).
2. Crear `keystore.properties` en la raíz del proyecto (no se sube a Git) con `storeFile`, `storePassword`, `keyAlias` y `keyPassword`. La ruta va con barras normales.
3. Compilar: `.\gradlew.bat assembleRelease`
4. La APK queda en `app\build\outputs\apk\release\app-release.apk` (versión 1.1.0, código 2).
5. Verificar la firma con `apksigner verify --verbose --print-certs`.

El archivo `.jks` y `keystore.properties` están en `.gitignore` y nunca se suben al repositorio.

[Contrato de la API](API_CONTRATO.md) · [Backend](../backend/README.md) · [índice](README.md)
