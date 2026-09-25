# Pagame - Aplicación Android Nativa (Jetpack Compose & Material 3)

Aplicación nativa para Android desarrollada en **Kotlin** con **Jetpack Compose**, **Material 3** y arquitectura **MVVM**, diseñada para dividir cuentas y gastos entre amigos en Guatemala (con soporte de moneda Quetzal `Q`).

---

## 🛠️ Especificaciones Técnicas

- **Lenguaje:** Kotlin 2.1.10 (Versión estable)
- **compileSdk:** 37
- **targetSdk:** 37 (Android 17)
- **minSdk:** 24 (Android 7.0 Nougat)
- **Android Gradle Plugin (AGP):** 9.4.0 (compatible con API 37)
- **Gradle:** 9.6 (versión estable configurada en `gradle-wrapper.properties`)
- **JDK:** Java 17
- **UI Toolkit:** Jetpack Compose + Material 3 + Compose Navigation
- **Carga de Imágenes:** Coil para Compose (`io.coil-kt:coil-compose`)
- **Gestión de Dependencias:** Version Catalog centralizado en `gradle/libs.versions.toml`
- **Arquitectura:** MVVM (Model-View-ViewModel) con `StateFlow` reactivo

---

## 📱 Pantallas Implementadas y Fidelidad de Diseño

1. **Iniciar Sesión (`LoginScreen.kt` & `LoginViewModel.kt`)**
   - Logo squircle en degradado coral (`#B83214` a `#E04E28`) con ícono de recibo Material 3.
   - Píldora de estado "Disponible en Guatemala (Q)".
   - Campos estilizados para correo/teléfono y contraseña con botón para alternar visibilidad.
   - Checkbox "Recordar mi cuenta" e indicador de "Sesión segura".
   - Botón primario coral con elevación tonal y flecha indicadora.
   - Tarjeta informativa de cuenta de demostración activa (Mateo González).

2. **Crear Cuenta (`RegisterScreen.kt` & `RegisterViewModel.kt`)**
   - Botón de navegación circular de retorno.
   - Título con acento de punto de color corporativo.
   - Validación en tiempo real de coincidencia de contraseñas con chip verde "✓ Coinciden".
   - Checkbox de aceptación de Términos de servicio y Políticas de privacidad.
   - Botón de acción principal "Crear cuenta ->".

3. **Eventos (`EventsScreen.kt` & `EventsViewModel.kt`)**
   - Encabezado con contador interactivo de eventos y selector de moneda.
   - Botón para alternar a "Estado vacío" (Empty state) con diseño amigable e ilustración.
   - Lista de 4 eventos preconfigurados con datos realistas:
     - **Pizza & Birra** (`Q12.400` total, chip verde "Te deben Q320").
     - **Viaje a Antigua** (`Q3.850` total, chip coral "Debes Q180").
     - **Almuerzo Universidad** (`Q420` total, chip neutro "Al día").
     - **Noche de Tacos & Juegos** (`Q1.150` total, chip verde "Liquidado").
   - Pila de avatares superpuestos de participantes (`MG`, `SM`, `VC`, `+3`).
   - Botón de Acción Flotante (FAB) extendido "+ Crearevento" con modal interactivo.

4. **Detalle del Evento (`EventDetailScreen.kt` & `EventDetailViewModel.kt`)**
   - Cabecera con botón de retroceso y total general destacado.
   - **Segmented Control M3:** alternador entre pestañas **Gastos (2)** y **Balance**.
   - **Pestaña Gastos:**
     - Tarjeta "Total Gastado" (`Q12.400`) con degradado profundo.
     - Tarjeta "Participantes" (6 personas, promedio `Q2.066 / p`).
     - Desglose cronológico de gastos: Pizzas napolitanas (`Q4.500`, Santi) y Cervezas y bebidas (`Q7.900`, Vale).
     - FAB "+ Agregar gasto" con diálogo para registrar nuevos conceptos.
   - **Pestaña Balance:**
     - Tarjeta de balance personal ("Tu balance en este evento Q0.00", desglose "Te deben Q1.500" y "Debes Q1.500").
     - Grid de 4 tarjetas de participantes con indicadores de balance y tendencia.
     - Tarjeta de **Pago sugerido optimizado** (Lu → Santi: `Q1.500`).
     - Botón de acción "Liquidar cuentas del evento".

