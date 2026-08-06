# Design: Compartir vehículo

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado
- **Fecha**: 2026-08-04

## Enfoque

Invitaciones de **un solo uso y 24 h**, creadas y canjeadas por **funciones invocables**, porque
las reglas impiden que un cliente busque un vehículo del que no es miembro o se añada a él.

### Decisiones tomadas
- Envío por **hoja de compartir nativa** *y* **diálogo copiable**.
- Unirse desde **pestañas** en la pantalla de añadir vehículo.
- Compartir desde una pantalla de **detalle provisional**, visible **solo al dueño**.

## La pieza central: una colección que el cliente no toca

`invites/{código}` — el propio código es el ID del documento, así que se busca directamente sin
consulta. Campos: `vehicleId`, `createdBy`, `createdAt`, `expiresAt`, `usedBy`.

Las reglas **deniegan todo acceso de cliente** a esa colección; el Admin SDK las salta. Eso da
dos cosas de golpe: los códigos no se pueden enumerar, y un cliente manipulado no puede fabricarse
una invitación que no caduque nunca — que es exactamente lo que ocurriría si la creara él.

**`createVehicleInvite`**: exige ser el dueño, **invalida las invitaciones anteriores sin usar**
de ese vehículo (con lo que hay como mucho un código activo, y volver a compartir revoca el
anterior) y crea una nueva con `expiresAt` puesto por el servidor. Usa `create`, de modo que una
colisión de código nunca pisa una invitación viva.

**`joinVehicleWithCode`**: normaliza el código, valida existencia, uso y caducidad, y si ya eres
miembro avisa **sin consumir** la invitación. La escritura va en una transacción que **vuelve a
comprobar** el uso dentro, para que dos personas compitiendo por el mismo código no entren ambas.

**Formato**: 8 caracteres de `ABCDEFGHJKMNPQRSTUVWXYZ23456789` — sin I, L, O, 0 ni 1, porque se
teclea a mano. La entrada se normaliza, así que dan igual mayúsculas, espacios o guiones.

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `functions/src/invites.ts` | **Nuevo**: ambos manejadores |
| `functions/src/index.ts` | Cuatro exports nuevos (dos funciones × dos bases de datos) |
| `firestore.rules` | **Solo se añade** el bloque de denegación de `invites` |
| `domain/model/VehicleInvite.kt`, `domain/repository/InviteRepository.kt` | **Nuevos** |
| `data/repository/FirebaseInviteRepository.kt` | **Nuevo**: invoca las funciones |
| `domain/usecase/{CreateVehicleInvite,JoinVehicleWithCode}UseCase.kt` | **Nuevos** |
| `presentation/vehicledetail/` | **Nueva** pantalla provisional + ViewModel |
| `presentation/addvehicle/` | Pestañas *Crear* / *Unirme* |
| `utils/ShareTextHandler.kt` (+ android/ios) | **Nuevo** `expect/actual` |
| `presentation/navigation/BasicNavigationWrapper.kt` | Primer destino con parámetro |
| `iosApp.xcodeproj/project.pbxproj` | Producto SPM `FirebaseFunctions` |

## Detalles que solo aparecieron al implementar

**Las funciones invocables no saben a qué base de datos pertenece quien llama.** Un trigger de
Firestore sí, porque se registra por base de datos; una invocable la llama el cliente, que puede
ser debug o release. Se resuelve igual que los triggers: **cada función se exporta dos veces** y
el cliente elige según `AppConfig.isDebug`.

**El proyecto de iOS usa Swift Package Manager** y solo declaraba `FirebaseAuth`, `FirebaseCore`
y `FirebaseFirestore`. Añadir el módulo de gitlive hace que el framework de Kotlin referencie
`FirebaseFunctions`, y el enlazado falla hasta que se añade ese producto al target de Xcode.
**Añadir una dependencia de Firebase en KMP tiene siempre este segundo paso en iOS.**

**`AddVehicleScreen` importa los recursos por nombre**, no con comodín, así que cada clave nueva
necesita su import; el detalle usa comodín y no lo necesita.

**Los resultados esperados se devuelven como estado, no como excepción.** La primera versión
lanzaba `HttpsError` para "código no válido", "ya usada", "caducada" y "ya eres miembro". Al
probarlo, al usuario le llegaba **el volcado crudo de la excepción de Swift**, en inglés:

```
FirebaseFunctions.FunctionsError(code: ..., errorUserInfo: ["NSLocalizedDescription":
"You already have this vehicle.", "region": "europe-west4", ...])
```

Inspeccionar el código de error desde Kotlin común es incómodo y depende del transporte de cada
plataforma. La función devuelve ahora `{ status }` (`ok`, `invalid`, `used`, `expired`,
`already_member`) y el cliente lo traduce a su idioma. Solo se lanza excepción para fallos
verdaderos (sin autenticación, argumento inválido, red).

**La confirmación de copiado no puede ser un snackbar**: el `Scaffold` que lo aloja queda detrás
del velo del `AlertDialog`, así que copiar parecía no hacer nada. El aviso va en el propio botón,
que pasa a decir "Código copiado" durante dos segundos.

## Limpieza justificada

Se elimina **`joinVehicleByCodeOrId`** (demostrado que no puede funcionar contra estas reglas,
igual que pasó con `updateUserCars` y `removeUserFromVehicle`) y se deja de generar el
`inviteCode` al crear vehículos, sustituido por la colección `invites`. **El campo se mantiene en
el modelo** para no romper la lectura de los documentos que ya lo tienen.

## Riesgos

- **Región**: las invocables están en `europe-west4` y el cliente **debe** pedir esa región; por
  defecto llamaría a `us-central1` y fallaría.
- **Verificación del "unirse"**: requiere una segunda cuenta, y el asistente no introduce
  credenciales.

## Resolución de las preguntas abiertas de la spec

1. **Formato** → 8 caracteres, alfabeto sin ambigüedades, entrada normalizada.
2. **Modelo** → `invites/{código}` con el código como ID; sin consultas.
3. **Quién genera** → una función, no el cliente (caducidad no falsificable). Cada vez que se
   comparte se crea una nueva y **se revoca la anterior sin usar**.
4. **Limpieza de caducadas** → las sin usar se borran al generar otra para el mismo vehículo. No
   se añade TTL: el volumen es mínimo.
5. **Qué se devuelve** → el **nombre del vehículo**, para confirmar a qué te has unido.
6. **`inviteCode` heredado** → se deja de generar; el campo permanece por compatibilidad.
7. **Fuerza bruta** → caducidad de 24 h, un solo uso, 8 caracteres y la función como único punto
   de entrada. Suficiente para el alcance actual.
