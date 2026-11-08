# Auditoría de la base funcional

Fecha: 5 de noviembre de 2026. Referencia: **1.1.2 / versionCode 21**, paquete com.fytsmart.dashboard. No es una certificación universal.

| Elemento | Configuración |
|---|---|
| Lenguaje / UI | Java 17 · Views y Canvas · módulo app |
| Gradle Wrapper / AGP | 9.6.0 / 8.7.3 |
| compileSdk / minSdk / targetSdk | 35 / 26 / 28 |
| Runtime | APIs del sistema; sin bibliotecas externas declaradas |
| Equipo de referencia | FYT UIS7862, ro.fyt.platform=6315, Android 10/API 29 |
| Pantalla | 1280×720, horizontal; densidad no generalizada |
| Código auditado | 46 clases Java de producción en la copia pública (incluye GenericVehicleArtwork); 14 suites Java en host |
| Compilación previa | assembleDebug y assembleDebugAndroidTest correctos |
| Validación gráfica | 256 comprobaciones Canvas de adornos, capturas reales en ambos modos |
| Release | Firma y optimización de producción pendientes |

La carpeta original no tenía Git, AGENTS.md ni .gitignore raíz. Su README describía v0.1 y contradecía las funciones actuales. La copia anterior se conserva solo en el archivo privado. El respaldo completo previo a esta preparación pasó unzip -t, CRC y SHA-256; ruta y checksum figuran en el informe privado.

La aplicación original se conserva byte a byte. La copia pública cambia el valor inicial de matrícula, sus fixtures/textos y la severidad explícita de ExpiredTargetSdkVersion; la posterior sustitución gráfica genérica se documenta en GENERIC_VEHICLE.md. El resto del comportamiento Android se conserva. No se importa historial privado ni evidencias personales.

## Pruebas y límites

La base se instaló en el equipo de referencia conservando datos y HOME. Se verificó inicio y Navidad/Halloween claro/oscuro sin fatal/ANR durante una observación breve. Eso no prueba conducción prolongada, suspensión en todos los firmwares, CANBUS o controles multimedia en otros dispositivos. En esa comprobación el GPS esperaba señal y no se mostraba sesión multimedia: no se presenta como evidencia de recepción GPS o reproducción.

Lint conserva un error ExpiredTargetSdkVersion por targetSdk 28 y advertencias anteriores. No se actualizan SDK/dependencias ni se ocultan incidencias. Build correcto no significa Lint correcto o publicación en Google Play aprobada.

## Acoplamientos

- MainActivity compone UI, cinco paquetes del dock y diseño horizontal del cockpit.
- SourceManager usa JSON privado y MAIN/LAUNCHER; tratamiento especial de NavRadio+ para evitar actividades de servicio.
- MediaSources limita explícitamente Amazon Music, YouTube Music y NavRadio+.
- En la copia pública, SuvRearView usa GenericVehicleArtwork: geometría Canvas sin fabricante, carretera, matrícula y luces decorativas por horario. Los recursos del vehículo de referencia se retiraron de esta copia.
- VehicleLightingManager tiene una puerta futura CANBUS sin llamadores activos de producción.
- No hay cliente Binder FYT/SYU/CANBUS activo. El Binder GPS pertenece solo a nuestra aplicación.
- WeatherClient consulta Open-Meteo por HTTPS con coordenadas GPS. No mide temperatura exterior del coche.

## Estructura

app/src/main/java: UI, proveedores y políticas. app/src/main/res: recursos propios y vehículo optimizado. tests/java: políticas en host; app/src/androidTest: fixtures separados de producción. tools, schemas, profiles/examples, docs/reusable y prompts forman la capa reutilizable. Perfiles privados y builds quedan fuera de Git.

Actualización pública: Release y Lint completo ya finalizan correctamente después del ajuste específico de severidad autorizado. El error indicado arriba describe la base histórica/original; la advertencia permanece visible en la copia pública. Firma Release aún no configurada.
