# ADB inalámbrico · macOS, Windows y Linux

Usar solo un equipo propio y estacionado, en una red local confiable. No se presume que el fabricante permita ADB. Si lo bloquea o no expone un método documentado, la salida correcta es **no disponible**, sin root, códigos secretos, aplicaciones auxiliares o modificaciones OEM.

## 1. Platform Tools y red

Obtén Platform Tools del [SDK Manager o paquete oficial](https://developer.android.com/tools/releases/platform-tools). Verifica `adb version`. Las herramientas del proyecto son Python estándar y lanzadores sh/PowerShell; consulta [Inicio rápido](QUICKSTART.md) para PATH y JDK. Windows puede requerir un driver OEM para USB; eso no habilita ADB por Wi-Fi. Linux puede requerir permisos USB/udev si se usa el cable; no ejecutar ADB como root. [Preparación oficial del equipo](https://developer.android.com/studio/run/device).

La IP se obtiene de los ajustes Wi-Fi/Ethernet del estéreo o de la lista de clientes del router, identificando manualmente el equipo. No recolectes ni publiques MAC, SSID o el inventario del router. DHCP puede cambiar la dirección: ningún script conserva la IP de referencia. La herramienta no recorre subredes.

Para descubrir servicios anunciados y puertos: `python3 tools/dashboard.py discover`. Es una consulta mDNS y puede contener identificadores; se muestra en la terminal y no se incorpora al perfil. No encontrar anuncios no demuestra ausencia de ADB: el firmware antiguo o la red pueden no anunciarlos. Confirma host/puerto con la pantalla del equipo. El diagnóstico, una vez conectado, consulta solo propiedades concretas de puertos TCP/TLS; no supone que esos valores estén escuchando.

## 2. Android 10 y anteriores

Dos casos distintos:

- **FYT de referencia**: se utilizó el servicio ADB TCP que el firmware ya tenía habilitado por un procedimiento autorizado del propietario. Se conectó al endpoint mostrado/comprobado; no se modificaron MCU/CANBUS ni servicios. Es un caso de ese firmware, no una instrucción universal para otros modelos.
- **Flujo Android estándar por USB**: habilitar depuración en opciones oficiales, conectar cable compatible y aceptar RSA. Cambiar a TCP con `adb tcpip` modifica el servicio ADB del dispositivo y no pertenece al diagnóstico de lectura. Solo se documenta para una autorización independiente; nuestros scripts no lo ejecutan. Algunos estéreos tienen puertos USB host y no permiten este flujo. [Procedimiento oficial](https://developer.android.com/tools/adb#wireless-adb-android-10).

Ejemplo manual, **solo tras autorización específica para activar TCP**:

```sh
adb devices -l
# USB_DEVICE: transporte USB exacto; PORT: puerto escogido/autorizado.
adb -s "$USB_DEVICE" tcpip "$PORT"
```

Si no existe canal USB autorizado ni ADB TCP habilitado por el fabricante, detenerse. Nunca usar setprop, su, adb root, reinicios o instalaciones auxiliares para eludirlo.

Con un endpoint válido y habilitado:

```sh
python3 tools/dashboard.py network --host "$HOST" --port "$PORT"
python3 tools/dashboard.py connect --host "$HOST" --port "$PORT"
adb devices -l
adb -s "$DEVICE" get-state
```

HOST/PORT/DEVICE se introducen según [Inicio rápido](QUICKSTART.md). No hay valor predeterminado ni se presupone puerto 5555. TCP abierto no prueba que el servicio sea ADB o que esté autorizado; ping fallido no excluye un ADB accesible.

## 3. Android 11 o posterior, cuando el fabricante lo exponga

Abre manualmente Opciones de desarrollador → Depuración inalámbrica → Emparejar mediante código. Usa la IP y **puerto de emparejamiento** mostrados. Ejecuta desde una terminal local interactiva:

```sh
python3 tools/dashboard.py pair --host "$HOST" --port "$PAIR_PORT"
```

Introduce el código únicamente cuando ADB lo solicite. No lo pases como argumento, variable de entorno, archivo, prompt de Codex o captura pública. Después consulta la dirección/puerto de **conexión** (puede ser otro) y ejecuta connect; valida con devices y get-state. No basta con tener SDK 30+: la implementación del fabricante y el tipo de Android pueden limitar esta función. [Emparejamiento oficial](https://developer.android.com/tools/adb#connect-to-a-device-over-wi-fi).

## 4. Diagnóstico de fallos

| Síntoma | Revisar manualmente | Acción del script |
|---|---|---|
| No route to host | Auto encendido, IP/DHCP actual, interfaz/VPN, ruta entre redes, aislamiento de clientes | Informar; no probar otra IP |
| Connection refused | Puerto incorrecto o servicio apagado; TCP rechazado no se arregla instalando APK | Informar; no reiniciar servicios |
| Timeout | Firewall, IoT/guest VLAN, aislamiento AP, enlace dormido; permiso de red local del ordenador | Tiempo límite; sin escaneo de red |
| offline | Android suspendido o transporte obsoleto | Detener consultas; pedir revisar la pantalla y reconectar ese endpoint |
| unauthorized | Autorización RSA pendiente o emparejamiento incompleto | Detenerse; aceptación física por el propietario |
| Ping falla, TCP funciona | Puede estar bloqueado ICMP | Continuar validación ADB, no diagnosticar red caída solo por ping |
| TCP abre, ADB falla | Servicio distinto, autorización o protocolo no coincidente | No asumir compatibilidad |
| mDNS vacío | Anuncios no disponibles o filtrados; no concluye que el equipo sea incompatible | Introducir endpoint confirmado por el usuario |
| Operation not permitted | Permisos locales, aplicación terminal/Codex, firewall o entorno de ejecución | Pedir permiso acotado; no cambiar seguridad automáticamente |

macOS: revisa manualmente permisos de Red local para la aplicación que ejecuta la terminal, cuando el sistema los ofrezca; el proceso adb puede depender de ese contexto. No desactives el firewall como solución automática. Windows/Linux: verifica reglas de salida, resolución de nombres y multicast con el administrador si procede. Redes IoT/guest pueden impedir comunicación entre clientes aunque ambos tengan Internet; estar en el mismo SSID no garantiza conectividad directa. No se cambian redes, router, firewall o políticas mediante nuestras herramientas.

No ejecutar adb kill-server ni disconnect global como recuperación rutinaria: afectaría otros dispositivos. Cualquier desconexión debe ser explícita y limitada al objetivo.

## 5. Terminar la sesión

```sh
adb disconnect "$DEVICE"
```

Es una acción manual del propietario, solo contra ese endpoint. No apaga ADB en el estéreo. Si lo habilitaste temporalmente, desactívalo o revoca la autorización desde su interfaz oficial al terminar, de forma consciente. No expongas ADB TCP a Internet, no crees reenvíos de puertos y no uses redes públicas para esta prueba.

Las consultas remotas siempre llevan `-s`. version, devices, mdns, pair y connect son operaciones del cliente/selección de transporte y no equivalen a enviar un comando shell a todos los equipos.
