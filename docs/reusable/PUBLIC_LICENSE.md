# Aplicación de Apache License 2.0

Fecha: 6 de noviembre de 2026. Titular y año proporcionados expresamente por el usuario: **Luis Zapien, 2026**. Selección de Apache-2.0 y aplicación autorizadas en la conversación. No se deducen otros titulares, años ni permisos de publicación.

## Alcance y avisos

Las aportaciones propias del código Android, herramientas, pruebas, documentación, esquemas, ejemplos y recursos gráficos originales de esta copia pública se licencian bajo Apache-2.0. NOTICE contiene **Copyright 2026 Luis Zapien**. No se alteran cabeceras de cientos de archivos ni se sustituyen avisos ajenos.

LICENSE es una copia byte a byte del [texto oficial completo en inglés](https://www.apache.org/licenses/LICENSE-2.0.txt), incluido su apéndice estándar. Los campos de ejemplo del apéndice oficial no se rellenan ni se confunden con el aviso específico del proyecto, que figura en NOTICE y README.

Los componentes ajenos conservan sus titulares/licencias. THIRD_PARTY_NOTICES.md identifica Wrapper Gradle, herramientas externas, Open-Meteo y apps/contenidos externos. Los scripts Wrapper y su JAR no se modifican; licenses/GRADLE_WRAPPER_LICENSE.txt conserva exactamente META-INF/LICENSE del JAR.

El automóvil público es geometría Canvas genérica, junto con los vectores sustituidos y el preview local; no se distribuyen las imágenes anteriores del vehículo de referencia. Apache-2.0 no concede derechos sobre marcas ni sobre futuros recursos de terceros.

## Fuentes y binarios

Al redistribuir las fuentes o binarios derivados, conservar la licencia y avisos aplicables, incluir NOTICE y los avisos de terceros correspondientes, e identificar archivos modificados conforme a la sección 4 de LICENSE. Los recordatorios de README no reemplazan el texto íntegro de la licencia.

La APK incluye assets/legal/LICENSE.txt, NOTICE.txt y THIRD_PARTY_NOTICES.md, sincronizados byte a byte con sus originales raíz. La licencia del Wrapper acompaña las fuentes; el Wrapper es una herramienta de desarrollo, no un componente runtime de la APK. tools/prepare_public.py conserva estos archivos en nuevas exportaciones de la copia revisada.

LICENSE_PROPOSAL.md es solo el registro de la decisión aplicada, no una licencia alternativa. MIT nunca se aplicó al código propio. No se cambiaron SDK, identidad, versión, firma, permisos, Java, diseño o comportamiento de la aplicación en esta fase.

## Límites y pendientes

Apache-2.0 permite usos del código propio sujetos a sus condiciones y no garantiza compatibilidad con todos los estéreos Android. Los términos de la API Open-Meteo son separados: su atribución visible junto al widget sigue pendiente; los avisos empaquetados no la sustituyen. La APK Release sigue sin firma pública. Faltan firma, pruebas de destino y autorización de publicación; no se instaló ni publicó nada ni se cambió HOME.

Fuentes oficiales: [Apache, aplicación a software propio](https://www.apache.org/foundation/license-faq.html), [Open-Meteo, datos y atribución](https://open-meteo.com/en/licence).

## Validación de la aplicación de licencia

- Texto LICENSE idéntico al oficial descargado, 11358 bytes; SHA-256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`.
- assembleDebug, assembleRelease, lintDebug y lintRelease correctos con el Wrapper existente; Lint: 0 errores y 21 advertencias previas en ambos tipos. ExpiredTargetSdkVersion sigue como Warning.
- Tres assets legales extraídos de cada APK y comparados byte a byte con sus originales raíz: idénticos. Integridad ZIP correcta.
- Debug mantiene firma de desarrollo verificada; Release continúa unsigned. No se generó una clave ni se cambió configuración de firma.
- Exportador ejecutado en una copia temporal aislada: conserva LICENSE, NOTICE, THIRD_PARTY_NOTICES.md, licencia del Wrapper y tres assets legales. 17 pruebas de herramientas correctas con ADB simulado.
- Código Java, recursos gráficos/XML, configuración Gradle, manifiesto y archivos del proyecto Android privado se conservan sin cambios durante esta fase. Solo documentación, metadatos legales, assets de texto y selección del exportador público se actualizan.

Compilar y verificar archivos no equivale a ejecutar una prueba en el estéreo. No se realizó conexión ADB ni publicación.
