# Tasks: detalle del vehículo, miembros y propiedad

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Servidor y reglas
- [ ] `requireUid` compartido entre las callables
- [ ] `functions/src/members.ts`: `getVehicleMembers`; `memberList` en `membersPlan.ts`, sin imports
- [ ] Exportada desde `index.ts`, en `europe-west4`
- [ ] `firestore.rules`: el dueño solo transfiere a un miembro, y no puede añadir miembros
- [ ] `tools/test/members.test.ts` y casos nuevos en `vehicles.rules.test.ts`, también los denegados

## Dominio y datos
- [ ] `VehicleMember`, `MemberRepository` + `FirebaseMemberRepository`
- [ ] `VehicleRepository.removeMember`; `updatedAt` al transferir
- [ ] `GetVehicleMembersUseCase`, `RemoveVehicleMemberUseCase`; `TransferVehicleOwnershipUseCase` en Koin
- [ ] Fuera `shareVehicleWithUser` y `ShareVehicleWithUserUseCase`

## Interfaz
- [ ] `VehicleDetailViewModel` en tiempo real, con «ya no tienes acceso» y los miembros al día
- [ ] `VehicleDetailScreen`: cabecera, dónde está, miembros con sus acciones, compartir, y las acciones en rojo
- [ ] «Ver en el mapa»: `TabShell` y `HomeScreen`
- [ ] Cadenas en `values`, `values-en` y `values-fr`, con descripciones de accesibilidad

## Tests
- [ ] `commonTest`: la lógica pura del cliente

## Documentación
- [ ] `AGENTS.md`, `DATA-MODEL.md` (R7 con una callable; el mapa `members` sigue aplazado), `ROADMAP.md` (2.3 y 2.4)
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] Compila Android, iOS-Kotlin, tests JVM y nativos, Xcode; `functions` compila; tests de `tools`
- [ ] **(1)** Como dueño del compartido: él (Dueño, Tú) y el otro miembro, con su menú
- [ ] **(4)** Quitar a un miembro: desaparece de la lista y deja de ver el vehículo
- [ ] **(5)(8)** Pasar la propiedad: la pantalla cambia a la de un miembro sin salir
- [ ] **(7)** Mirando el detalle, me quitan desde fuera: «Ya no tienes acceso» y vuelta atrás
- [ ] **(6)** Reglas: los casos denegados fallan en los tests
- [ ] «Ver en el mapa» desde la pestaña Vehículos
- [ ] **(9)** Un miembro sin nombre sale con su email
- [ ] El aspecto, Android y dos teléfonos a la vez — **Alberto**
