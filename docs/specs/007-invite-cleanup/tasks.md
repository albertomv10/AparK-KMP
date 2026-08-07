# Tasks: Caducidad real de las invitaciones

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Infraestructura — lo hace Alberto en la consola
- [x] Política TTL en **`apark-at`** y en **`(default)`**: grupo `invites`, campo `expiresAt`.
      Aplicadas por Alberto. **El asistente no puede verificarlas**: `firebase-tools` no expone
      configuración de TTL y `gcloud` no está instalado

## Código
- [x] `cleanupVehicleReferences`: borra también las invitaciones cuyo `vehicleId` sea el del
      vehículo eliminado, en lote
- [x] El borrado de invitaciones **no queda detrás del `return` temprano** de la limpieza de
      miembros: solo necesita el id de los parámetros del evento, no los datos del documento. Los
      dos van en `Promise.all`, de modo que un fallo en uno no cancela el otro
- [x] Desplegado en las dos bases (el trigger está exportado una vez por base de datos)

## Documentación
- [x] Corregida la **decisión 4** del diseño de la [spec 005](../005-share-vehicle/design.md)
- [x] Nota en `AGENTS.md`: la política TTL no vive en el repositorio
- [x] Entrada en `CHANGELOG.md`

## Verificación
- [x] `npm --prefix functions run build` limpio
- [x] **Criterio 4**: probado con documentos sintéticos en `apark-at` — un vehículo
      `ZZTEST-cleanup-vehicle` con una invitación `ZZTEST999` apuntándole. Al borrar el vehículo, la
      invitación desapareció y el log lo confirma:
      `{"message":"Deleted invitations for a removed vehicle","deleted":1}`. Las otras 6
      invitaciones quedaron intactas
- [x] La misma ejecución demuestra que **el orden importaba**: el log emitió
      `"No member documents left to clean"` —la limpieza de miembros salió por su `return`
      temprano— y aun así borró la invitación. Detrás de ese `return` no se habría ejecutado
- [x] **Criterio 6**: `firestore.rules` sin tocar; `invites` sigue cerrada al cliente y el Admin
      SDK se salta las reglas
- [ ] **Criterios 1 y 2**: requieren esperar al TTL. Con desfase de 7 días, `PG5CWYQ9` (caducada el
      2026-08-05) debería desaparecer hacia el **2026-08-12**, y `QRAGPSM7` de `(default)` con ella.
      Si desaparecen antes de 24 h, el desfase quedó en 0 y conviene revisarlo
- [ ] **Criterio 3**: meter el código de una invitación caducada hace menos de 7 días y comprobar
      que sigue diciendo *"esa invitación ha caducado"*, no *"no es válido"*

## Estado de los datos antes de aplicar nada
Tomado el 2026-08-07, para poder comprobar después qué se ha ido:

| Base | Código | Vehículo | Estado |
|------|--------|----------|--------|
| `apark-at` | `279XVTRB` | `bfTS7hAL…` | sin usar, **caducada** (adelantada a mano en la prueba) |
| `apark-at` | `8DDKYWZN` | `bfTS7hAL…` | **usada** el 2026-08-06 |
| `apark-at` | `PG5CWYQ9` | `RbkaL9hV…` | **usada** el 2026-08-04 |
| `apark-at` | `AESV4D89` | `jS14oiPo…` | viva |
| `apark-at` | `JM3CND6G` | `RbkaL9hV…` | viva |
| `apark-at` | `UCTXAXJZ` | `WgLVw8Bc…` | viva |
| `(default)` | `QRAGPSM7` | `sWxRMqoT…` | sin usar, **caducada** el 2026-08-05 |
