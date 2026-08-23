# AparK — Roadmap y revisión de arquitectura

- **Fecha**: 2026-08-07
- **Estado**: Propuesta para acordar

Este documento responde a dos preguntas a la vez, porque no son separables:

1. **¿Aguanta la arquitectura actual todo lo que queremos construir?** (parte 2)
2. **¿En qué orden lo construimos?** (parte 4)

El orden sale de la revisión: hay cambios que son baratos hoy y caros dentro de tres features,
y ese es el criterio principal para ponerlos primero.

---

## Parte 1 — Dónde estamos

Lo que la app hace hoy, de verdad:

| Área | Estado |
|------|--------|
| Auth | Email/contraseña, Google, Apple (iOS). Verificación de email. Reset de contraseña |
| Vehículos | Crear (nombre + matrícula), eliminar, salirse de uno compartido, reordenar |
| Aparcar | Ubicación actual, arrastrar el marcador, deshacer |
| Compartir | Código de un solo uso, 24 h, revocable. Unirse desde *Añadir vehículo* |
| Detalle | Existe la pantalla, pero **solo tiene el botón de compartir** |
| Backend | 1 trigger de limpieza + 2 callables, ×2 bases de datos |
| Idiomas | es · en · fr (130 cadenas), con algunas cadenas en castellano incrustadas en el código |
| Tema | Esquema Material 3 completo, claro y oscuro, **siguiendo al sistema, sin poder elegir** |

Y lo que **no** existe todavía, más allá de las features pedidas: no hay tests (1 placeholder),
no hay CI, no hay Crashlytics, no hay App Check, no hay persistencia local, y la barra de
navegación inferior es decorativa — sus tres pestañas cambian un `remember` y nada más.

---

## Parte 2 — Revisión de arquitectura

La arquitectura de la app (Clean + MVI + Koin + repositorios tras interfaz) **está bien y no
hay que tocarla**. Es la decisión correcta y escala. Lo que sí necesita revisión es la capa de
datos, y un puñado de cosas de "preparación para publicar" que hoy no existen.

### 2.1 El modelo de datos es la restricción principal

Hoy la lista de vehículos se lee así:

```
users/{uid}.userVehicles → [id1, id2, id3]
   └── por cada id: un listener sobre vehicles/{id}
        └── combine(...)
```

Es decir, **N listeners para N vehículos**, y la lista de ids vive en el documento del usuario.
Ese diseño ha ido generando parches, y los parches están todos documentados en el repo:

- `retryWhen` + `catch` en `getVehiclesForUser`, porque escuchar un documento que no existe
  devuelve `PERMISSION_DENIED` (la regla desreferencia un `resource` nulo) y **la app crasheaba**.
- Los **ids colgantes**, que motivaron toda la [spec 002](specs/002-vehicle-cleanup-function/spec.md)
  y su Cloud Function.
- Las **tarjetas fantasma** de la [spec 004](specs/004-stale-cache-cards/spec.md), agravadas
  precisamente por los 3 reintentos de 1 s del primer parche.

Los tres síntomas tienen la misma causa: **la lista de vehículos se deriva de un array de ids
que puede mentir**. Nada garantiza que un id de `userVehicles` corresponda a un documento que
existe y que puedas leer.

**La alternativa es el patrón canónico de Firestore**: preguntar por los vehículos de los que
eres miembro, en vez de por una lista de ids guardada aparte.

```
vehicles/{id}.memberIds: [uid_dueño, uid_compartido, ...]

vehicles.where("memberIds", "array-contains", miUid).snapshots   ← 1 listener, 1 query
```

Con la regla `allow read: if request.auth.uid in resource.data.memberIds`, Firestore puede
demostrar que la consulta es segura y la acepta. Y entonces:

- **Una query en vez de N listeners.**
- La consulta **solo puede devolver documentos que puedes leer**, así que el `PERMISSION_DENIED`
  desaparece por construcción, y con él el `retryWhen`.
- Un vehículo borrado **deja de aparecer en el resultado**. No hay id colgante que filtrar.
- El **orden por usuario de la [spec 003](specs/003-reorder-vehicles/spec.md) se conserva**:
  `userVehicles` pasa de ser la fuente de la lista a ser **solo la pista de ordenación**. Los ids
  que sobran se ignoran; los vehículos que faltan van al final. Un id colgante deja de ser un bug
  y pasa a ser ruido inofensivo.

