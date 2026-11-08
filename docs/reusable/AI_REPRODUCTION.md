# Reproducir y adaptar el proyecto con otra IA

Esta entrega contiene **código fuente, recursos, herramientas y documentación**, no una APK. Puede utilizarse con un asistente de programación que tenga acceso autorizado a la carpeta extraída; no requiere una cuenta ni una API de IA dentro del dashboard.

## Primeros pasos sin conectar el estéreo

1. Extrae el ZIP completo y conserva su estructura. La raíz del proyecto contiene README.md, AGENTS.md, settings.gradle, app/ y gradlew. No copies solo MainActivity.java: depende de las demás clases y recursos.
2. Si existe SOURCE_MANIFEST.json, compara sus rutas, tamaños y SHA-256 con los archivos extraídos. La lista identifica esta entrega; un checksum verifica integridad, no sustituye una firma digital o la confianza en quien lo entrega.
3. Da acceso al asistente únicamente a esa carpeta o adjunta el ZIP si admite archivos. Una IA sin herramientas locales puede proponer cambios, pero no puede afirmar que editó, compiló o probó el proyecto en tu ordenador.
4. Usa el [prompt inicial](../../prompts/00-replicate-source.md). Pide que lea expresamente AGENTS.md aunque no lo cargue automáticamente.
5. Empieza con auditoría local y plan. No necesitas encender el vehículo para entender el código, cambiar documentación o proponer una adaptación.

## Orden de lectura para la IA

| Archivo | Para qué sirve |
|---|---|
| README.md y AGENTS.md | Funciones, alcance, permisos y restricciones |
| docs/reusable/BASELINE.md | Versiones y evidencia histórica de la base |
| docs/reusable/ARCHITECTURE.md | Clases actuales y fronteras propuestas; no confundir interfaces propuestas con código existente |
| docs/reusable/GENERIC_VEHICLE.md | Vehículo genérico Canvas y diferencias de la copia pública |
| docs/reusable/CUSTOMIZATION.md | Puntos de personalización y funciones a conservar |
| docs/reusable/DEVICE_PROFILE.md y WIRELESS_ADB.md | Diagnóstico opcional y limitado del equipo del nuevo usuario |
| docs/reusable/COMPATIBILITY.md y VALIDATION.md | Compatibilidad y límites de pruebas anteriores |
| LICENSE, NOTICE y THIRD_PARTY_NOTICES.md | Apache-2.0, Copyright 2026 Luis Zapien y condiciones de componentes externos |

## Qué se reproduce y qué depende del equipo

La base pública conserva el cockpit Java/Views/Canvas, velocímetro GPS, brújula de rumbo GPS, SUV genérico, matrícula editable, temas, reloj, clima, multimedia, SOURCE y cinco accesos. Java 17, Gradle Wrapper 9.6.0, Android Gradle Plugin 8.7.3, compileSdk 35, minSdk 26 y targetSdk 28 son los valores de la base; no actualizarlos automáticamente.

El equipo histórico FYT no representa el hardware del lector. profiles/examples/fyt-uis7862.json es un ejemplo sanitizado, no una detección actual. GPS y sus permisos, resolución, apps instaladas, MediaSession, ADB y restricciones de arranque se verifican por dispositivo. No hay integración activa de volante, firmware Binder FYT, CANBUS ni temperatura exterior física. Sin dato fiable, conservar -- o el estado no disponible; no inventar lecturas.

Si el usuario autoriza un diagnóstico real, debe indicar el dispositivo y endpoint actuales, habilitar ADB por los mecanismos permitidos por el fabricante y aceptar la autorización correspondiente. La IA debe usar las herramientas de consulta documentadas y un objetivo explícito. No necesita root ni debe habilitar ADB por mecanismos ocultos. No compartir perfiles privados sin revisar su contenido.

## Plan antes de adaptar

La IA debe identificar clases y recursos afectados, respaldar o registrar una referencia verificable, separar hechos verificados/detectados/desconocidos y presentar un cambio pequeño. Conservar GPS, multimedia, clima, reloj, temas, dock y App Drawer. No convertir todos los proveedores en módulos nuevos solo para reutilizar el proyecto.

Al solicitar una modificación, especificar objetivo, pantalla/resolución cuando se conozcan y funciones que deben conservarse. Pedir un diff y documentar lo probado y lo pendiente. Una adaptación visual a otra marca no demuestra compatibilidad con sus sensores.

## Compilación futura, solo si el nuevo usuario la pide

Esta entrega no ejecuta Gradle ni genera, firma o distribuye APK. Las [instrucciones de herramientas y compilación](BUILD_SIGNING.md), [inicio rápido](QUICKSTART.md), pruebas y Wrapper se incluyen para que el lector pueda reproducirla posteriormente bajo su control. SDK, JDK y dependencias de compilación no están empaquetados: la primera compilación puede necesitar descargas y licencias oficiales.

Los resultados existentes corresponden a revisiones anteriores en macOS, no a una compilación del ZIP en todos los sistemas. Windows/Linux y otros estéreos requieren sus propias pruebas. Crear una APK, instalarla, modificar HOME, generar una clave o publicar son acciones separadas; no ejecutarlas al abrir o analizar el proyecto.

## Pendientes que debe conocer cualquier IA receptora

- La atribución visible de Open-Meteo junto al widget sigue pendiente antes de distribuir una APK con clima. Los avisos en README/assets no sustituyen ese enlace.
- La firma pública no está configurada. No hace falta crear una clave para compartir estas fuentes.
- No se garantizan funciones de NavRadio+, Amazon Music o YouTube Music sin verificar las interfaces reales de las versiones instaladas.
- El vehículo y el valor de matrícula públicos son genéricos; no restaurar archivos o personalizaciones del proyecto privado original.
- Mantener LICENSE/NOTICE y los avisos de terceros en redistribuciones y registrar archivos modificados conforme a LICENSE.
