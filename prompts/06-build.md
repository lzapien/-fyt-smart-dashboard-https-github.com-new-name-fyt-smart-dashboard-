# Compilar y verificar sin instalar

Lee AGENTS.md. Verifica JDK 17/SDK, conserva Wrapper/dependencias y compila Debug con tools/build.py o el Wrapper existente. Ejecuta pruebas locales pertinentes. Si falla, distingue errores previos y nuevos; no suprimas Lint ni actualices SDK automáticamente. Informa paquete, versión, ruta, SHA-256, firma y límites de las pruebas. No firmes Release con una clave del usuario, no instales ni cambies HOME. Propón una prueba aislada que yo autorizaré por separado.
