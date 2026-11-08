# Componentes y servicios de terceros

Las aportaciones propias de FYT Smart Dashboard llevan Copyright 2026 Luis Zapien y Apache-2.0. Este documento identifica ámbitos separados; no cambia las condiciones de LICENSE ni concede derechos sobre contenido ajeno.

## Gradle Wrapper · distribuido con las fuentes

- Archivos: gradlew, gradlew.bat y gradle/wrapper/gradle-wrapper.jar.
- Licencia: Apache License 2.0. Las cabeceras originales de los scripts identifican a sus autores originales y se conservan sin cambios.
- El texto de META-INF/LICENSE del JAR se conserva íntegro en [licenses/GRADLE_WRAPPER_LICENSE.txt](licenses/GRADLE_WRAPPER_LICENSE.txt). No se ha encontrado un NOTICE adicional en ese JAR.
- No se modifica el código de estos archivos ni se sustituye su copyright por el del dashboard.
- Proyecto: [Gradle](https://gradle.org/). La distribución descargada por el Wrapper y sus componentes son externos a esta copia fuente y conservan sus propios términos.

## Herramientas y plataforma · obtenidas por el usuario

Android SDK, Platform Tools/ADB, Android Gradle Plugin y JDK no se redistribuyen como instalaciones dentro de este repositorio. El uso de sus APIs no convierte sus componentes en código propio del dashboard. Descárgalos de sus proveedores y conserva sus licencias si decides redistribuir alguna herramienta por separado. Las fuentes tipográficas utilizadas son las del sistema Android: no hay archivos de fuentes empaquetados por el proyecto.

No hay bibliotecas runtime externas declaradas en app/build.gradle. Si se incorporan dependencias en el futuro, revisar sus licencias y avisos antes de publicar una nueva versión.

## Open-Meteo · servicio y datos en ejecución

- Proveedor de datos meteorológicos: [Open-Meteo](https://open-meteo.com/).
- Datos: [Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/), conforme a la [licencia del proveedor](https://open-meteo.com/en/licence).
- El cliente selecciona datos actuales, traduce descripciones y adapta su presentación; no mide la temperatura exterior del vehículo. No se incorporan respuestas meteorológicas de usuarios como recursos del proyecto.
- Para obtener clima, WeatherClient transmite latitud y longitud GPS al servicio por HTTPS. Requiere ubicación e Internet.
- La API gratuita está sujeta a condiciones propias de uso no comercial y límites. Apache-2.0 permite usos comerciales del código propio, pero no concede uso comercial gratuito de esta API. Consulta los [términos vigentes](https://open-meteo.com/en/terms).
- El proveedor exige un enlace junto a donde se muestran sus datos. **La atribución visible junto al widget sigue pendiente antes de distribuir una APK con clima.** Incluir este aviso o un crédito en README no cierra por sí solo ese requisito.
- No se incluye código del servidor Open-Meteo ni se atribuye su licencia AGPL al código de este cliente por el mero consumo HTTP de la API.

## Aplicaciones externas y contenido multimedia

NavRadio+, Amazon Music, YouTube Music, Waze y Google Maps se abren o consultan mediante mecanismos Android. No se redistribuyen sus APKs, iconos oficiales, licencias de usuario ni contenido de sus servicios. Los nombres de paquete identifican interoperabilidad y no implican afiliación o respaldo.

Las carátulas y metadatos recibidos de sesiones multimedia pertenecen a sus respectivos titulares; no quedan cubiertos por la licencia del dashboard ni deben incluirse en capturas públicas sin los derechos pertinentes.

## Recursos gráficos de esta copia pública

El SUV genérico se dibuja mediante GenericVehicleArtwork; los iconos y adornos propios usan paths/Canvas del proyecto. Los recursos anteriores del vehículo de referencia y los vectores heredados de origen no acreditado fueron sustituidos en esta copia. No se distribuyen fotografías ni logotipos de fabricantes dentro de ella. Las aportaciones gráficas propias y su preview local forman parte del alcance Apache-2.0, sin conceder derechos sobre marcas de terceros.

Si se añade un recurso cuya procedencia o licencia no está acreditada, no se presume que LICENSE lo cubre: documentar el hallazgo y resolverlo antes de redistribuirlo.
