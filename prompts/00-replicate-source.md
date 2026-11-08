# Prompt inicial para cualquier asistente de programación

Trabaja sobre la raíz extraída de FYT Smart Dashboard que te proporcioné. Primero confirma qué archivos puedes leer; si no tienes herramientas locales, no afirmes que modificaste, ejecutaste o probaste algo.

Lee README.md, AGENTS.md y docs/reusable/AI_REPRODUCTION.md. Después revisa BASELINE.md, ARCHITECTURE.md, COMPATIBILITY.md, VALIDATION.md, LICENSE, NOTICE y THIRD_PARTY_NOTICES.md. Inspecciona el código real de MainActivity, SourceManager, GPS, multimedia, clima, temas, GenericVehicleArtwork y sus recursos. Distingue código existente de arquitectura propuesta y pruebas locales de pruebas en Android real.

Mi primera petición es solo analizar el código fuente y proponer cómo reproducir o adaptar este dashboard a mi equipo. No ejecutes Gradle ni generes APK, no firmes, no instales, no publiques y no te conectes por ADB todavía. No modifiques archivos hasta que autorice un cambio concreto. No uses root, no modifiques firmware, MCU, CANBUS, aplicaciones OEM ni HOME.

Si SOURCE_MANIFEST.json está presente, comprueba integridad de los archivos que enumera. Conserva las licencias: aportaciones propias Apache-2.0, Copyright 2026 Luis Zapien; terceros mantienen sus condiciones.

Si no te di un perfil real de mi dispositivo, trabaja localmente y declara sus capacidades desconocidas. El ejemplo FYT incluido no es mi equipo ni una garantía de compatibilidad. Explica qué datos mínimos necesitarás después y cómo obtenerlos mediante el diagnóstico documentado, solo cuando lo autorice.

Entrega: estructura, versiones reales, clases principales, dependencias, funciones disponibles y pendientes, riesgos y plan mínimo por archivos. Conserva el cockpit, GPS real, multimedia, clima, reloj, temas, cinco accesos y App Drawer. No inventes paquetes, interfaces, mediciones o resultados. Espera mi autorización para implementar el plan.
