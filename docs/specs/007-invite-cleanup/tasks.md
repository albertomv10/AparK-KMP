# Tasks: Caducidad real de las invitaciones

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Infraestructura — lo hace Alberto en la consola
- [ ] Política TTL en **`apark-at`**: grupo `invites`, campo `expiresAt`, desfase **7 días**
- [ ] Política TTL en **`(default)`**: los mismos valores
- [ ] Comprobar que la política queda en estado activo en ambas

## Código
- [ ] `cleanupVehicleReferences`: borrar también las invitaciones cuyo `vehicleId` sea el del
      vehículo eliminado, en lote y dentro del mismo trigger
- [ ] Desplegar (el trigger está exportado dos veces, una por base de datos)

## Documentación
- [ ] Corregir la **decisión 4** del diseño de la [spec 005](../005-share-vehicle/design.md), que
      dice que no hace falta TTL
- [ ] Nota en `AGENTS.md`: la política TTL no vive en el repositorio y hay que recrearla a mano en
      cualquier base de datos nueva
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] **Criterio 4**: borrar un vehículo que tenga invitación viva → desaparece de `invites` en el
      acto, sin esperar a la caducidad
- [ ] **Criterio 3**: con una invitación caducada hace menos de 7 días, meter el código → sigue
      diciendo *"esa invitación ha caducado"*, no *"no es válido"*
- [ ] **Criterios 1 y 2**: pasada la ventana, comprobar que `QRAGPSM7` (release, caducada el
      2026-08-05) y las muertas de `apark-at` han desaparecido. Requiere esperar: Firestore borra
      dentro de las 24 h siguientes al vencimiento, y aquí el vencimiento lleva 7 días de desfase
- [ ] **Criterio 6**: `invites` sigue cerrada al cliente; el diff de `firestore.rules` debe estar
      vacío

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
