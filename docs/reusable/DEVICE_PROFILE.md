# Perfil del dispositivo y privacidad

```sh
python3 tools/dashboard.py profile --device "$DEVICE" --output profiles/private/mi-equipo
```

DEVICE es un transporte ya autorizado y en estado device. No se elige el primer equipo ni se habilita ADB. El comando escribe **device-profile.json** y **device-report.md** en la carpeta local indicada y se niega a sobrescribir diagnósticos anteriores. Cada ejecución puede usar una carpeta nueva. Se aplica permiso local 0600 cuando el sistema lo admite. Revisa ambos archivos antes de compartirlos.

## Datos permitidos

| Área | Fuente limitada | Qué se guarda |
|---|---|---|
| Fabricante/modelo/Android | Propiedades explícitas de ro.product, ro.hardware, SDK y plataforma | Modelo técnico, versión y plataforma; no fingerprint/serial |
| CPU | SoC/board y ABI declaradas | Arquitecturas; sin leer serial de cpuinfo |
| Pantalla | wm size y wm density sin argumentos de escritura | Valores físicos/override y efectivo; no se cambia la pantalla |
| Memoria | /proc/meminfo | Solo MemTotal y MemAvailable en KiB |
| Sensores/GPS | pm list features y ajuste location_mode del usuario actual | Capacidades declaradas y estado del ajuste; sin coordenadas ni últimas ubicaciones |
| Paquetes | Lista PM con versionCode si está soportado | Solo coincidencias radio/navimods/amazon/waze/youtube/maps/music/syu/fyt/canbus |
| HOME | Resolver MAIN/HOME, solo lectura | Componente resuelto; no se selecciona otro launcher |
| Servicios | service list y dumpsys -l | Solo nombres multimedia/audio y FYT/SYU/CANBUS/MCU relevantes; ninguna transacción Binder |
| Transporte | Estado de objetivo seleccionado, propiedades concretas TCP/TLS | Tipo de transporte y puertos declarados; sin IP/serial del endpoint |

No se usa getprop completo, dumpsys location, dumpsys media_session, dumpsys battery/thermal, logcat, cuentas, almacenamiento compartido, datos privados de apps, notificaciones, VIN o credenciales. Las consultas de metadatos pueden devolver inventarios más amplios en memoria: los campos no pertinentes se descartan, no se guardan volcados crudos. El reporte escapa contenido recibido y no lo convierte en órdenes.

## Evidencia y degradación

- **verified**: prueba concreta y acotada, por ejemplo get-state devolvió device al comenzar. No implica que todos los componentes estén verificados.
- **detected**: declaración o metadato observado. Un feature GPS no confirma recepción ni precisión; un servicio media no confirma controles.
- **unknown**: falta prueba, acceso denegado, comando no soportado o dato ausente. No se inventa un cero.
- **unavailable**: reservado para una indisponibilidad realmente establecida, documentando su causa. No se deduce solo de permiso denegado.

Las consultas fallidas producen query/reason sin guardar errores crudos o rutas del usuario. El perfil puede quedar parcial, con los límites explícitos. Si falta autorización ADB o el dispositivo está offline, se detiene antes de shell.

La brújula actual usa rumbo GPS mientras hay movimiento fiable: no promete orientación magnética al estar detenido. La temperatura exterior y la información del vehículo quedan unknown aunque aparezca un servicio OEM. Verificar esos datos exige una interfaz documentada y comparación con una medición real; nunca se invocan métodos desconocidos.

## Formato

[JSON Schema](../../schemas/device-profile.schema.json), versión 1. El ejemplo [FYT UIS7862](../../profiles/examples/fyt-uis7862.json) es histórico y sanitizado: no contiene un endpoint utilizable ni cuenta como diagnóstico vivo. No apliques automáticamente ajustes basándote en él.

La compatibilidad del perfil no altera el APK: primero Codex lo revisa, inspecciona clases y presenta adaptaciones. Las futuras interfaces de capacidades se describen en [Arquitectura](ARCHITECTURE.md).

Prueba local sin dispositivo: `python3 -m unittest discover -s tests/platform -v`. Utiliza dobles de ADB; no ejecuta comandos reales sobre el estéreo.
