# Tasks: detalle del vehículo, miembros y propiedad

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Servidor y reglas
- [x] `requireUid` compartido entre las callables
- [x] `functions/src/members.ts`: `getVehicleMembers`; `memberList` en `membersPlan.ts`, sin imports
- [x] Exportada desde `index.ts`, en `europe-west4`
- [x] `firestore.rules`: el dueño solo transfiere a un miembro, y no puede añadir miembros
- [x] `tools/test/members.test.ts` y casos nuevos en `vehicles.rules.test.ts`, también los denegados

## Dominio y datos
- [x] `VehicleMember`, `MemberRepository` + `FirebaseMemberRepository`
- [x] `VehicleRepository.removeMember`; `updatedAt` al transferir
- [x] `GetVehicleMembersUseCase`, `RemoveVehicleMemberUseCase`; `TransferVehicleOwnershipUseCase` en Koin
- [x] Fuera `shareVehicleWithUser` y `ShareVehicleWithUserUseCase`, y también `getVehicleById` y su caso de uso, que solo usaba el detalle viejo

## Interfaz
- [x] `VehicleDetailViewModel` en tiempo real, con «ya no tienes acceso» y los miembros al día
- [x] `VehicleDetailScreen`: cabecera, dónde está, miembros con sus acciones, compartir, y las acciones en rojo
- [x] «Ver en el mapa»: `TabShell` y `HomeScreen`
- [x] Cadenas en `values`, `values-en` y `values-fr`, con descripciones de accesibilidad

## Tests
- [x] `commonTest`: la lógica pura del cliente

## Documentación
- [x] `AGENTS.md`, `DATA-MODEL.md` (R7 con una callable; el mapa `members` sigue aplazado), `ROADMAP.md` (2.3 y 2.4)
- [x] Entrada en `CHANGELOG.md`

## Verificación

Contra los emuladores, en el simulador de iOS. Los cambios «desde fuera» (pasar la propiedad, quitar a
alguien mientras mira) se hicieron con la API de administración del emulador, como lo haría otro teléfono.
- [x] Compila Android, iOS-Kotlin, tests JVM y nativos, Xcode; `functions` compila; tests de `tools`
- [x] **(1)** Como dueño del compartido: él (Dueño, Tú) y el otro miembro, con su menú
- [x] **(4)** Quitar a un miembro: desaparece de la lista y deja de ver el vehículo
- [x] **(5)(8)** Pasar la propiedad: la pantalla cambia a la de un miembro sin salir
- [x] **(7)** Mirando el detalle, me quitan desde fuera: «Ya no tienes acceso» y vuelta atrás
- [x] **(6)** Reglas: los casos denegados fallan en los tests
- [x] «Ver en el mapa» desde la pestaña Vehículos
- [x] Salirse desde el detalle: cierra la pantalla sin el aviso de «ya no tienes acceso»
- [x] **(9)** Un miembro sin nombre sale con su email
- [x] El aspecto, Android y dos teléfonos a la vez — **Alberto**, contra `apark-dev`
