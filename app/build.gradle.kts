plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // JaCoCo: mide la cobertura de las pruebas unitarias
    jacoco
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
