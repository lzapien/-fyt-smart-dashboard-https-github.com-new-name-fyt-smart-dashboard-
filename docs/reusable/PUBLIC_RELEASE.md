# Preparación pública y revisión final

El proyecto original es privado y conserva su funcionamiento. La distribución se crea mediante una **lista permitida** de archivos, no copiando toda la carpeta y confiando solo en .gitignore.

```sh
python3 tools/prepare_public.py
```

Crea distribution/public, se niega a sobrescribirla y no toca app/ del original. Para otra revisión usa --name con un nombre nuevo. Copia código, recursos runtime, Wrapper, pruebas, herramientas, documentación nueva y ejemplo FYT sanitizado. Sustituye el default personal de matrícula por **TU-PLACA** y actualiza sus fixtures/textos de prueba; no elimina el campo ni su mecanismo de ajustes.

Los recursos raster del vehículo de referencia se retiraron de la copia pública. GenericVehicleArtwork dibuja un SUV sin marca mediante Canvas, con matrícula editable aparte. El preview público se regeneró a partir de ese código; no es una captura Android. La exportación debe partir de esta copia revisada y no regenerarse desde el original privado, pues reintroduciría los recursos anteriores.

## Fuera de la copia

Informes históricos privados, artifacts, local.properties, .idea, .gradle, builds, APK previas, capturas del estéreo, ZIPs de respaldo, perfiles privados, claves y credenciales. Los scripts originales de un solo equipo no se exportan: contenían IP fija y ejecución de instalación. Las nuevas herramientas exigen host/puerto/objetivo y no instalan.

El default genérico de una copia pública no sobrescribe preferencias del estéreo existente. No se instala la copia pública en ese estéreo. Una persona que ya guardó una matrícula conserva su dato privado hasta cambiarlo manualmente.

## Auditoría de privacidad

```sh
python3 tools/public_audit.py --root distribution/public
```

El responsable puede añadir --private-values-file con una lista JSON local de valores personales conocidos. Esa lista **no se copia al repositorio ni al reporte**, que solo nombra archivo y motivo. Se revisan default genérico, archivos de claves/configuración, rutas personales, direcciones privadas y material de clave. El análisis de texto no es OCR ni revisión jurídica.

El escáner excluye directorios generados (.gradle, build, artifacts, __pycache__) y .git como archivos; si hay Git, sí revisa objetos alcanzables, incluidos blobs y mensajes de commit. Hay que revisar también nombres de autor/correo de los commits antes de publicar: no se anonimizan automáticamente y solo los datos conocidos pueden buscarse explícitamente. No es un buscador universal de secretos; revisión visual/manual y lista de exportación siguen siendo necesarias.

La carpeta original no tenía Git. La nueva copia puede comenzar con un Git local vacío, **sin importar el historial privado**. No se fabrica identidad de autor ni se crea un commit en nombre del usuario. No se añaden remotos ni se ejecuta git push.

Si un historial a publicar contiene datos privados, no lo subas ni pretendas ocultarlos con un commit posterior. Conserva el original privadamente y genera una copia/historia inicial limpia tras revisión y aprobación. Reescribir una historia ya publicada exige otro plan; no se hace aquí.

## Canal de esta entrega: fuentes y documentación

El usuario eligió compartir únicamente el proyecto y su documentación para que otras IA puedan reproducirlo. El ZIP no incluye APK/AAB, claves, cachés, builds, historial Git o perfiles privados. La firma pública no es un requisito para distribuir estas fuentes. El pendiente de atribución meteorológica y las pruebas físicas quedan documentados para futuras distribuciones de binarios.

Consultar AI_REPRODUCTION.md y prompts/00-replicate-source.md. La verificación de esta entrega incluye integridad ZIP, manifiesto SHA-256 por archivo y auditoría de fuentes, sin ejecutar Gradle o conectar dispositivos. La autorización para entregar un archivo local no implica crear un repositorio remoto ni publicar en una cuenta.

## Antes de un primer commit público

1. Revisar el reporte de archivos y la comparación de la aplicación original.
2. Comprobar LICENSE Apache-2.0, NOTICE con Copyright 2026 Luis Zapien y THIRD_PARTY_NOTICES.md; conservar licencias de terceros y auditar cualquier recurso nuevo. La selección de licencia propia ya está autorizada y aplicada.
3. Revisar todos los archivos públicos y el default genérico; no añadir capturas personales.
4. Para entregar solo fuentes, verificar ZIP, manifiesto e integridad de recursos/configuración; registrar pruebas de compilación anteriores como históricas. No generar APK para este canal.
5. Si se solicita después distribución binaria, compilar y probar una instalación aislada con autorización. Los previews Java2D incluidos son renders locales, no capturas Android.
6. Elegir una identidad de commit pública y revisar git status/diff antes de agregar archivos. No se proporciona un correo inventado.
7. Obtener autorización explícita para crear el repositorio remoto/publicar, firmar una Release o instalar. Son decisiones distintas.

No ejecutar git add . en el proyecto privado ni publicar un ZIP completo de esa carpeta. No declarar una APK lista para distribución mientras haya pendientes de firma, atribución meteorológica o validación de plataforma. Para compartir estas fuentes, conservar esos pendientes explícitos y verificar el paquete sin binarios.

## Revisión de firma y licencias

La auditoría actual y los pasos que requieren autorización están en [FINAL_PUBLICATION_AUDIT.md](FINAL_PUBLICATION_AUDIT.md). Release ya compila, pero está sin firma; la plantilla externa está propuesta y no activa. No crear o publicar otro repositorio ni generar claves hasta que el propietario apruebe esas acciones concretas.
