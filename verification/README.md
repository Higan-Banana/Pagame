# Pruebas de Pagame

Esta carpeta contiene herramientas de desarrollo; no forma parte del APK ni carga datos automáticamente en Firebase.

## Archivos

- rules-model.test.mjs: 44 escenarios de regresión que leen las reglas actuales.
- rules-model.mjs: intérprete local de una parte del lenguaje de reglas. No certifica compilación oficial, presupuestos de evaluación ni permisos desplegados.
- fixtures.mjs y reference-domain.cjs: preparación de escenarios y referencia de cálculos.
- rules-cases.json: escenarios regenerados por las pruebas locales y utilizados por la suite oficial.
- emulator.test.mjs: pruebas mediante la biblioteca oficial de Firebase. Requiere un emulador en localhost y usa demo-pagame-spark; rechaza otros destinos.

## Ejecución

Desde esta carpeta, las pruebas locales no necesitan dependencias instaladas:

~~~sh
npm test
~~~

Para la suite del emulador oficial, con Node.js y Java compatibles con las herramientas instaladas:

~~~sh
npm install
npm run test:emulator
~~~

## Cobertura y estado

Los escenarios incluyen perfiles, permisos, amistad recíproca, solicitudes, eliminación de amistad desde ambas cuentas, participantes, reparto en centavos, revisiones, gastos y confirmación de pagos. También cubren altas de gastos por otros miembros, autoría conservada y eventos/gastos sin emoji. Los metadatos históricos de emoji no se validan; la interfaz muestra iniciales.

Última ejecución funcional documentada, 19 de octubre de 2026: pasaron los 41 escenarios locales y 27 pruebas Kotlin del modelo/estado de detalle con compilador local y JUnit. No se ejecutó la suite oficial ni se publicó nada en Firebase.

La compilación completa de Android se detuvo antes de compilar con Unable to establish loopback connection de Gradle. Las pruebas Kotlin mencionadas no equivalen a compilar el APK, probar Compose o validar SharedPreferences en Android. Quedan pendientes las pruebas visuales, los borradores en dispositivo y la comprobación con el emulador oficial.

Consultar el [README principal](../README.md) para configuración y funciones, y [CONTEXTO_CHAT.txt](../CONTEXTO_CHAT.txt) para las decisiones del proyecto.

Actualización del 6 de noviembre de 2026: pasaron los 43 tests locales de reglas,
incluida autenticación reciente para eliminar/anonimizar cuenta. Las sesiones
antiguas no pueden borrar preferencias ni códigos. El intérprete usa un tiempo
fijo y la suite oficial transforma la antigüedad a auth_time del reloj actual.
No se ejecutó la suite oficial. También pasaron 34 pruebas Kotlin con compilación
local de todos los fuentes de producción; no se generó un APK actualizado.

El máximo vigente por gasto es 100,000.00 (10,000,000 centavos). Los 44 tests locales incluyen el máximo exacto y el rechazo de 100,000.01.

La restricción propia de cinco minutos fue retirada por solicitud del usuario. Los tests actuales comprueban sesiones antiguas sin perder controles de propietario y anonimización. El historial anterior de resultados no describe la regla vigente de sesión.
