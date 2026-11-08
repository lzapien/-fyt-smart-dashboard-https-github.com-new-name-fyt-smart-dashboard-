> Canal elegido 2026-11-07: entregar solo fuentes y documentación, sin APK ni publicación remota. Firma y pruebas físicas son pendientes de una eventual distribución binaria, no requisitos para abrir el código. Véase AI_REPRODUCTION.md.

> Actualización de licencia: Apache-2.0 aplicada con Copyright 2026 Luis Zapien; detalles en PUBLIC_LICENSE.md. No se firmó, instaló ni publicó una APK.

> Actualización gráfica 2026-11-06: vehículo y vectores públicos sustituidos por geometrías nuevas; consultar GENERIC_VEHICLE.md. Resultados posteriores al reemplazo: Debug/Release y APK de pruebas compilan; Lint Debug/Release: 0 errores, 21 advertencias anteriores, ninguna nueva. Las demás cifras de esta auditoría describen la revisión anterior salvo indicación expresa.

# Auditoría final de firma, licencias y preparación GitHub

Fecha de revisión: 6 de noviembre de 2026. **Preparación local, no publicación.** Release compila, pero el binario actual sigue sin firma de distribución. Apache-2.0 ya se aplicó al contenido propio con Copyright 2026 Luis Zapien; no hay autorización para generar la clave pública de distribución. El código y los recursos del dashboard no se modificaron en esta auditoría.

## 1. Firma recomendada

No se encontró un keystore Release en los proyectos público/original auditados ni una referencia Gradle a un almacén externo. No están definidas las variables propuestas para firma pública. Existe el almacén Debug habitual, que no se seleccionó ni leyó para firmar. No se puede descartar una clave externa que el propietario no haya facilitado, y no se certifica ninguna como válida.

La APK actual se verificó con apksigner y sigue unsigned: el resultado Missing META-INF/MANIFEST.MF corresponde a la ausencia de firma JAR/APK válida, no a un permiso para distribuirla. Identidad actual: com.fytsmart.dashboard, 1.1.2 / 21; no se cambia.

Se recomienda una clave **nueva y exclusiva del canal público**, PKCS12/RSA 3072, 10000 días propuestos, fuera de ambos proyectos y con respaldos cifrados. La clave debe ser estable para futuras actualizaciones. Contraseñas en gestor de secretos, no en Git/gradle.properties/chat/historial de shell. La ruta, alias, responsable y datos públicos del certificado requieren aprobación. El [plan de firma](PUBLIC_SIGNING_PLAN.md) contiene configuración externa propuesta y pasos exactos, sin activarla.

