# Estrategia de firma pública · propuesta sin activar

Estado: no se ha generado ni almacenado ninguna clave. app/build.gradle sigue sin signingConfig Release. La APK actual compila y permanece sin firma. Este documento prepara la configuración y el procedimiento; integrarlos y crear la clave requiere aprobación expresa.

## Resultado de la inspección

No hay archivos .jks/.keystore/.p12/.pfx en el proyecto público ni en el original auditado y no hay referencia Gradle a un keystore Release externo. Existe el almacén Debug habitual del ordenador, que no se reutiliza. No se buscaron secretos por todo el equipo ni se inspeccionó el llavero. No se puede certificar un keystore externo cuya ubicación y credenciales no fueron proporcionadas; por ahora no existe una identidad Release pública validada.

## Clave recomendada y custodia

Clave nueva y exclusiva del canal público, PKCS12 y RSA 3072 bits, validez propuesta 10000 días. Alias propuesto public-release-v1, sujeto a aprobación. La validez y el alias se comprueban después de crearla; no son valores ya existentes. Registrar fuera del repositorio el fingerprint SHA-256 del certificado y el responsable de su custodia.

Ubicación propuesta, fuera tanto del repositorio público como del original:

- macOS/Linux: $HOME/.local/share/fyt-smart-dashboard-signing/public-release/public-release.p12.
- Windows: %LOCALAPPDATA%/FYT-Smart-Dashboard/Signing/public-release/public-release.p12.

No se crearon esas carpetas. Usar almacenamiento cifrado del sistema; directorio 0700 y archivo 0600 en POSIX, acceso solo al propietario mediante ACL en Windows. Mantener dos respaldos cifrados independientes, uno offline, y la contraseña en un gestor de secretos separado. Probar recuperación mediante keytool -list -v sobre una copia autorizada. No subir la keystore ni sus contraseñas a GitHub, chats, capturas, perfiles, logs o un ZIP de fuentes.

El certificado y su fingerprint son públicos; la clave privada y contraseñas no. Definir conscientemente los datos públicos del certificado sin información sensible innecesaria ni identidad ajena.

## Configuración Gradle preparada

**Propuesta para añadir al final del app/build.gradle, no aplicada.** No cambia SDK, applicationId, versión o código. Solo selecciona firma al solicitar -PfytPublicReleaseSigning=true; si faltan credenciales, falla y no sustituye la clave por Debug. Sin ese flag, conserva la Release unsigned de desarrollo.

```groovy
// Propuesta: añadir después del bloque android existente SOLO tras aprobación.
def publicSigningRequested = providers.gradleProperty('fytPublicReleaseSigning').getOrElse('false')
if (!(publicSigningRequested in ['true', 'false'])) {
    throw new GradleException('fytPublicReleaseSigning debe ser true o false.')
}
if (publicSigningRequested == 'true') {
    def externalNames = [
        'FYT_PUBLIC_SIGNING_STORE_FILE',
        'FYT_PUBLIC_SIGNING_STORE_PASSWORD',
        'FYT_PUBLIC_SIGNING_KEY_ALIAS',
        'FYT_PUBLIC_SIGNING_KEY_PASSWORD'
    ]
    def externalValues = externalNames.collectEntries { name ->
        [(name): providers.environmentVariable(name).getOrNull()]
    }
    def missingNames = externalNames.findAll { !externalValues[it] }
    if (!missingNames.isEmpty()) {
        throw new GradleException('Firma pública solicitada, faltan variables: ' + missingNames.join(', '))
    }
    def suppliedStore = new File(externalValues['FYT_PUBLIC_SIGNING_STORE_FILE'])
    def publicStore = suppliedStore.canonicalFile
    if (!suppliedStore.isAbsolute() || !publicStore.isFile() ||
            publicStore.toPath().startsWith(rootProject.projectDir.canonicalFile.toPath())) {
        throw new GradleException('El keystore público debe existir en una ruta absoluta fuera del repositorio.')
    }
    def publicAlias = externalValues['FYT_PUBLIC_SIGNING_KEY_ALIAS']
    if (publicStore.name.equalsIgnoreCase('debug.keystore') || publicAlias.equalsIgnoreCase('androiddebugkey')) {
        throw new GradleException('No se permite seleccionar el almacén o alias Debug para la firma pública.')
    }
    android {
        signingConfigs {
            publicDistribution {
                storeFile = publicStore
                storeType = 'PKCS12'
                storePassword = externalValues['FYT_PUBLIC_SIGNING_STORE_PASSWORD']
                keyAlias = publicAlias
                keyPassword = externalValues['FYT_PUBLIC_SIGNING_KEY_PASSWORD']
            }
        }
        buildTypes {
            release {
                signingConfig = signingConfigs.publicDistribution
            }
        }
    }
}
```

