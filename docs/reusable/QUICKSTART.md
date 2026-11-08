# Inicio rápido

Instala Git, JDK 17, Python 3 y Android Studio o las herramientas Android de línea de comandos. Se recomienda Python mantenido 3.11+; scripts compatibles con 3.9+. Instala SDK Platform 35, Build Tools 34.0.0 (predeterminado de AGP 8.7) y Platform Tools; Build Tools 35 también puede usarse para las utilidades de firma. La primera compilación necesita Internet para descargar Gradle/AGP y aceptar licencias. Nuestros scripts no instalan aplicaciones Android ni preparan el SDK por su cuenta; Gradle puede solicitar componentes SDK faltantes según su funcionamiento normal.

Descarga [Platform Tools oficial por sistema](https://developer.android.com/tools/releases/platform-tools), descomprime y añade su carpeta a PATH. No necesitas Fastboot ni desbloquear el bootloader.

| Sistema | Verificar |
|---|---|
| macOS/Linux | command -v adb; adb version; python3 --version; java -version |
| Windows PowerShell | Get-Command adb; adb version; py -3 --version; java -version |

Si ADB no está en PATH: `python3 tools/dashboard.py --adb RUTA/adb doctor` o `py -3 tools/dashboard.py --adb RUTA/adb.exe doctor`.

En macOS selecciona JDK 17 con `export JAVA_HOME="$(/usr/libexec/java_home -v 17)"` y antepone `$JAVA_HOME/bin` al PATH. En Windows/Linux configura JAVA_HOME/PATH según el instalador; Android Studio debe usar el mismo JDK. No instales Gradle global: usa el Wrapper incluido.

## Empezar desde el ZIP de fuentes

Extrae el proyecto completo y abre README.md. Otra IA puede usar docs/reusable/AI_REPRODUCTION.md y prompts/00-replicate-source.md sin tener acceso al estéreo. Para lectura y propuestas no hace falta ejecutar Gradle ni generar APK; las herramientas de compilación se preparan cuando el receptor solicite esa etapa.

## Proyecto y Codex

Cuando exista una URL GitHub aprobada, clona ese repositorio o descarga su ZIP. Hoy no se inventa una URL. Para la copia preparada localmente, utiliza la carpeta indicada en el informe. No distribuyas la carpeta privada.

Abre la raíz pública con Codex local y verifica que lea AGENTS.md. Instala o configura la aplicación, extensión o CLI siguiendo la [guía oficial de Codex](https://learn.chatgpt.com/docs/codex/cli) y el [inicio de la aplicación](https://learn.chatgpt.com/docs/quickstart). El acceso al estéreo ocurre desde tu ordenador; Codex Cloud no tiene acceso implícito a tu Wi-Fi. La app Android no requiere una API key de OpenAI.

## Conectar y diagnosticar

Con el vehículo estacionado, confirma que el fabricante permite ADB y abre manualmente sus opciones autorizadas. No utilices códigos secretos ni bypass. Identifica host y puerto reales; sigue [ADB inalámbrico](WIRELESS_ADB.md).

macOS/Linux, desde la raíz:

```sh
read -r -p 'IP o nombre del estéreo: ' HOST
read -r -p 'Puerto ADB mostrado: ' PORT
python3 tools/dashboard.py doctor
python3 tools/dashboard.py connect --host "$HOST" --port "$PORT"
adb devices -l
read -r -p 'Objetivo exacto de adb devices -l: ' DEVICE
python3 tools/dashboard.py profile --device "$DEVICE" --output profiles/private/mi-equipo
```

Estas líneas usan bash; en zsh introduce las variables con `read 'HOST?IP: '` / `read 'PORT?Puerto: '` o usa directamente los argumentos sin pegar una IP de otro usuario. El lanzador tools/dashboard.sh es POSIX y no depende de esos prompts.

PowerShell (no usar $Host, que es reservado):

```powershell
$HostAddress = Read-Host 'IP o nombre del estéreo'
$AdbPort = Read-Host 'Puerto ADB mostrado'
py -3 tools/dashboard.py connect --host $HostAddress --port $AdbPort
adb devices -l
$DeviceId = Read-Host 'Objetivo exacto de adb devices -l'
py -3 tools/dashboard.py profile --device $DeviceId --output profiles/private/mi-equipo
```

El puerto de emparejamiento puede ser distinto del de conexión. Unauthorized requiere aceptar la autorización en el equipo; si no puedes, detente. La herramienta nunca activa ADB ni modifica firewall.

## Personalizar y compilar

Entrega a Codex ambos informes y prompts/01-audit.md. Primero aprueba un plan y un cambio pequeño. Matrícula, adornos y modo de pantalla ya se editan manteniendo SOURCE desde el dashboard, sin código.

```sh
python3 -m unittest discover -s tests/platform -v
python3 tools/run_host_tests.py --sdk "$ANDROID_HOME"
python3 tools/build.py debug --sdk "$ANDROID_HOME"
```

En Windows usa `py -3`. Hay lanzadores tools/dashboard.ps1 y tools/dashboard.sh; si PowerShell restringe scripts, usa Python sin modificar la política de ejecución. Alternativa: gradlew.bat assembleDebug o ./gradlew assembleDebug. Offline solo con dependencias ya descargadas. Compilar no instala.

Lee [firma e instalación manual](BUILD_SIGNING.md) antes de transferir APKs. Conserva el launcher OEM. No cambies HOME en la primera prueba.