La plantilla se probó solo para configuración de Gradle en una copia temporal: sin opt-in mantiene desarrollo unsigned; opt-in sin credenciales falla explícitamente. No se generó clave, se validó un keystore real, se firmó APK o activó signingConfig en el proyecto público. Antes de usarla con una clave se debe verificar el certificado aprobado y comparar su fingerprint con el de la APK final. [Criterios oficiales de firma](https://developer.android.com/studio/publish/app-signing).

Una nueva firma pública no actualizará una instalación privada firmada con Debug u otra clave del mismo paquete. No se desinstala, cambia applicationId o borra datos para resolverlo sin un plan autorizado. Las limitaciones de actualización no desaparecen al conservar versionName.

## 2. Inventario de licencias y riesgos

| Componente | Evidencia | Estado antes de publicar |
|---|---|---|
| Java, políticas y Canvas propios | Código del proyecto, sin bibliotecas runtime externas declaradas | Apache-2.0 aplicada; Copyright 2026 Luis Zapien |
| Iconos personalizados, nieve y Halloween | Paths/Canvas propios; no iconos oficiales descargados | Integrar en el alcance aprobado del código propio |
| Vehículo genérico y preview actualizado | GenericVehicleArtwork.java: Canvas sin marca; los recursos anteriores fueron retirados de la copia pública | Cambio gráfico autorizado e incluido en las aportaciones propias Apache-2.0 |
| PNG maestro anterior | Retirado de la distribución pública junto con los dos WebP | Solo conservado en el proyecto privado y sus respaldos |
| Fuentes | Tipografías Android del sistema; no hay TTF/OTF empaquetadas | Sin archivos de fuentes que relicenciar en el APK |
| Wrapper Gradle | Scripts Apache-2.0; licencia del JAR copiada y comprobada | Mantener cabeceras y licenses/GRADLE_WRAPPER_LICENSE.txt; conservar su licencia original, sin cambiar titulares |
| AGP, JDK, SDK, ADB | Herramientas externas, no incluidas en la copia fuente | Descargar de fuentes oficiales y respetar sus términos; no distribuir sus instalaciones |
| Open-Meteo | Consumo HTTPS de API, no código del servidor incorporado | Revisar términos del servicio y atribución de datos; pendiente específico descrito abajo |
| Apps de radio, navegación y música | Solo nombres de paquetes y APIs de interoperabilidad | No se incluyen sus APKs, logos oficiales o licencias de usuario |

**Licencia aplicada:** Apache License 2.0 para las aportaciones propias, Copyright 2026 Luis Zapien, sin extenderla a recursos o servicios de terceros. LICENSE conserva el texto oficial completo; NOTICE registra el copyright proporcionado por el usuario. LICENSE_PROPOSAL.md se conserva como registro histórico de la decisión aplicada. Consultar THIRD_PARTY_NOTICES.md y PUBLIC_LICENSE.md; esta revisión técnica no es una conclusión jurídica sobre todas las jurisdicciones.

Open-Meteo publica sus datos bajo CC BY 4.0 y exige un enlace junto a donde se muestran. WeatherClient usa el servicio y MainActivity muestra datos sin ese enlace: el README no corrige por sí solo esa falta. Antes de distribuir una APK con clima hay que aprobar una atribución accesible junto al widget y los avisos de licencia/cambios pertinentes. **No se modificó la UI en esta auditoría.** [Condiciones de atribución](https://open-meteo.com/en/licence).

La API gratuita tiene condiciones de uso no comercial y límites propios; una licencia Apache-2.0 del cliente no concede uso comercial ilimitado del servicio. No se incorpora el código AGPL del servidor, por lo que no se ha identificado aquí una dependencia AGPL empaquetada: consumo de API, datos y código tienen ámbitos diferentes. Revisar el modelo de distribución/comercialización con los términos del proveedor. [Términos de Open-Meteo](https://open-meteo.com/en/terms).

Los recursos del vehículo de referencia se retiraron con autorización de esta copia pública, incluido su maestro y preview derivado. La geometría genérica y el preview actual se crearon para el proyecto. No se concede con ellos una licencia sobre marcas o diseños de terceros que alguien decida añadir en el futuro.

## 3. Compatibilidad Android real

| Dato | Configuración conservada |
|---|---|
| Java / Gradle / AGP | 17 / Wrapper 9.6.0 / 8.7.3 |
| compileSdk / minSdk / targetSdk | 35 / 26 / 28 |
| Paquete / versión | com.fytsmart.dashboard / 1.1.2 / 21 |
| Código nativo/ABI propio | Sin .so ni filtros ABI; apps externas tienen sus propios requisitos |
| Release | BUILD SUCCESSFUL; debuggable=false; unsigned |
| Lint completo Debug/Release | 0 errores y 21 advertencias; ExpiredTargetSdkVersion visible como Warning |

minSdk 26 permite instalar a partir de Android 8 si el equipo cumple los demás requisitos. targetSdk 28 conserva comportamientos de compatibilidad antiguos; no significa máximo Android 9 ni certifica versiones posteriores.

- Android 10/FYT es la referencia visual funcional previa. No hubo pruebas físicas nuevas en esta auditoría. GPS, medios, clima, suspensión y arranque requieren condiciones y permisos reales.
- Android 14 bloquea por esa regla de instalación los targets inferiores a 23; Android 15, inferiores a 24. El target 28 supera esos umbrales concretos, pero esto **no garantiza instalación o funcionamiento completo** ante otras restricciones. [Android 14](https://developer.android.com/about/versions/14/behavior-changes-all), [Android 15](https://developer.android.com/about/versions/15/behavior-changes-all).
- Android 16/17 y OEM recientes siguen pendientes de prueba. Cambios aplicables a todas las apps, restricciones de batería/background, notificaciones, permisos y arranque pueden afectar incluso a un target antiguo. No se usará bypass de instalación ni root para aparentar compatibilidad.
- Google Play exige objetivos mucho más recientes: la página oficial consultada indica API 36 para nuevas apps/actualizaciones generales desde agosto de 2026, con excepciones por plataforma. target 28 no cumple. Un estéreo FYT no se clasifica automáticamente como Android Automotive OS. Reclasificar Lint no da elegibilidad para la tienda. [Política oficial](https://developer.android.com/google/play/requirements/target-sdk).
- La verificación de desarrolladores y registro de paquetes puede aplicar según canal, región y certificación del dispositivo. No es lo mismo que firmar una APK ni se presupone un bloqueo global en todos los FYT. Revisar requisitos al elegir distribución. [Información oficial](https://developer.android.com/developer-verification).

No se actualizó SDK, Gradle, plugin, versión o dashboard. Si se busca modernización/Play, hace falta un plan separado sobre permisos de ubicación, foreground services, notificaciones, componentes exportados, apertura de actividades y pruebas.

### Dependencias de hardware y apps

No hay cliente Binder FYT/SYU/CANBUS activo. La unión Binder GPS es interna a esta aplicación. No existen integración de volante, lectura exterior física o CANBUS disponibles por detectarse un servicio OEM. Las luces del coche siguen siendo decorativas.

Dock y MediaSources usan paquetes concretos de NavRadio+, Amazon Music, YouTube Music, Maps y Waze. Sus metadatos, controles y disponibilidad se verifican por versión/dispositivo; no se distribuyen esas apps. Sin GPS fresco se muestra --; rumbo GPS fiable requiere movimiento y no sustituye una brújula magnética estacionaria. OEM distintos pueden limitar inicio, GPS continuo y NotificationListener. Matriz ampliada en COMPATIBILITY.md.

## 4. Revisión de GitHub y privacidad

El Git **local preexistente** de esta copia está vacío: sin commits, tags, archivos en el índice o remotos. No se creó otro repositorio y no hay historial privado importado. Antes del primer commit hay que elegir conscientemente identidad pública de autor/correo; una historia vacía no tiene un autor ya aprobado.

La revisión de fuente no encontró matrícula personal, IP privada fija, credenciales reales, keystores o rutas de usuario. El default público es TU-PLACA y se conserva su edición desde ajustes. Los recursos son los ya revisados con placa vacía/genérica; los hashes y la inspección anterior respaldan esa comprobación. El análisis de texto no es OCR ni garantiza detectar cualquier secreto futuro.

README, AGENTS y guías de compilación/validación tenían afirmaciones del bloqueo antiguo de Release; se corrigieron como documentación. El plan de firma es nuevo y está propuesto, no activo. Posteriormente se aplicó Apache-2.0 y se añadieron avisos de licencia, sin activar signingConfig. Los scripts de diagnóstico siguen usando lista cerrada de consultas de lectura, sin ejecutar Binder o instalar apps. Doctor/discover pueden mostrar identificadores de transporte en terminal: no publicar sus salidas.

Excluir de la publicación .git como contenido de ZIP, .gradle, .idea, build, artifacts, local.properties, perfiles privados, respaldos, APK unsigned, claves/certificados privados y archivos de secretos. .gitignore se reforzó para .pfx/.pk8/.p8, .env y archivos de propiedades de firma. Los comandos de selección de archivos deben ejecutarse **solo en esta copia pública**, nunca en la raíz privada.

Limitaciones del escáner existente: no detecta todos los formatos de almacén o secretos arbitrarios, no hace OCR y excluye carpetas generadas; la lista de exportación y revisión manual son parte del proceso. Esta auditoría revisó además nombres de .pfx/.pk8/.p8 y claves conocidas, pero no cambió código del escáner. No convertir un PASS en garantía universal de privacidad.

El ZIP de fuentes anterior al ajuste Lint y a estos documentos ya está desactualizado para una primera publicación; no se sobrescribió ni publicó. Una exportación final nueva debe incorporar exactamente los archivos revisados después de todas las aprobaciones.

## 5. Lista final y pasos de publicación (NO ejecutados)

1. **Conservar alcance y avisos:** licencia propia Apache-2.0 aplicada, Copyright 2026 Luis Zapien; vehículo público genérico y vectores sustituidos con autorización. Conservar LICENSE/NOTICE y avisos de terceros. Resolver atribución meteorológica y revisar derechos de cualquier recurso futuro antes de publicarlo.
2. **Autorizar correcciones pendientes:** atribución junto al clima y cualquier corrección de privacidad/herramientas necesaria, sin rediseñar. Revisar las otras advertencias existentes y el icono de aplicación para el primer lanzamiento.
3. **Aprobar identidad de firma pública:** ruta fuera de ambos proyectos, alias, datos públicos del certificado, custodia y respaldo. Aprobar generación y ejecutar el procedimiento del plan con contraseñas introducidas por el propietario, no enviadas al asistente.
4. **Aprobar integración de firma externa:** incorporar la plantilla propuesta, validar ruta/certificado/fingerprint de la clave NUEVA y confirmar que no usa Debug/original. Compilar y firmar con caché de configuración desactivada, sin instalar automáticamente.
5. **Verificar binarios:** assembleDebug/assembleRelease, Lint completo, firmas y fingerprint, manifiesto/SDK/permisos/versiones, SHA-256, privacidad de fuentes/APK y consistencia de avisos/licencias. No adjuntar un unsigned a Releases.
6. **Prueba aislada autorizada:** equipo o emulador de destino, preservar HOME OEM/datos, revisar GPS y sin señal, sesión multimedia y controles reales, clima/atribución, temas y tamaños. Documentar lo no probado y obtener capturas sin datos personales. No usar fixtures sobre el estéreo habitual sin permiso.
7. **Revisión local final:** crear exportación nueva por lista permitida, revisar su manifest de hashes, git status y diff. Elegir identidad pública de Git; agregar solo archivos aprobados, revisar el índice y preparar el primer commit/tag limpio tras autorización. No importar historia privada.
8. **Aprobación de GitHub:** decidir cuenta, nombre, visibilidad, URL, primer tag y si se publican fuentes o también APK firmada. Solo después crear el remoto/repositorio y hacer push. Esta auditoría no ejecuta gh repo create, git commit, git remote add, git push o release upload.
9. **Publicar y mantener:** incluir LICENSE aprobado, avisos de terceros, créditos/privacidad, matriz real, instalación/recuperación y hashes/certificado público de una APK firmada. Mantener la clave privada fuera, controlar futuras versiones y revisar seguridad/compatibilidad por release.

**Conclusión: no está lista para distribución binaria pública.** El código está preparado técnicamente para revisión; siguen pendientes firma válida, atribución meteorológica, validaciones de destino y aprobación de publicación. No se generó una clave ni se modificó el dashboard para cerrar artificialmente esos pendientes.