Las cuatro variables se cargan solo en el entorno temporal del proceso, desde un gestor de secretos o una entrada interactiva que no deje contraseñas en el historial. No crear un .env o signing.properties con contraseñas en el repositorio. No imprimir env, usar set -x, registrar la terminal con secretos, compartir procesos con usuarios no confiables ni activar build scans/debug logs. Aunque la clave esté fuera de Git, el entorno del proceso no es una caja fuerte y necesita un ordenador confiable.

Validar manualmente la ruta y que la clave corresponda al canal público NUEVO aprobado. El rechazo de nombre/alias Debug es una defensa adicional, no un detector de todas las claves privadas posibles. No se busca ni se elige automáticamente ninguna clave del proyecto original.

Para firmar, una vez aprobada e integrada la propuesta, mantener la caché de configuración desactivada para no persistir credenciales en su estado serializado:

```sh
./gradlew --no-daemon --no-configuration-cache --console=plain -PfytPublicReleaseSigning=true assembleRelease
```

No se ejecutó ese comando de firma. La plantilla se evaluó en un proyecto temporal con Gradle: help sin opt-in pasó; help con opt-in y sin credenciales falló indicando solo los nombres de variables ausentes. No se usaron claves y eso **no prueba todavía una firma real**.

## Procedimiento posterior, solo tras aprobación

1. Acordar titular, alias, ruta, datos públicos del certificado, contraseña/custodia y canal de actualizaciones. No pedir que el usuario pegue contraseñas en Codex.
2. Crear la carpeta segura y generar una clave NUEVA, con entrada interactiva. Ejemplo no ejecutado:

```sh
keytool -genkeypair -storetype PKCS12 -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS" -keyalg RSA -keysize 3072 -validity 10000
keytool -list -v -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS"
```

3. Confirmar PrivateKeyEntry, algoritmo, fechas, alias y certificado no Debug; realizar y comprobar respaldos cifrados. Registrar su fingerprint de referencia.
4. Aprobar e integrar la propuesta Gradle. Cargar credenciales de forma temporal y ejecutar assembleRelease firmado. Nunca copiar la keystore al árbol del repositorio.
5. Verificar APK con apksigner verify --verbose --print-certs y comparar su certificado con el fingerprint público aprobado. Comprobar paquete, versión, permisos, SDK y debuggable=false. Calcular SHA-256. No editar la APK después de firmar.
6. Si se firma manualmente el unsigned, usar zipalign antes de apksigner sign y escribir un archivo nuevo. No hacer ambos métodos sin decidir cuál será el procedimiento oficial.
7. Realizar prueba aislada autorizada y revisar requisitos de publicación. Generar clave o compilar no autoriza instalar, cambiar HOME o subir nada.

## Identidad de actualizaciones

La firma pública nueva no actualizará automáticamente una instalación previa del mismo paquete firmada con otra clave, incluida la instalación Debug privada del autor. No se modifica el estéreo privado ni se desinstala/borran datos para resolverlo. Mantener certificado del canal público estable, versionCode apropiado y procedimiento de migración/coexistencia separado si fuera necesario; cualquier cambio de applicationId requiere aprobación.

Fuentes: [firma Android](https://developer.android.com/studio/publish/app-signing), [apksigner](https://developer.android.com/tools/apksigner). La firma acredita integridad/identidad; no acredita permisos de recursos, compatibilidad total o aprobación de Google Play.