5. **Amigos (`FriendsScreen.kt` & `FriendsViewModel.kt`)**
   - Tarjeta destacada "TU CÓDIGO DE AMIGO" (`PG-7489`) con botón para copiar al portapapeles.
   - Alternador de estado vacío.
   - Lista de amigos con iniciales, cantidad de eventos compartidos y estado ("Al día" o código de amigo).
   - FAB "+ Agregar amigo" con modal de registro.

6. **Opciones (`OptionsScreen.kt` & `OptionsViewModel.kt`)**
   - Perfil de usuario con avatar circular con degradado, nombre, correo y chip "Cuenta verificada".
   - Tarjeta de código de amigo con botón de copiado.
   - Preferencias generales con navegación y controles:
     - **Moneda:** Quetzal (Q), Dólar ($ USD), Euro (€).
     - **Tema:** Claro, Oscuro, Automático.
     - **Notificaciones:** Switch interactivo Material 3.
     - **Ayuda y soporte:** FAQ y chat.
   - Botón estilizado de advertencia para "Cerrar sesión" con diálogo de confirmación.
   - Enlace discreto "Eliminar cuenta" con diálogo modal de advertencia de seguridad.

---

## 📂 Estructura del Proyecto

```
android/
├── gradle/
│   ├── libs.versions.toml             # Catálogo centralizado de versiones y librerías
│   └── wrapper/
│       └── gradle-wrapper.properties  # Gradle 9.6
├── build.gradle.kts                   # Configuración Gradle raíz
├── settings.gradle.kts                # Repositorios y módulos
├── gradle.properties                  # JVM & flags de compilación AndroidX
├── app/
│   ├── build.gradle.kts               # compileSdk 37, AGP 9.4.0, Compose, Kotlin 2.1.10
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── res/
│           │   └── values/
│           │       ├── strings.xml
│           │       ├── colors.xml
│           │       └── themes.xml
│           └── java/gt/pagame/app/
│               ├── MainActivity.kt
│               ├── data/
│               │   ├── model/
│               │   │   └── Models.kt
│               │   └── repository/
│               │       └── PagameRepository.kt
│               └── ui/
│                   ├── components/
│                   │   └── CommonComponents.kt
│                   ├── navigation/
│                   │   ├── Screen.kt
│                   │   └── NavGraph.kt
│                   ├── screens/
│                   │   ├── login/
│                   │   │   ├── LoginScreen.kt
│                   │   │   └── LoginViewModel.kt
│                   │   ├── register/
│                   │   │   ├── RegisterScreen.kt
│                   │   │   └── RegisterViewModel.kt
│                   │   ├── events/
│                   │   │   ├── EventsScreen.kt
│                   │   │   └── EventsViewModel.kt
│                   │   ├── eventdetail/
│                   │   │   ├── EventDetailScreen.kt
│                   │   │   └── EventDetailViewModel.kt
│                   │   ├── friends/
│                   │   │   ├── FriendsScreen.kt
│                   │   │   └── FriendsViewModel.kt
│                   │   └── options/
│                   │       ├── OptionsScreen.kt
│                   │       └── OptionsViewModel.kt
│                   └── theme/
│                       ├── Color.kt
│                       ├── Theme.kt
│                       └── Type.kt
```

---

## 🚀 Cómo Ejecutar en Android Studio

1. Abre **Android Studio** (Ladybug / Koala o versión compatible con API 37 / AGP 9.4).
2. Selecciona **Open** y navega a la carpeta `android/` de este proyecto.
3. Asegúrate de configurar **JDK 17** en `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`.
4. Deja que Gradle sincronice las dependencias del catálogo `libs.versions.toml`.
5. Selecciona un dispositivo virtual (Emulador Pixel con Android 17 / API 37) o dispositivo físico conectado.
6. Presiona **Run** (`Shift + F10`).
7. Para ver las vistas previas de Compose, abre cualquiera de los archivos `*Screen.kt` y activa la vista **Split** o **Design**.
