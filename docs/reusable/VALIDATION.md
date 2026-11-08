# Validación de la preparación reutilizable

Fecha: 5 de noviembre de 2026. Entorno ejecutado: macOS, JDK 17 y Python 3.9; herramientas de diagnóstico sin dependencias Python externas.

| Verificación | Resultado |
|---|---|
| Integridad app/ y configuración Android original | 73 archivos comparados con respaldo, idénticos |
| Producción de copia pública | Matrícula y vehículo genéricos; sustitución gráfica autorizada, funciones conservadas |
| APK Debug pública | BUILD SUCCESSFUL, firma Debug verificada, paquete 1.1.2/21 |
| Suites Java en host | 14, correctas |
| Herramientas de perfil/seguridad | 17 tests, correctos; ADB simulado |
| CLI profile completa | Ejecutada con falso ADB local; generó JSON/Markdown válidos sin conexión real |
| JSON Schema | Draft 2020-12, perfil simulado y ejemplo histórico válidos; validación con jsonschema ya instalado, no dependencia nueva del CLI |
| Canvas | 265 comprobaciones tras el vehículo genérico; preview local, no captura Android |
| Instrumentación | APK de pruebas compilada, no instalada ni ejecutada |
| Lint Debug/Release público actual | 0 errores, 21 advertencias: ExpiredTargetSdkVersion Warning; ninguna nueva por el vehículo genérico |
| Release público actual | BUILD SUCCESSFUL; APK sin firma, pendiente de clave pública y validación de distribución |
| Privacidad de copia fuente | Sin valor personal conocido, IP privada fija, rutas locales, claves o informes privados detectados |
| Historial | Git local vacío, sin historial privado importado ni remotos |
| Windows/Linux nativos | Pendiente; código portable y tests simulados no equivalen a ejecución nativa |

No se omitió Lint, no se firmó Release ni se modificaron SDK/dependencias. No se conectó el estéreo, instaló APK, cambió HOME o publicó un remoto durante esta preparación.

La revisión anterior del PNG maestro y WebP correspondía al vehículo de referencia. Esos tres archivos se retiraron de esta copia pública y se sustituyeron por GenericVehicleArtwork sin imágenes de fabricantes. El preview usa ahora esa geometría y matrícula genérica. Véase GENERIC_VEHICLE.md para el alcance y la validación de este cambio.

Antes de publicar: conservar la licencia Apache-2.0 aplicada y los avisos de terceros, aprobar la firma Release externa y cerrar la atribución meteorológica, probar sistemas operativos y hardware adicionales y obtener capturas Android genéricas en una prueba aislada autorizada. No se afirma compatibilidad universal ni una Release de producción aprobada.

La configuración de firma propuesta no se activó. Ver [auditoría final](FINAL_PUBLICATION_AUDIT.md) y [plan de firma](PUBLIC_SIGNING_PLAN.md). La migración de targetSdk sigue pendiente solo si se decide modernizar compatibilidad o acceder a Google Play; ya no es necesaria para desbloquear el ensamblado local actual.

Actualización 2026-11-06: Debug, Release, instrumentación compilada y Lint Debug/Release verificados tras la sustitución gráfica. 14 suites Java y 17 tests de herramientas correctos. Detalle en GENERIC_VEHICLE.md; no se ejecutó Android ni se instaló nada.

Actualización de licencia 2026-11-06: Apache-2.0 aplicada, Copyright 2026 Luis Zapien. LICENSE/NOTICE/avisos incluidos y verificados en Debug/Release; ambas compilaciones y Lint completos correctos (0 errores, 21 advertencias previas). Exportador conserva los siete archivos legales esperados; 17 tests de herramientas correctos. Sin cambios de Java, gráficos, SDK, permisos o firma. Ver PUBLIC_LICENSE.md.
