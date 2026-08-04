# Tasks: Limpieza de referencias con Cloud Function

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Backend (nuevo)
- [x] Crear `functions/` con TypeScript: `package.json` (Node 22), `tsconfig.json`, `.gitignore`
- [x] `functions/src/index.ts`: manejador `removeVehicleFromMembers`
- [x] Escribir en la **misma base de datos** que disparó el evento
- [x] Leer con `getAll` y actualizar **solo los documentos existentes**
- [x] Exportar los dos triggers (`(default)` y `apark-at`) en `europe-west4`
- [x] Importar el logger desde `firebase-functions/logger`, no desde el barril `v2`

## Configuración
- [x] Bloque `functions` en `firebase.json` con `predeploy` que compila
- [x] Instalar dependencias y compilar (`npm run build`)

## App
- [x] Sin cambios: ni Kotlin ni reglas de Firestore (no-objetivo de la spec)

## Documentación
- [x] Entrada en `CHANGELOG.md`
- [x] Nota en `AGENTS.md`: el proyecto ya tiene backend, con la región y el paso de compilación
- [x] Pasar la spec a *Aprobada* y cerrar sus preguntas abiertas

## Verificación
- [x] El código compila y el módulo carga sin errores
- [x] Desplegado en ambas bases de datos
- [x] Criterio 1: verificado borrando el documento **sin intervención del cliente**; ambas
      listas quedaron limpias en el mismo batch (log: `members:2, cleaned:2`)
- [x] Criterio 2: la función existe y se ejecuta para las dos bases de datos
- [x] Criterio 3: la app se comporta igual que antes
- [~] Regresión: no probada explícitamente. Con un solo miembro la función hace exactamente
      lo mismo con la lista del dueño, y `arrayRemove` es idempotente, así que coincide con
      lo que el cliente ya hacía