Matiz honesto: esto **no cierra del todo la spec 004**. Firestore también cachea resultados de
query, así que un vehículo borrado mientras la app estaba cerrada todavía puede pintarse un
instante desde disco. Lo que sí elimina es la **amplificación de ~3 segundos** que añadían los
reintentos, y deja el problema en su tamaño real: un parpadeo, no una tarjeta fantasma que se
queda.

Coste: una spec, un backfill de `memberIds` sobre los vehículos existentes (hoy son un puñado),
y mantener `memberIds` sincronizado en los cuatro sitios donde cambia la pertenencia (crear,
unirse, salirse, transferir). La función de limpieza de la spec 002 sigue siendo útil para dejar
`userVehicles` limpio, pero **deja de sostener el peso** de que la app funcione.

> **Este es el cambio de mayor palanca de toda la revisión, y hoy es lo más barato que va a ser
> nunca.** Retira tres parches ya escritos, y es el prerequisito de todo lo que viene: en cuanto
> haya incidencias, mantenimientos y ubicaciones como subcolecciones, migrar el modelo de
> pertenencia significa migrar también sus reglas y sus datos.

### 2.2 Reglas y subcolecciones: lo que va a pasar cuando lleguen las incidencias

Hoy las reglas de `vehicles` se bastan solas: `resource.data.ownerId` y `resource.data.sharedUsers`
están en el propio documento, y comprobarlas no cuesta nada.

En cuanto exista `vehicles/{vid}/incidents/{iid}`, la regla necesita saber si eres miembro del
**vehículo padre**, y eso obliga a:

```
function esMiembro(vid) {
  let v = get(/databases/$(database)/documents/vehicles/$(vid)).data;
  return request.auth.uid in v.memberIds;
}
```

Ese `get()` **es una lectura de documento facturada** y añade latencia a cada evaluación.
Firestore lo cachea dentro de una misma petición, así que listar 30 incidencias no son 30
lecturas extra, pero conviene saber que el coste existe.

**Recomendación**: aceptar el `get()` para las subcolecciones. Es la opción simple y a esta
escala es irrelevante. La alternativa (copiar `memberIds` en *cada* documento hijo, para que la
regla no lea nada) obliga a una Cloud Function que rehaga todos los hijos cada vez que alguien
entra o sale de un vehículo. Es más rápido y más caro de mantener: se hace cuando duela, no antes.

### 2.3 Campos que faltan, campos que sobran y una fuga

**Falta `createdAt` / `updatedAt` en todo.** No hay una sola marca de tiempo de creación o
modificación en `users` ni en `vehicles`. Las vas a necesitar para ordenar incidencias, para
saber qué ha cambiado desde la última notificación, y para depurar problemas de un usuario que
te escribe. Añadirlas después de tener datos es incómodo; añadirlas ahora es gratis.

**Sobra `Vehicle.inviteCode`.** Es un resto de antes de la spec 005: las invitaciones viven en
la colección `invites`. Nada lo escribe. Fuera.

**`Vehicle.model` y `Vehicle.color` están muertos.** Solo los escribe `updateVehicle`, que **no
lo llama nadie** (igual que `transferVehicleOwnership`). Se van a reemplazar por los datos ricos
del vehículo de todas formas.

