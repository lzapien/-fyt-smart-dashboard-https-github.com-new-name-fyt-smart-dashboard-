# Guion de video · del proyecto a tu propio dashboard

Duración sugerida 12–15 minutos. Vehículo estacionado. No grabar emparejamiento, red privada, cuentas, matrícula personal o registros crudos. Usar la copia pública genérica y un perfil sanitizado; una demostración no confirma compatibilidad con todos los fabricantes.

| Tiempo | Pantalla/acción | Narración |
|---|---|---|
| 00:00–01:00 | Cockpit genérico, velocímetro, SUV, cinco accesos | «Partimos de la misma app Android funcional, conservando diseño y animaciones. Las funciones dependen del hardware y apps reales». |
| 01:00–02:00 | Página GitHub aprobada o copia local si no existe publicación | «Clona o descarga el proyecto revisado. No hay que copiar datos o claves del autor. Si aún no existe URL, no se presenta una página ficticia». |
| 02:00–03:30 | Documentación de herramientas y Codex | «Instala Platform Tools, JDK 17, SDK Platform 35 y Python. Abre esta carpeta con Codex local y comprueba AGENTS.md». |
| 03:30–05:00 | Ajustes oficiales Android, sin mostrar código/IP privada | «Android 10 suele necesitar USB inicial o un ADB TCP que el fabricante ya permita. Android 11+ puede ofrecer pairing. No todos los estéreos lo soportan y no se usa root». |
| 05:00–06:00 | Terminal con host/puertos introducidos fuera de cámara | «Connect verifica tu endpoint; el puerto de pairing puede ser otro. Validamos el dispositivo exacto antes de enviar consultas». |
| 06:00–07:30 | Ejecutar profile y abrir JSON/Markdown sanitizados | «El perfil obtiene hardware, pantalla, metadatos y capacidades declaradas. Detectado no equivale a verificado. No se recopilan coordenadas o contenido multimedia». |
| 07:30–09:00 | Codex lee perfil y presenta plan con archivos/pruebas | «Autorizamos un cambio pequeño, por ejemplo colores. Si la temperatura física no existe, se oculta: no se inventan mediciones». |
| 09:00–10:00 | SOURCE → ajustes de matrícula y temas | «Muchas personalizaciones ya están disponibles sin código. La copia pública usa un valor genérico; cada dueño configura el suyo». |
| 10:00–11:00 | Pruebas locales y tools/build.py debug | «Compilar no instala. Pruebas simuladas no equivalen a señal GPS o controles reales. Se documentan errores preexistentes de Lint». |
| 11:00–12:30 | Firma y certificado; dispositivo de prueba autorizado | «Revisa paquete, versión, firma y datos antes de instalar manualmente. Cambiar certificado no actualiza una app existente sin más. Conserva HOME OEM». |
| 12:30–14:00 | App de prueba, capturas genéricas y reporte | «Comprueba sensores reales, estado sin señal, controles soportados y tamaños. No se modifica MCU/CANBUS. Publicar o cambiar HOME necesita otra autorización». |
| 14:00–15:00 | Pendientes y fuentes | «Comparte cambios revisados sin perfiles privados ni claves. Consulta las guías oficiales y documenta equipo, versión y límites de las pruebas». |

## Preparación del rodaje

- [Codex CLI oficial](https://learn.chatgpt.com/docs/codex/cli), [inicio de la app](https://learn.chatgpt.com/docs/quickstart), [instrucciones AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md). Instalar desde el canal indicado para el sistema; no se requiere programar con la API OpenAI.
- En el escritorio local abre la raíz de la copia pública. Una sesión cloud no obtiene acceso a la red del automóvil automáticamente.
- Demostrar tools/dashboard.py doctor, network, connect o pair según el equipo, y profile. No escribir endpoints fijos en scripts del video.
- Emplear prompts/01-audit.md, 04-adapt.md y 06-build.md; esperar el plan y dar autorización concreta.
- Para instalación grabar un objetivo de prueba autorizado y usar -s. No grabar ni ejecutar una instalación en el vehículo del autor sin permiso nuevo.
- No grabar claves, códigos RSA/pairing, certificados con identidad personal, cuentas o rutas privadas. No reemplazar la UI por una maqueta HTML.

Capturas reales de una copia genérica se deben obtener en una prueba autorizada posterior. Los renders Canvas locales incluidos en documentación son previews identificados; no afirmar que son evidencia Android ejecutándose.
