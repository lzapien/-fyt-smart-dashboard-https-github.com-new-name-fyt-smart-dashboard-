# Arquitectura actual y diseño adaptable

Este es un **diseño propuesto** de interfaces; no una refactorización aplicada ni un launcher nuevo. La composición y el aspecto del cockpit se mantienen.

## Mapa actual

| Responsabilidad | Clases |
|---|---|
| Composición, GPS visible, reloj/clima/dock | MainActivity, SpanishDateView/Formatter |
| Velocidad, validez y telemetría | GpsLocationService, GpsSpeedState, GpsTelemetry, GpsUpdatePolicy |
| Vistas de instrumentos | SpeedometerView, GpsIndicatorView, CompassIndicatorView |
| Vehículo y decoraciones | SuvRearView, GenericVehicleArtwork, DecorationRenderer, VehiclePlateConfig |
| Iluminación virtual | VehicleLightingManager/Policy; horario, no CANBUS activo |
| Clima | WeatherClient, WeatherPresentation, WeatherScene |
| Multimedia | MediaSessionCoordinator, MediaSelectionPolicy, MediaControlDispatcher, MediaActions, MediaSources, MediaArtwork, ArtworkStage, MediaListener |
| Apertura de aplicaciones | SourceManager, SourceCycle, LauncherAppCatalog, AppDrawerActivity/Search |
| Apariencia | DashboardPalette, DisplayMode/Controller/Preferences, DisplayUi, ThemeManager/Config, DockIconDrawable |
| Configuración y arranque | DashboardSettingsActivity, DashboardPreferences, StartupRecoveryPolicy, DashboardRecoveryController, DashboardBootReceiver |

El enlace Binder de GpsLocationService es interno al proceso/aplicación; no es integración Binder con FYT. No existe SteerClient. SOURCE es un botón de nuestra UI; no se asume origen de una tecla física.

## Fronteras propuestas

| Interfaz futura | Datos/acciones | Adaptador inicial | Sin capacidad |
|---|---|---|---|
| LocationSpeedProvider | Fix, velocidad, precisión, marca monotónica, estado | Android LocationManager/GpsLocationService | -- y esperar permiso/señal, sin estimar |
| HeadingProvider | Rumbo, origen GPS/magnético, fiabilidad | Curso GPS actual | -- al estar detenido o sin rumbo válido |
| MediaControllerProvider | Sesión identificada, metadatos, imagen, acciones soportadas | MediaSession actual | Sin sesión; ocultar controles inválidos |
| RadioAdapter | Abrir radio; estación/frecuencia/acciones opcionales | NavRadio+ MAIN/LAUNCHER + MediaSession | Solo acceso si está instalada; sin comandos FM inventados |
| VehicleDataProvider | Eventos con fuente, edad y verificación | UnsupportedVehicleDataProvider | Campos ocultos/no disponibles |
| ExteriorTemperatureProvider | Temperatura exterior física, unidad, origen y frescura | UnsupportedExteriorTemperatureProvider | Sin dato; nunca meteorología/CPU/batería |
| ThemeProvider | Modo claro/oscuro/auto, colores, adornos y matrícula | Configuración existente | Paleta y tema predeterminados seguros |
| DisplayProfileProvider | Área útil, dp, densidad, insets, orientación | Recursos y WindowMetrics compatibles | Mantener límites legibles, sin forzar wm |

Cada muestra debe llevar fuente, edad y validez; las verificaciones se ligan a dispositivo/versión y función. Los adaptadores no se seleccionan por detectar un nombre de paquete parecido. Una interfaz futura OEM exige documentación, permisos autorizados y validación específica; su detección no activa conexiones.

## Flujo y adopción gradual

Diagnóstico sanitizado → comparación con requisitos → diseño aprobado → adaptador Android estándar → UI existente. El perfil es entrada de ingeniería, no ejecutor de comandos ni detector mágico de CANBUS. No se conecta a servicios Binder durante el arranque para probar qué ocurre.

Primero extraer contratos alrededor de proveedores existentes, con cambios separados y pruebas equivalentes. Después conectar UI a esos contratos sin alterar layouts/animaciones. Añadir un adaptador específico solo si hay necesidad y evidencia. No introducir módulos, frameworks DI o dependencias para esta preparación.

## Degradación ya existente y propuesta

Ya existen: GPS inválido muestra --; metadatos unidos a una sola sesión; controles no soportados ocultos; apps ausentes no derriban el dashboard; JSON SOURCE dañado se conserva y usa defaults en memoria; imagen ausente muestra símbolo genérico; animaciones respetan ahorro de energía.

Información de vehículo y temperatura exterior no tienen integración de producción: permanecen fuera de la UI funcional. Para nuevos perfiles se documenta ocultar funciones no soportadas tras una adaptación autorizada. No se añade un motor runtime de capacidades sin aprobar primero este diseño.

Para resoluciones nuevas se probarán área útil, aspect ratio, dp, tamaños táctiles, texto y recorte Canvas. La vista actual tiene variantes compactas, pero eso no certifica verticales, pantallas ultrapanorámicas o múltiples displays.
