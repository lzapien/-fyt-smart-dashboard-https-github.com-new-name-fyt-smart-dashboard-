# Inventario de recursos y revisión de distribución

Estado: revisión técnica de procedencia, **no autorización de publicación ni conclusión jurídica**. Apache-2.0 aplicada a las aportaciones propias con Copyright 2026 Luis Zapien; ver LICENSE, NOTICE y THIRD_PARTY_NOTICES.md. No implica autorización de publicación.

| Componente | Procedencia/evidencia | Preparación |
|---|---|---|
| Java/UI/dibujos Canvas | Código del proyecto; historial privado de desarrollo | Apache-2.0; Copyright 2026 Luis Zapien |
| Iconos del dock | Paths personalizados de DockIconDrawable; no iconos descargados de apps | Apache-2.0 para los trazos propios; conservar avisos del proyecto |
| SUV genérico Canvas | GenericVehicleArtwork.java: geometrías y gradientes nuevos, sin imagen de fabricante ni logotipo | Sustituye y retira los dos WebP y el PNG maestro anteriores de la copia pública; incluido en el alcance propio Apache-2.0 |
| Vectores heredados y backups | car, car_front, plane_takeoff y circle_letter_p redibujados con coordenadas nuevas | Sin paths importados; ocho XML sustituidos. No atribuirlos a bibliotecas por semejanza |
| Preview | Render local de GenericVehicleArtwork y DecorationRenderer con TU-PLACA | Actualizado; no captura Android ni recurso del vehículo anterior |
| Nieve/Halloween | Paths y Paint propios, sin imágenes descargadas | Incluidos con el código propio Apache-2.0 |
| Tipografías | Fuentes Android del sistema | No se redistribuyen archivos de fuentes |
| Gradle Wrapper | Scripts con cabecera Apache-2.0; JAR contiene META-INF/LICENSE | Conservar cabeceras y licencia empaquetada; copiar aviso extraído sin alterarlo |
| AGP, JDK y SDK | Herramientas oficiales externas | No se incluyen sus distribuciones; el usuario obtiene/acepta términos oficiales |
| Open-Meteo | Servicio HTTPS externo, no biblioteca empaquetada | Identificar proveedor y revisar atribución/condiciones de uso antes de distribuir binarios |
| NavRadio+, Amazon, YouTube Music, Maps, Waze | Apps externas; nombres de paquete para interoperabilidad | Ninguna APK, icono oficial, clave o recurso de estas apps se distribuye |

Open-Meteo distingue las condiciones de su API gratuita no comercial y los datos CC-BY 4.0. Un repositorio con licencia permisiva no convierte el servicio gratuito en un servicio comercial ilimitado. El usuario debe revisar su caso y la atribución exigida; no se cambia endpoint o licencia del proveedor en esta preparación. [Condiciones oficiales](https://open-meteo.com/en/terms). La consulta actual envía coordenadas GPS al proveedor: debe explicarse en la documentación de privacidad de una distribución.

No se presentan capturas privadas como recursos públicos. Los previews nuevos son renders locales genéricos, etiquetados como tales; no se hacen pasar por capturas Android. Capturas reales de una instalación genérica quedan pendientes de autorización de una prueba aislada.

Antes de GitHub: conservar LICENSE/NOTICE y avisos originales de terceros, conservar la sustitución genérica autorizada, decidir atribución meteorológica visible apropiada y revisar todos los archivos de la copia limpia. No copiar el archivo privado de generación de imágenes ni sus capturas históricas.

## Pendiente concreto de atribución

La página oficial de licencia Open-Meteo exige un enlace junto a donde se muestran los datos. El widget actual no lo incluye; citar al proveedor en README no cierra por sí solo ese requisito. Hace falta aprobar un cambio acotado de atribución antes de distribuir la APK. No se modificó la interfaz en esta auditoría. Ver FINAL_PUBLICATION_AUDIT.md.
