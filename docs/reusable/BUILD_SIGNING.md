# Compilación, firma y distribución controladas

## Entorno preservado

JDK 17, Wrapper 9.6.0, AGP 8.7.3, compileSdk 35, minSdk 26, targetSdk 28. Mantenerlos para reproducir la base; no sustituir Wrapper por Gradle global. [Requisitos oficiales de AGP 8.7](https://developer.android.com/build/releases/agp-8-7-0-release-notes).

Sin código nativo/NDK ni filtros ABI propios: la aplicación Java no impone una CPU concreta, pero las apps externas y cada firmware sí pueden limitar compatibilidad. Las 46 clases no demuestran soporte universal para todos los SDK superiores a minSdk.

El manifiesto solicita ubicación fina/gruesa, Internet, servicio en primer plano y recepción de arranque; registra NotificationListener para multimedia y candidata HOME. No solicita root, QUERY_ALL_PACKAGES ni permisos propietarios FYT. GPS/notificaciones siguen requiriendo autorización del usuario. Las restricciones de foreground/background y paquete visible deben probarse en cada versión Android antes de actualizar targetSdk.

## Debug

```sh
python3 tools/build.py debug --sdk "$ANDROID_HOME"
# Alternativa desde la raíz en macOS/Linux:
./gradlew --no-daemon --console=plain assembleDebug assembleDebugAndroidTest
# Windows:
gradlew.bat --no-daemon --console=plain assembleDebug assembleDebugAndroidTest
```

tools/build.py acepta --offline solo para un equipo con caché completa. Su ruta Windows ejecuta el JAR del Wrapper mediante Java, sin ensamblar comandos de shell. Python no guarda rutas locales en el repositorio. No copies local.properties de otro ordenador: configura tu SDK local o ANDROID_HOME.

APK Debug: app/build/outputs/apk/debug/app-debug.apk. La firma Debug es local a cada entorno: dos ordenadores pueden tener certificados distintos. No distribuyas la keystore privada del autor para resolverlo.

## Pruebas

```sh
python3 -m unittest discover -s tests/platform -v
python3 tools/run_host_tests.py --sdk "$ANDROID_HOME"
python3 tests/render_appearance_local.py
./gradlew lintDebug
```

La copia pública conserva targetSdk 28 y la regla ExpiredTargetSdkVersion como advertencia explícita. Release y Lint Debug/Release ya pasan, con 0 errores y 22 advertencias. Las otras 21 incidencias permanecen visibles. No significa cumplimiento de Google Play o pruebas físicas nuevas. Compilar instrumentación no significa ejecutarla.

## Release sin firma automática

```sh
python3 tools/build.py release --sdk "$ANDROID_HOME"
```

La configuración actual no contiene signingConfig Release ni minifyEnabled. assembleRelease finaliza correctamente y produce app-release-unsigned.apk, sin firma y no distribuible como instalador. El bloqueo histórico de lintVitalRelease se resolvió con la advertencia específica autorizada, sin migrar el SDK.

La configuración con credenciales externas está preparada como [propuesta sin activar](PUBLIC_SIGNING_PLAN.md). Generar la clave pública exclusiva, integrar esa propuesta o firmar requieren aprobación. No se reutiliza Debug ni la clave del original. Optimizar/minificar necesita un plan y pruebas separados.

Elige quién custodiará la clave y dónde quedará su copia cifrada. Mantén claves/contraseñas fuera del repositorio, perfiles, capturas, prompts y logs de CI. No se proporciona una clave compartida o contraseña de ejemplo.

Procedimiento **manual**, desde Build Tools oficiales ya instaladas y con variables/rutas del propietario:

```sh
# Generar una clave NUEVA solo si el propietario lo decide; preguntas interactivas, sin contraseñas en argumentos.
keytool -genkeypair -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS" -keyalg RSA -keysize 3072 -validity 10000
# Elegir archivos de salida nuevos; alinear ANTES de firmar.
zipalign -p 4 "$UNSIGNED_APK" "$ALIGNED_APK"
apksigner sign --ks "$KEYSTORE_PATH" --ks-key-alias "$KEY_ALIAS" --out "$SIGNED_APK" "$ALIGNED_APK"
apksigner verify --verbose --print-certs "$SIGNED_APK"
zipalign -c 4 "$SIGNED_APK"
```

En Windows utiliza las variables/rutas equivalentes de PowerShell y ejecutables de Build Tools. No pases contraseñas con flags, no publiques datos del titular introducidos en el certificado. Android Studio también permite Generate Signed Bundle / APK, bajo control del propietario. [Firma oficial](https://developer.android.com/studio/publish/app-signing), [apksigner y orden de alineación](https://developer.android.com/tools/apksigner).

Calcula SHA-256 de la APK con `shasum -a 256` (macOS), `sha256sum` (Linux) o `Get-FileHash -Algorithm SHA256` (PowerShell). Registra versión, certificado, compilación, pruebas y límites. El checksum no sustituye validar la firma.

## Actualizaciones y coexistencia

Mismo applicationId requiere identidad de firma compatible y versionCode adecuado. Cambiar de Debug a una clave Release nueva **no es una actualización compatible automáticamente**. Si falla la firma, detente: no desinstales ni borres datos para forzarlo. Para pruebas aisladas se puede proponer otro applicationId, revisando todos los componentes/IDs asociados, sin afectar la instalación original.

## Instalación: paso separado y autorizado

Primero verificar objetivo, modelo/SDK, paquete y certificado, guardar APK previa cuando sea accesible y comprobar HOME actual. Solo después de autorización explícita para ese dispositivo y archivo:

```sh
adb -s "$DEVICE" install -r "$SIGNED_OR_DEBUG_APK"
```

No automatizado por nuestras herramientas. Abrir manualmente la app desde el launcher existente; revisar GPS real, temas, cinco accesos, permisos y controles publicados por cada app. No seleccionar HOME en la primera prueba. No reboot, pm clear, uninstall, root o modificaciones OEM como recuperación. Una reversión debe considerar versión/certificado y conservar datos, con autorización específica.
