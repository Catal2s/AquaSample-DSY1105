import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // JaCoCo: mide la cobertura de las pruebas unitarias
    jacoco
}

// URL del backend. Se cambia en local.properties (backend.url=http://192.168.1.50:8080/).
// Por defecto es el PC visto desde el emulador.
val backendUrl: String = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText
    .map { texto -> Properties().apply { load(texto.reader()) }.getProperty("backend.url").orEmpty() }
    .orNull?.trim()?.ifEmpty { null }
    .let { it ?: "http://10.0.2.2:8080/" }
    .let { if (it.endsWith("/")) it else "$it/" }
val backendUri: URI = runCatching { URI(backendUrl) }.getOrElse { throw GradleException("backend.url no es válida: $backendUrl") }
if (backendUri.scheme !in setOf("http", "https") || backendUri.host == null) {
    throw GradleException("backend.url no es válida: $backendUrl (ejemplo: http://192.168.1.50:8080/)")
}

android {
    namespace = "com.example.aquasample"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.aquasample"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Registra qué líneas ejecutan las pruebas unitarias (para el reporte de JaCoCo)
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

/**
 * Genera res/xml/network_security_config.xml con el host de backend.url.
 * Android no permite HTTP por defecto y ese archivo no acepta variables ni rangos de IP,
 * así que se escribe al compilar: HTTP queda permitido solo hacia el backend, en debug y release.
 */
abstract class GenerarConfigRed : DefaultTask() {
    @get:Input
    abstract val host: Property<String>

    @get:OutputDirectory
    abstract val carpeta: DirectoryProperty

    @TaskAction
    fun generar() {
        val archivo = carpeta.file("xml/network_security_config.xml").get().asFile
        archivo.parentFile.mkdirs()
        archivo.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <network-security-config>
                <base-config cleartextTrafficPermitted="false" />
                <domain-config cleartextTrafficPermitted="true">
                    <domain includeSubdomains="false">${host.get()}</domain>
                </domain-config>
            </network-security-config>
            """.trimIndent()
        )
    }
}

val generarConfigRed = tasks.register<GenerarConfigRed>("generarConfigRed") {
    host.set(backendUri.host)
}

androidComponents {
    onVariants { variante ->
        variante.sources.res?.addGeneratedSourceDirectory(generarConfigRed, GenerarConfigRed::carpeta)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Pruebas unitarias (Guía 15): Kotest + JUnit 5 + MockK + corrutinas
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    // Permite que las pruebas antiguas de JUnit 4 sigan corriendo junto a JUnit 5
    testRuntimeOnly(libs.junit.vintage.engine)
}

// Obligatorio para usar JUnit 5 (y Kotest) en las pruebas unitarias
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}


/*
 * Cobertura con JaCoCo. Se mide la lógica evaluable: ViewModels y capa de datos (Repository).
 * Se excluyen las pantallas Compose (se prueban con Compose UI Test), el tema, la navegación
 * y las clases que solo funcionan dentro de Android (Room generado, base de datos y archivos de fotos).
 *
 *   ./gradlew :app:reporteCobertura   → app/build/reports/jacoco/reporteCobertura/html/index.html
 *   ./gradlew :app:verificarCobertura → falla si la cobertura de líneas baja del 90%
 */
val clasesEvaluables = fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
    include("com/example/aquasample/viewmodel/**", "com/example/aquasample/data/**")
    exclude(
        "com/example/aquasample/data/FotoStorage*",
        // Dibuja con PdfDocument (solo existe en Android): se prueba en GeneradorPdfTest (androidTest)
        "com/example/aquasample/data/GeneradorPdf*",
        "com/example/aquasample/data/local/AppDatabase*",
        "**/*_Impl*"
    )
}
val datosDeEjecucion = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
}

tasks.register<JacocoReport>("reporteCobertura") {
    group = "verification"
    description = "Ejecuta las pruebas unitarias y genera el reporte de cobertura (HTML y XML)."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(clasesEvaluables)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(datosDeEjecucion)
    reports {
        html.required = true
        xml.required = true
    }
}

tasks.register<JacocoCoverageVerification>("verificarCobertura") {
    group = "verification"
    description = "Falla si la cobertura de líneas de ViewModel y Repository es menor al 90%."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(clasesEvaluables)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(datosDeEjecucion)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}