**Y hay una fuga de datos pequeña pero real** en
[`UpdateVehicleLocationUseCase.kt:34`](../composeApp/src/commonMain/kotlin/com/albertomedina/apark/domain/usecase/UpdateVehicleLocationUseCase.kt#L34):

```kotlin
location.copy(user = userProfile, ...)   // userProfile es un User completo
```

`User` incluye `userVehicles`. O sea: **cada vez que aparcas, escribes tu lista completa de ids
de vehículos dentro del documento del vehículo**, y cualquier otro miembro de ese vehículo puede
leerla. No puede abrir esos otros vehículos (las reglas se lo impiden), así que la fuga es de
identificadores, no de contenido — pero está mal, engorda el documento, y la regla que valida el
`lastLocation` de un usuario compartido solo comprueba el email, así que el array viaja sin que
nadie lo mire. `lastLocation.user` debería ser `{uid, name, email}` y nada más.

### 2.4 Un solo proyecto de Firebase para debug y para producción

Hoy: un proyecto, dos bases de datos (`apark-at` para debug, `(default)` para release).

Eso significa que **debug y producción comparten**: el mismo pool de usuarios de Auth, el mismo
despliegue de Functions, las mismas cuotas, la misma facturación, y —lo más delicado— **el mismo
`firestore.rules`, que `firebase.json` despliega a las dos bases a la vez**. Una regla mal escrita
llega a producción en el mismo comando en el que llega a desarrollo.

Lo estándar para una app publicada son **dos proyectos de Firebase** (`apark-dev` y `apark-prod`),
elegidos por variante de compilación mediante `google-services.json` / `GoogleService-Info.plist`
distintos. Es para lo que existen los alias de `.firebaserc`.

**La clave para que esto sea barato es hacerlo en la dirección correcta**: el proyecto actual
**se queda como producción**, y se crea uno nuevo **para desarrollo**. Así no se mueve ni un solo
usuario real ni un solo documento de `(default)`; lo que se abandona es la base `apark-at`, cuyos
datos son desechables. Hacerlo al revés —dos proyectos nuevos y migrar producción— obliga a
exportar e importar los usuarios de Auth con sus hashes de contraseña y a mover Firestore por GCS,
y eso ya es otra cosa.

Coste real, sin adornos: crear el proyecto de desarrollo, registrar en él las apps Android e iOS,
dar de alta las huellas SHA y los clientes OAuth de Google Sign-In, desplegar allí Functions y
reglas, crear a mano su **política TTL de `invites`** (que, como dice `AGENTS.md`, no está
versionada en ningún sitio), y cambiar el `google-services.json` / `GoogleService-Info.plist` de
la variante de debug.

Es media tarde de trabajo tedioso. Y también es lo más barato que va a ser nunca: cada usuario
real que entre en `(default)` sube el precio de equivocarse.

### 2.5 No hay ninguna capa de persistencia local

No hay DataStore, ni `multiplatform-settings`, ni nada. Hoy no hace falta. Pero lo necesitan,
como mínimo: **el tema** (claro/oscuro/sistema), **el idioma**, **el vehículo por defecto del
widget**, si ya se ha visto el onboarding, y un espejo local de las preferencias de notificación.

Es una pieza pequeña que bloquea a cuatro features, así que conviene ponerla antes que ellas y
no a la vez que la primera. `androidx.datastore` ya es multiplataforma; es la opción por defecto.

Ojo con una sutileza: **las preferencias de notificación no pueden vivir solo en local**. Quien
decide si te manda un push es una Cloud Function, y esa función necesita leer tu preferencia
desde Firestore. El ajuste vive en `users/{uid}`, y en local solo se cachea.

### 2.6 Seguridad: falta App Check

Las reglas están razonablemente bien escritas. Dos observaciones menores y una que no lo es:

- *Menor*: el dueño puede cambiar **cualquier cosa** de su vehículo, incluido meter uids
  arbitrarios en `sharedUsers`. Es inofensivo (no añade el vehículo al `userVehicles` de esa
  persona, así que nunca lo vería), pero significa que estar en `sharedUsers` no prueba consentimiento.
- *Menor*: las validaciones de longitud (`name`, `licensePlate`) solo se aplican en `create`.
  En `update` no hay ninguna, así que el dueño puede escribir un nombre de 1 MB.
- **No menor**: no hay **App Check**. Es el mecanismo (Play Integrity en Android, DeviceCheck /
  App Attest en iOS) que garantiza que quien llama a tus callables y a tu Firestore es tu app y
  no un script. Con `createVehicleInvite` expuesta como callable y sin límite de invocaciones,
  cualquiera con tu configuración de Firebase —que va dentro del APK— puede llamarla en bucle.
  Fuerza bruta sobre los códigos no es viable (31⁸ ≈ 8·10¹¹ combinaciones), pero el abuso de
  cuota sí. Para una app publicada, App Check es de manual.

### 2.7 Release engineering: dos cosas verificadas, y una está rota

He compilado `:composeApp:assembleRelease` para comprobarlo. **La compilación funciona, pero R8
imprime esto:**

```
> Task :composeApp:minifyReleaseWithR8
Supplied proguard configuration does not exist:
  .../composeApp/proguard-rules.pro
```

El fichero existe — pero está en **`composeApp/src/proguard-rules.pro`**, y
`build.gradle.kts` lo referencia como `"proguard-rules.pro"`, que Gradle resuelve **relativo al
directorio del módulo**, o sea `composeApp/proguard-rules.pro`. R8 no encuentra el fichero, **avisa,
y sigue adelante sin fallar**. Resultado: el APK de release se está ofuscando y encogiendo
**sin ninguna de las reglas `-keep` escritas** — ni las de los modelos `@Serializable`, ni las de
Koin, ni las de gitlive, ni las de los recursos de Compose.

Puede que funcione igualmente, porque kotlinx.serialization, Firebase y Compose Multiplatform
publican sus propias *consumer rules*. **Eso no lo he verificado**: haría falta instalar el APK
minificado y probarlo. Pero es exactamente la clase de fallo que solo aparece en la build de la
tienda, y el arreglo son dos minutos: mover el fichero a `composeApp/`.

> Y encaja con lo que ya sabemos de cómo pruebas: el release lo pruebas en tu iPhone, y **en iOS
> no hay R8**. La ruta de release de Android es la que no ha tenido ojos encima.

Lo segundo verificado: la salida es **`composeApp-release-unsigned.apk`**. No hay `signingConfig`,
y Play necesita un **AAB firmado** (`bundleRelease`), no un APK. Falta toda la configuración de
firma con clave de subida.

Y lo que no hay y debería haber antes de publicar:

- **Crashlytics**. Publicar para miles de usuarios sin informes de crash es ir a ciegas. Es la
  pieza que más te va a devolver por lo poco que cuesta ponerla.
- **CI**. No hay `.github/` de ninguna clase. El mínimo razonable: compilar Android, compilar
  `compileKotlinIosSimulatorArm64` y pasar los tests en cada PR.
- **Tests**. Un placeholder. Los ViewModels y los use cases son Kotlin puro contra interfaces:
  son directamente testeables. Las **reglas de Firestore** se testean con el emulador y
  `@firebase/rules-unit-testing`, y son lo que más se agradece tener cubierto, porque un fallo ahí
  es un fallo de seguridad, no un bug.
- **`android:allowBackup="true"`** con Firebase: el backup automático puede restaurar en un
  dispositivo nuevo la caché de Firestore y el estado de sesión del anterior. Hay que excluir los
  directorios de Firebase con `dataExtractionRules`, o desactivarlo.
- Las **versiones de Android e iOS han empezado a separarse** (1.0.1 / código 2 frente a
  `MARKETING_VERSION=1.0` / build 1). Conviene una única fuente antes de la primera subida.

---

## Parte 3 — Requisitos de tienda que son bloqueantes

Esto no es opinión: son motivos de rechazo.

1. **Eliminar la cuenta desde dentro de la app.** Obligatorio en App Store y en Google Play para
   cualquier app que permita crear cuenta. Ya está en tu lista — pero conviene saber que **no es
   una pantalla, es backend**: hay que borrar los vehículos de los que eres dueño (con su cascada),
   salirte de los compartidos, borrar tu documento de usuario y borrar el usuario de Auth. El
   cliente no puede hacer casi nada de eso, así que es una callable.
2. **Política de privacidad publicada, con URL.** Manejas ubicación, que es dato sensible.
3. **Data Safety (Play) y las etiquetas de privacidad (App Store)**, declarando ubicación, email
   y lo que recojan Crashlytics/Analytics si los añades.
4. **Cuenta de demostración para la revisión de Apple**, porque la app requiere login para
   hacer cualquier cosa. Sin ella, rechazo casi seguro.

---

## Parte 4 — El plan por fases

Los tamaños son **relativos entre sí**, no en horas: **S** = un PR pequeño · **M** = una spec
completa · **L** = varias specs · **XL** = proyecto aparte, con incógnitas de verdad.

### Fase 0 — Los cimientos que no se ven

*Por qué primero: son cosas que se abaratan al hacerlas hoy y se encarecen con cada feature nueva.
Ninguna se ve en la pantalla, y todas hacen que lo siguiente cueste menos.*

| # | Qué | Tamaño | Estado | Por qué ahora |
|---|-----|--------|--------|---------------|
| 0.1 | **Mover `proguard-rules.pro` a `composeApp/`** | S | ✅ hecho | Verificado roto. La build de la tienda es la única que lo sufre |
| 0.2 | **Modelo de pertenencia `memberIds` + query única** (§2.1) | M | [spec 008](specs/008-vehicle-membership-model/spec.md) | Retira 3 parches, desbloquea todas las subcolecciones |
| 0.3 | **`createdAt`/`updatedAt`, quitar `inviteCode`, arreglar la fuga de `lastLocation.user`** (§2.3) | S | va en la spec 008 | Misma migración que 0.2 |
| 0.4 | **Persistencia local (DataStore)** (§2.5) | S | | Bloquea tema, idioma, widget y notificaciones |
| 0.5 | **Crashlytics** + un log mínimo compartido | S | | Lo que más devuelve por lo que cuesta. **Subido de prioridad**: ver abajo |
| 0.6 | **CI en GitHub Actions**: Android + iOS (Xcode) + functions | S | ✅ hecho | A partir de aquí cada PR se valida solo |
| 0.7 | **Firma y `bundleRelease`** | S | ✅ hecho | Sin esto no hay subida posible |
| 0.8 | **App Check** (§2.6) | M | | Antes de tener usuarios, no después |
| 0.9 | **Separar proyectos Firebase dev/prod** (§2.4) | M | acordado | Hoy media tarde; después, una migración |

> **Por qué 0.5 subió de prioridad.** Diagnosticar por qué fallaba el login con Google en Android
> costó una tarde, y no por lo difícil del fallo —era una configuración de OAuth— sino porque **la
> app no lo contaba**: el `onError` del botón social solo hacía un `println`, y el dispositivo de
> pruebas es un Xiaomi, que se traga los logs de las apps de terceros. El error real (`28444`)
> apareció en la primera prueba en cuanto la app lo mostró en pantalla. Sin un sitio donde mandar
> los fallos, cada incidencia así cuesta lo mismo.

> **Orden dentro de la fase 0**: 0.9 (separar proyectos) conviene **antes** que 0.2, y la razón no
> es el coste sino el riesgo. La spec 008 incluye un cambio de reglas y un backfill sobre datos
> reales, y hoy **no hay ningún sitio donde ensayarlo**: `firebase.json` despliega el mismo
> `firestore.rules` a las dos bases en el mismo comando, así que la prueba y la producción se
> tocan a la vez. Separar primero da un entorno donde equivocarse sale gratis.

### Fase 1 — Cuenta, ajustes y navegación de verdad

*Por qué aquí: contiene el bloqueante de tienda (borrar cuenta), y la navegación real es
prerequisito de cualquier pantalla nueva — hoy las tres pestañas no llevan a ningún sitio.*

| # | Qué | Tamaño |
|---|-----|--------|
| 1.1 | **Navegación real de las 3 pestañas** (un backstack por pestaña en Navigation 3) | M |
| 1.2 | **Pantalla de perfil**: nombre, cambiar email, cambiar contraseña, cerrar sesión | M |
| 1.3 | **Foto de perfil** → estrena **Firebase Storage** (pieza nueva: reglas, subida, recorte, caché) | M |
| 1.4 | **Eliminar cuenta** — callable + cascada. **Bloqueante de tienda** | M |
| 1.5 | **Ajustes**: tema (claro/oscuro/sistema) e idioma | S |
| 1.6 | **Sacar a recursos las cadenas incrustadas** (el snackbar de GPS y "Logout" en `HomeScreen`) | S |

### Fase 2 — El vehículo como entidad rica

| # | Qué | Tamaño |
|---|-----|--------|
| 2.1 | **Datos del vehículo**: marca, modelo, tipo, año, combustible | M |
| 2.2 | **Editar vehículo** — `UpdateVehicleUseCase` ya existe y no lo llama nadie | S |
| 2.3 | **Pantalla de detalle de verdad** (hoy es un nombre y un botón) | M |
| 2.4 | **Gestión de miembros + transferir propiedad** — `transferVehicleOwnership` ya existe sin UI, y el diálogo de borrado **ya se la sugiere al usuario** | M |
| 2.5 | **Desplegables de marca/modelo** | S–M |

Sobre 2.5, la recomendación es **no usar una API**: un **JSON estático curado** en
`composeResources`. Sin clave, sin límite de peticiones, sin latencia, funciona sin conexión, y las
marcas y modelos cambian una vez al año. La opción pública gratuita real (vPIC de la NHTSA) está
centrada en EE. UU. y no cubre bien el mercado europeo. Si algún día quieres exhaustividad, el
JSON se sustituye por una colección `catalog` en Firestore que la app cachea — sin cambiar la UI.

### Fase 3 — Lo que hace la app útil para un grupo

*Esta es la fase que justifica la app. Todo lo anterior es soporte.*

| # | Qué | Tamaño | Notas |
|---|-----|--------|-------|
| 3.1 | **Incidencias** (`vehicles/{id}/incidents`): reportar, estado, quién y cuándo, resolver | L | El corazón de la propuesta compartida |
| 3.2 | **ITV y revisiones**: fechas, aviso de proximidad | M | Necesita el **primer índice** del proyecto |
| 3.3 | **Mantenimiento**: historial de intervenciones | M | Mismo patrón de subcolección que 3.1 |
| 3.4 | **Ubicaciones predeterminadas** (garaje) | M | Ver decisión abierta abajo |

Sobre 3.4, la duda de diseño es a quién pertenece un garaje. En tu ejemplo —dos vehículos, una
plaza— el garaje es **del hogar**, no de un vehículo, y en el modelo actual no existe el concepto
de hogar. Las opciones:

- **Por vehículo** (`vehicles/{id}/places/`): defines "Garaje" dos veces. Simple, y la etiqueta
  la ven todos los miembros de ese vehículo.
- **Por usuario**: la defines una vez, pero **los demás miembros no ven la etiqueta**, solo unas
  coordenadas. Malo justo para lo que quieres.
- **Colección propia con sus miembros**: la defines una vez y todos la ven, pero has construido un
  **segundo sistema de permisos** en paralelo al de vehículos.

**Recomendación: por vehículo**, y guardar el nombre del sitio dentro de `lastLocation`
(`placeId` + `placeName`), para que la tarjeta pueda decir "Aparcado en el garaje" sin una segunda
lectura. Se paga con definir el garaje dos veces, una vez en la vida. Si algún día aparece el
concepto de "grupo" o "familia", las ubicaciones se mudan ahí de forma natural.

### Fase 4 — Notificaciones

| # | Qué | Tamaño |
|---|-----|--------|
| 4.1 | **Infraestructura FCM**: tokens por dispositivo, permisos, APNs en iOS | M |
| 4.2 | **Preferencias en `users/{uid}`** (las lee la función, no el móvil) | S |
| 4.3 | **Fan-out al aparcar y al cambiar una incidencia**, excluyendo a quien lo hizo | M |
| 4.4 | **Recordatorios de ITV**: función programada diaria + índice sobre la fecha | M |

Va después de la fase 3 por una razón concreta: **una notificación necesita algo de lo que
avisar**. Con solo "han aparcado", el valor es escaso; con incidencias e ITV detrás, es lo que
hace que la gente abra la app.

### Fase 5 — Fuera del teléfono

*Aquí es donde hay que ser honesto: esto no es "más de lo mismo". Nada de esto es Compose
Multiplatform, y nada vive en `commonMain`.*

| # | Qué | Tamaño | La parte incómoda |
|---|-----|--------|-------------------|
| 5.1 | **Widget Android** (Glance) | M | Glance es Compose-*like*, pero no es tu Compose. Código en `androidMain` |
| 5.2 | **Widget iOS** (WidgetKit) | L | **SwiftUI en una extensión aparte**, con App Group para compartir datos, y Firebase dentro de la extensión |
| 5.3 | **Android Auto** (Car App Library) | L | Plantillas nativas, no Compose. "Parking" es categoría soportada |
| 5.4 | **CarPlay** | XL | Plantillas UIKit (`CPTemplate`) **+ entitlement que Apple tiene que aprobar** |

Tres avisos concretos sobre esta fase:

**El widget de iOS necesita un spike antes que una spec.** Un widget de WidgetKit **no es tu app
corriendo**: es una extensión con su propio proceso y restricciones fuertes. Que pueda obtener la
ubicación con un toque, sin abrir la app, depende de detalles (`NSWidgetWantsLocation`, App
Intents interactivos, qué autorización hereda de la app) que **hay que verificar contra la
documentación vigente de Apple antes de prometer la feature**. Puede que la respuesta acabe siendo
"el widget abre la app y esta guarda la ubicación", que sigue siendo un toque menos, pero no es lo
que imaginas.

**El entitlement de CarPlay conviene pedirlo pronto.** *Parking* sí es una de las categorías que
Apple admite, y AparK encaja de lleno. Pero es una solicitud que Apple aprueba a mano y con
plazos, y **no depende de que hayas escrito una línea**. Pedirlo cuando empiece la fase 3 significa
que la respuesta esté llegando cuando llegues a la 5. *(Las condiciones exactas cambian con el
tiempo: hay que confirmarlas en el portal de Apple en el momento de pedirlo.)*

**Y una consecuencia de producto**: el widget y CarPlay necesitan saber **cuál es tu vehículo por
defecto**. Eso es un ajuste que hay que diseñar en la fase 1, aunque no se use hasta la 5.

### Transversal — durante todas las fases

- **Tests**, empezando por las **reglas de Firestore** con el emulador. No es una fase: es algo
  que entra con cada spec a partir de la 0.6.
- **Traducir todo lo nuevo** a es/en/fr desde el primer commit, no al final.
- **Documentos de tienda** (privacidad, Data Safety, cuenta de demo): se preparan durante la
  fase 4, no la semana de subir.

---

## Parte 5 — Las decisiones que hacen falta para empezar

1. ~~**¿Se hace 0.2 (el modelo `memberIds`)?**~~ → **Sí, acordado el 2026-08-07.** Redactada como
   [spec 008](specs/008-vehicle-membership-model/spec.md).
2. ~~**¿Se separan los proyectos de Firebase (0.9)?**~~ → **Sí, acordado el 2026-08-07**, y **antes
   que la 008**, para tener dónde ensayar su migración.
3. **¿La foto de perfil entra en la fase 1?** Estrena Firebase Storage, que es un servicio nuevo
   con sus propias reglas y sus propios costes. Es la única pieza de la fase 1 que se puede
   posponer sin arrastrar nada.
4. **¿Las ubicaciones predeterminadas van por vehículo?** (§3.4)
5. **¿Se pide el entitlement de CarPlay ya**, aunque no se vaya a tocar hasta dentro de meses?

---

## Apéndice — Lo que NO hay que cambiar

Para que la lista de arriba no dé una impresión equivocada, conviene decir también qué está bien:

- **Clean Architecture + MVI**: correcta, y sostiene todo lo que viene. Los use cases con un solo
  `operator fun invoke()` hacen que añadir features sea aburrido, que es exactamente lo que se busca.
- **Repositorios tras interfaz**: es lo que hace que los tests de la fase 0.6 sean posibles sin
  reescribir nada.
- **Expect/actual solo donde hace falta** (mapas, ubicación, permisos, botones de auth): la
  frontera está bien puesta. La fase 5 la va a estirar, pero no la rompe.
- **El backend**: las callables existen por razones de seguridad correctas, y están escritas con
  cuidado (la transacción de `joinWithCodeHandler` re-lee la invitación dentro de la transacción
  para que dos personas no puedan usar el mismo código a la vez).
- **El proceso**: SDD con specs numeradas y CHANGELOG al día. Es lo que ha hecho posible esta
  revisión — casi todo lo de la parte 2 sale de leer vuestras propias specs, no el código.

---

## Sobre monetización

No hay que hacer nada ahora, y meter infraestructura de pagos "por si acaso" sería justo el error.
Solo una precaución que **no cuesta nada** y que evita un callejón: si algún día pones un límite
(número de vehículos, número de personas por vehículo), **que lo imponga una regla de Firestore,
no la UI**. Un límite que vive en el cliente no se puede convertir en un plan de pago sin
reescribirlo, y además no es un límite: es una sugerencia.
