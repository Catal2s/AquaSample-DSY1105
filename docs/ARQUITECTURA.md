# Arquitectura de AquaSample

Diagramas de la app Android, el backend y la API externa. Describen el código de `main` al 24-11-2026.

- [1. Arquitectura general](#1-arquitectura-general)
- [2. Capas MVVM de la app](#2-capas-mvvm-de-la-app)
- [3. Secuencia de la sincronización](#3-secuencia-de-la-sincronización)
- [4. Estados de la revisión](#4-estados-de-la-revisión)
- [5. Estados de la sincronización](#5-estados-de-la-sincronización)
- [6. Modelo de datos](#6-modelo-de-datos)

## 1. Arquitectura general

```mermaid
flowchart TB
    subgraph APP["App Android (Kotlin)"]
        UI["Pantallas Compose"] --> VM["ViewModels"]
        VM --> REPO["MuestraRepository"]
        VM --> REPOM["CondicionesMarinasRepositoryOpenMeteo"]
        REPO --> ROOM[("Room: aquasample.db")]
        REPO --> SYNC["SincronizadorMuestras"]
        SYNC --> ROOM
        SYNC --> CB["ClienteBackend (Retrofit)"]
        REPOM --> CM["ClienteOpenMeteo (Retrofit)"]
    end

    subgraph BACK["Backend Spring Boot, puerto 8080"]
        CTRL["MuestraController y ReporteController"] --> SERV["MuestraService y FotoService"]
        SERV --> JPA["MuestraRepository (JPA)"]
        JPA --> H2[("H2: data/aquasample")]
        SERV --> FOTOS[/"Carpeta uploads"/]
    end

    CB <-->|"HTTP y JSON, header X-Rol al escribir"| CTRL
    CM <-->|"HTTPS y JSON"| OM["Open-Meteo Marine"]
```

La app no necesita internet para registrar muestras: todo se guarda primero en Room y después se envía al backend.
El backend está separado en capas: el Controller recibe la petición, el Service aplica las reglas y el Repository guarda en H2.
Hay dos clientes Retrofit independientes, uno para nuestro backend y otro para Open-Meteo. Si uno falla, el otro sigue funcionando.
Las condiciones marinas se consultan en el momento y no se guardan en Room.

## 2. Capas MVVM de la app

```mermaid
flowchart TB
    subgraph VIEW["View: ui"]
        P1["LoginScreen"]
        P2["InicioScreen, NuevaMuestraScreen, ConteoScreen, ResumenScreen, BandejaSupervisorScreen, DetalleMuestraScreen, DashboardScreen"]
        P3["HistorialScreen"]
        C1["BotonSincronizar"]
        C2["TarjetaCondicionesMarinas"]
        P3 --> C1
        P2 -->|"solo el detalle"| C2
    end

    subgraph VMS["ViewModel: viewmodel"]
        V1["LoginViewModel"]
        V2["MuestraViewModel"]
        V3["HistorialViewModel"]
        V4["SincronizacionViewModel"]
        V5["CondicionesMarinasViewModel"]
    end

    subgraph DATA["Model: data y model"]
        DF["DatosFicticios"]
        MR["MuestraRepository"]
        CR["CondicionesMarinasRepository"]
        SM["SincronizadorMuestras"]
        DAO["MuestraDao"]
        SDAO["MuestraSyncDao"]
        DB[("AppDatabase")]
        API1["AquaSampleApi"]
        API2["OpenMeteoMarineApi"]
    end

    P1 --> V1
    P2 --> V2
    P3 --> V3
    C1 --> V4
    C2 --> V5
    V1 --> DF
    V2 --> MR
    V3 --> MR
    V4 --> MR
    V5 --> CR
    MR --> DAO
    MR --> SM
    SM --> SDAO
    SM --> API1
    CR --> API2
    DAO --> DB
    SDAO --> DB
```

Cada pantalla observa el estado de su ViewModel con `StateFlow` y le avisa lo que hizo el usuario. La pantalla no llama a Room ni a Retrofit.
El ViewModel valida y le pide los datos al repositorio. El repositorio decide de dónde salen: Room o internet.
El login usa los usuarios de `DatosFicticios`, por eso no pasa por el backend.
Para no llenar el diagrama se omitió que varias pantallas también usan `LoginViewModel` para saber quién inició sesión.

## 3. Secuencia de la sincronización

```mermaid
sequenceDiagram
    actor Op as Operador
    participant VM as MuestraViewModel
    participant Repo as MuestraRepository
    participant Room
    participant Sync as SincronizadorMuestras
    participant API as Backend

    Op->>VM: Enviar a revisión
    VM->>Repo: guardar(muestra)
    Repo->>Room: insertar con estadoSync PENDIENTE
    Note over Room: La muestra ya aparece en la app
    Repo->>Sync: subir()
    Sync->>Room: pendientes()

    alt Hay conexión con el backend
        opt datosPendientes
            Sync->>API: PUT /api/muestras/{id} con X-Rol OPERADOR
            API-->>Sync: 200
            Sync->>Room: datosEnviados(id)
        end
        opt fotoPendiente
            Sync->>API: POST /api/muestras/{id}/foto con X-Rol OPERADOR
            API-->>Sync: 200
            Sync->>Room: fotoProcesada(id)
        end
        loop Cada fila de cambios_estado, en orden
            Sync->>API: PATCH /api/muestras/{id}/estado con X-Rol de quien lo hizo
            API-->>Sync: 200
            Sync->>Room: borrarCambio(id del cambio)
        end
        Sync->>Room: marcarSincronizada(id), queda SINCRONIZADA
    else Sin conexión (IOException)
        Sync-->>Repo: sinConexion, se detiene el envío
        Note over Room: Sigue PENDIENTE, no se pierde nada
        Op->>Repo: Más tarde, botón Sincronizar (vía SincronizacionViewModel)
        Repo->>Sync: subirYBajar()
    end
```

Una muestra nueva usa el PUT (con el id de la app, para no duplicar al reintentar) y, si tiene foto, el POST. El PATCH se usa cuando alguien la revisa o la marca como corregida.
Un error 5xx, 408 o 429 también la deja PENDIENTE. Un rechazo (400, 403, 404 o 409) la deja en ERROR, salvo que el 409 sea de un envío que ya había llegado.
El botón Sincronizar primero sube y después baja `GET /api/muestras`, sin pisar los cambios locales pendientes. Si se elimina una muestra, se envía un DELETE.
Hoy el envío se intenta al guardar, revisar o eliminar, y con el botón. Todavía no hay WorkManager que reintente solo.

## 4. Estados de la revisión

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE : OPERADOR registra
    PENDIENTE --> VALIDADO : SUPERVISOR aprueba
    PENDIENTE --> OBSERVADO : SUPERVISOR pide corrección, con comentario
    OBSERVADO --> CORREGIDO : OPERADOR marca corregida
    CORREGIDO --> VALIDADO : SUPERVISOR aprueba
    CORREGIDO --> OBSERVADO : SUPERVISOR pide otra corrección
    VALIDADO --> [*]
```

El camino con corrección es PENDIENTE, OBSERVADO, CORREGIDO y VALIDADO, pero el supervisor también puede aprobar directo.
El backend revisa el rol del header `X-Rol`: si no corresponde responde 403, y si el cambio no es válido desde el estado actual responde 409.
Una muestra VALIDADO ya no se puede editar ni cambiar de estado. Pedir el estado que ya tiene, con el rol correcto, responde 200 sin cambios.
El rol ANALISTA existe en el backend y solo consulta. La app tiene solamente OPERADOR y SUPERVISOR.

## 5. Estados de la sincronización

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE : muestra nueva
    [*] --> SINCRONIZADA : muestra de ejemplo o bajada del servidor
    PENDIENTE --> SINCRONIZADA : se envió todo
    PENDIENTE --> ERROR : rechazo
    SINCRONIZADA --> PENDIENTE : cambio local
    ERROR --> PENDIENTE : cambio local
    ERROR --> SINCRONIZADA : al bajar del servidor
    note right of PENDIENTE : Sin conexión, 5xx, 408 o 429, sigue PENDIENTE
```

PENDIENTE significa que falta enviar algo. Un cambio local es revisar, corregir o eliminar la muestra.
ERROR significa que el servidor rechazó el envío o no se pudo leer la foto (el motivo queda en `mensajeSync`). No se reintenta sola, porque daría lo mismo.
Al bajar, una muestra en ERROR cuyos datos ya habían llegado se reemplaza por la del servidor. Si nunca llegaron, se conserva la copia local.
Al bajar también se borran las SINCRONIZADA que ya no están en el servidor, salvo que el servidor venga sin ninguna muestra.

## 6. Modelo de datos

```mermaid
erDiagram
    ROOM_MUESTRAS ||--o{ ROOM_CAMBIOS_ESTADO : "muestraId (sin clave foránea)"
    ROOM_MUESTRAS |o--o| BACKEND_MUESTRAS : "mismo id, por la API"

    ROOM_MUESTRAS {
        Int id PK
        String centroId
        String trenId
        String lineaId
        String fecha
        String hora
        Double tramoMetros
        String operadorId
        Int individuos
        Double calibrePromedioMm "opcional"
        String observaciones
        String estado
        String comentarioSupervisor "opcional"
        String fotoUri "opcional"
        Boolean asistidoPorFoto "v2"
        Int marcasRegistradas "v2"
        Int ajusteManual "v2"
        String estadoSync "v2"
        Boolean datosPendientes "v2, falta el PUT"
        Boolean fotoPendiente "v2, falta la foto"
        Boolean enServidor "v2"
        String eliminadoPor "v2, opcional, falta el DELETE"
        String mensajeSync "v2, opcional, motivo del error"
    }

    ROOM_CAMBIOS_ESTADO {
        Long id PK "autogenerado"
        Int muestraId
        String estado
        String comentario "opcional"
        String rol
    }

    BACKEND_MUESTRAS {
        Integer id PK "lo envía la app"
        String centroId
        String trenId
        String lineaId
        String fecha
        String hora
        double tramoMetros
        String operadorId
        int individuos
        Double calibrePromedioMm "opcional"
        boolean asistidoPorFoto
        int marcasRegistradas
        int ajusteManual
        String observaciones
        EstadoRevision estado
        String comentarioSupervisor "opcional"
        String fotoNombre "opcional"
        String fotoTipo "opcional"
        Long fotoTamanoBytes "opcional"
        LocalDateTime creadoEn
        LocalDateTime actualizadoEn
    }
```

En Room las tablas se llaman `muestras` y `cambios_estado` (entidades `MuestraEntity` y `CambioEstadoEntity`). En el backend hay una sola tabla, `muestras`.
Son dos bases distintas: no hay clave foránea entre ellas, se relacionan por el mismo `id`. Los nombres son los del código; en H2 las columnas quedan como `CENTRO_ID`.
`cambios_estado` es una lista de PATCH por enviar. Es una tabla aparte porque sin internet se pueden acumular varios y hay que mandarlos en orden.
Room está en la versión 2. `MIGRACION_1_2` agrega las columnas marcadas "v2" y la tabla `cambios_estado` sin borrar muestras. La imagen no está en ninguna base, solo su ruta o nombre.

---

Más detalle: [contrato de la API](API_CONTRATO.md) · [API externa](API_EXTERNA.md) · [instalación](INSTALACION.md) · [índice](README.md)
