# Matriz de compatibilidad

Estados: verificado en una prueba concreta, detectado por metadatos/código, desconocido sin prueba suficiente. Nunca extrapolar el resultado de FYT a todos los estéreos.

| Función | FYT UIS7862 / Android 10 de referencia | Otros Android | Degradación |
|---|---|---|---|
| UI horizontal 1280×720 | Verificada visualmente | Desconocido hasta prueba de tamaño/dp | Ajustar sin forzar resolución del sistema |
| Claro/oscuro y adornos | Verificado manual en referencia | APIs estándar, falta prueba del equipo | Tema predeterminado, animación estática si ahorro |
| Auto 07:00/18:00 | Política local probada; selección manual en hardware | Depende de hora/zona del Android | Recalcular al reanudar |
| Velocidad GPS | Implementación real; depende de señal | Detectar declaración y probar recepción | -- sin medición fresca |
| Brújula | Rumbo GPS con movimiento; no magnética | Requiere fix/rumbo fiable | -- detenido o sin rumbo |
| Clima Open-Meteo | Implementado; ubicación + HTTPS | Requiere conexión y términos del proveedor | No disponible/sin actualización |
| Abrir Maps/Waze | Integración PackageManager | Paquete/actividad/visibilidad por verificar | Mensaje sin cerrar dashboard |
| SOURCE/App Drawer | Verificado en referencia | Consulta MAIN/LAUNCHER, restricciones OEM posibles | Lista vacía/mensaje, sin QUERY_ALL_PACKAGES |
| NavRadio+ | Paquete confirmado en referencia; MediaSession según su versión | Verificar instalación y API expuesta | Solo abrir app si no hay controles |
| Amazon/YouTube Music | Proveedores soportados por código y pruebas previas | Permisos, acciones, cuenta/sesión por verificar | Sin sesión/controles ocultos |
| Información CANBUS | Sin integración activa | Desconocido; presencia de servicio no basta | Campos ocultos |
| Temperatura exterior | Fuente física no verificada | Desconocido | Ocultar; no usar clima/CPU/batería |
| Botón SOURCE físico | No implementado | Desconocido | Botón UI sigue disponible |
| ADB TCP / USB / pairing | TCP autorizado utilizado en referencia | Depende de firmware y opciones expuestas | Trabajar offline sin bypass |
| Arranque/reanudación | Recuperación acotada implementada | OEM puede restringir callbacks/servicios | Permitir apertura manual |
| Android 8/9 u 11+ | Compila con minSdk 26; no certificado en todos | Desconocido | Evaluar permisos/background antes de adaptar |
| Windows/Linux herramientas | Código portable y pruebas simuladas | Ejecución nativa pendiente | Usar Python directo; no cambiar políticas |

MediaSession presente no implica controles o metadatos publicados. La tabla no da por verificados todos los botones de cada aplicación en su última versión. El APK no incorpora radio, Maps, Waze, Amazon o YouTube Music; deben estar disponibles legítimamente en el dispositivo.

## Auditoría pública actual

SDK 35/26/28 conservado. Release compila y permanece unsigned; Lint completo muestra 0 errores y 21 advertencias. Ver FINAL_PUBLICATION_AUDIT.md para límites en Android recientes, firma y requisitos de publicación; no hay prueba nueva en otros dispositivos.
