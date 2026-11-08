# Personalizar este dashboard con Codex

No se crea otra interfaz: se parte del cockpit existente. Primero presenta device-profile.json y device-report.md, lee AGENTS.md y aprueba el plan antes de editar código.

## Sin cambiar código

Mantén SOURCE aproximadamente un segundo → Ajustes. Selecciona Claro/Oscuro/Automático; el modo se guarda al elegirlo. Edita matrícula, adornos Original/Navidad/Halloween y animaciones; usa Guardar para estos últimos. Una matrícula en blanco vuelve al valor predeterminado genérico en la copia pública. Las preferencias privadas ya guardadas no se sobrescriben al cambiar ese valor inicial.

## Solicitudes reutilizables

| Objetivo | Qué debe revisar Codex | Límite |
|---|---|---|
| Colores/contraste | DashboardPalette, DisplayUi, Canvas de instrumentos/adornos | Conservar tamaños/lecturas/animaciones |
| Otro vehículo | GenericVehicleArtwork, SuvRearView, luces y placa | Propuesta gráfica + derechos, no foto pesada de fondo |
| Otra matrícula | Ajustes; VehiclePlateConfig solo para default público | No incluir matrícula personal en ejemplos/capturas |
| Resolución | Tamaño/densidad efectivos del perfil, compact, áreas Canvas | No ejecutar wm size/density de escritura |
| Otras apps | Paquetes reales, launcher accesible, MediaSources y dock | JSON SOURCE no reconfigura automáticamente el widget/dock |
| Radio alternativa | MAIN/LAUNCHER, MediaSession y documentación del desarrollador | No invocar servicios FM/Binder desconocidos |
| GPS/brújula | LocationManager, frescura, bearing y precisión | Sin muestras inventadas; no rumbo magnético si no existe |
| Datos del coche | Evidencia física e interfaz documentada | Sin fuente: no disponible, sin comandos de prueba al CAN |

Ejemplo de encargo: «Lee mi perfil local y el código. Quiero cambiar solo los colores del modo claro para mayor contraste. Presenta primero archivos, riesgos y pruebas; conserva el diseño y espera autorización antes de modificar. No te conectes al estéreo».

Después de aprobar: snapshot, cambio pequeño, pruebas host/compilación, revisión visual apropiada y reporte. Una APK compilada no se instala sin un segundo permiso explícito para dispositivo y archivo. Los prompts por fase se encuentran en ../../prompts/.
