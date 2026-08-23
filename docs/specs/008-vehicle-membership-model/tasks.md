# Tasks: la pertenencia como consulta

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

Se reparte en **dos PR**. Los pasos operativos van intercalados a propósito: el orden importa y
saltárselo rompe cosas.

## PR A — herramientas y reglas

### Herramientas
- [ ] `tools/` con su `package.json` y `tsconfig.json`, aislado de `functions/`
- [ ] `tools/migrate-member-ids.ts`: backfill idempotente de `memberIds` = `[ownerId] + sharedUsers`
- [ ] `--dry-run` que enseñe qué tocaría sin escribir
- [ ] `--project` obligatorio, para no correrlo contra producción por descuido
- [ ] `--drop-shared-users` para el paso 4, tras bandera explícita

### Reglas Firestore
- [ ] `read` pasa a `memberIds`
- [ ] `create` exige `memberIds == [uid]` **y** `ownerId == uid`
- [ ] Rama de salida sobre `memberIds`, con `uid != ownerId`
- [ ] La rama de `lastLocation` de un compartido sigue funcionando igual

### Tests de reglas (primeros tests reales del proyecto)
- [ ] Emulador de Firestore + `@firebase/rules-unit-testing`
- [ ] Un miembro lee su vehículo; un extraño **no**
- [ ] Crear con `memberIds != [uid]` o `ownerId != uid` → denegado
- [ ] Quitarse a sí mismo → permitido
- [ ] Quitar a otro → **denegado**
- [ ] El dueño por la vía de salida → **denegado**
- [ ] Un compartido escribe `lastLocation` y nada más

### Documentación
- [ ] `docs/FIREBASE.md`: la secuencia operativa de cuatro pasos

## Paso operativo 1 — backfill
- [ ] `--dry-run` contra `apark-dev` y revisar la salida
- [ ] Ejecutar contra `apark-dev`
- [ ] Comprobar con el MCP que **ningún** vehículo queda sin `memberIds`
- [ ] Ejecutarlo **otra vez**: no debe cambiar nada
- [ ] Repetir contra producción

## PR B — cliente y funciones

### Domain
- [ ] `Vehicle`: `memberIds`, `createdAt`, `updatedAt`; fuera `sharedUsers` e `inviteCode`
- [ ] `Vehicle.LocationModel.user` pasa a `{uid, name, email}`
- [ ] `User`: `createdAt` / `updatedAt`
- [ ] `UpdateVehicleLocationUseCase`: dejar de incrustar el `User` completo

### Data
- [ ] `getVehiclesForUser`: consulta `array-contains` + documento propio para el orden, con `combine`
- [ ] **Eliminar el `retryWhen`** y su `catch`
- [ ] `createVehicle` escribe `memberIds = [uid]` y las marcas de tiempo
- [ ] `removeUserFromVehicle` opera sobre `memberIds`
- [ ] `FirestoreConstants`: constante nueva, fuera la vieja

### Funciones
- [ ] `joinWithCodeHandler` escribe `memberIds`
- [ ] El trigger de limpieza lee `memberIds`

### Presentation
- [ ] Comprobar que no hace falta tocar nada: `isOwner` compara con `ownerId`

## Paso operativo 2 — reglas y apps
- [ ] Desplegar reglas a `apark-dev`
- [ ] Instalar la app nueva en los dos dispositivos
- [ ] Probar el ciclo completo contra dev
- [ ] Desplegar reglas a producción
- [ ] Actualizar los dos dispositivos contra producción

## Paso operativo 3 — limpieza
- [ ] `--drop-shared-users` contra `apark-dev`
- [ ] Comprobar que la app sigue funcionando
- [ ] Repetir contra producción

## Documentación
- [ ] Anotar en la [spec 004](../004-stale-cache-cards/spec.md) que queda **mitigada**, no cerrada
- [ ] Actualizar `AGENTS.md`: colecciones, y retirar el gotcha del `PERMISSION_DENIED`
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] Compila Android e iOS-Kotlin, y el proyecto de Xcode
- [ ] CI en verde
- [ ] Los vehículos aparecen **en el mismo orden que antes**
- [ ] Reordenar sigue sin afectar al otro miembro
- [ ] Crear, compartir, unirse, salirse, borrar — en los dos móviles
- [ ] Un id colgante en `userVehicles` no produce `PERMISSION_DENIED` ni reintentos
- [ ] Se cumplen los nueve criterios de aceptación de la spec
