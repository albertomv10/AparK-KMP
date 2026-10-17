# Spec: emuladores de Firebase en debug

- **ID**: 010-firebase-emulators
- **Estado**: Borrador
- **Fecha**: 2026-10-17

## Problema / Por qué

**Todo lo que importa de AparK vive detrás del inicio de sesión**, y el asistente no puede entrar.

No es una limitación técnica que se arregle con una contraseña: autenticarse contra un servicio
remoto —y Firebase Auth lo es, aunque el proyecto sea el de desarrollo— es algo que no hace. Así
que hoy la única persona capaz de mirar el mapa, el carrusel, el detalle o la pantalla de Cuenta
eres tú.

El coste se vio entero en la [spec 009](../009-tab-navigation/spec.md). Seis correcciones salieron
de probar en el móvil, y cada una costó una vuelta completa: escribir, compilar, avisarte, esperar,
que lo probaras, que lo contaras, volver a empezar. Tres de ellas —el logo de Maps tapado, el tema
del mapa en iOS, las transiciones entre pestañas— eran cosas que se ven **en dos segundos** con la
app delante. Una de ellas necesitó **tres intentos** porque yo razonaba sin poder mirar.

Hay un segundo coste, más silencioso: las pruebas manuales ensucian `apark-dev` con vehículos y
usuarios de prueba, y dependen de la red y del estado que hubiera de antes.

Un emulador local resuelve las dos cosas a la vez. El de Firestore **ya está configurado** en
`firebase.json` y lo usan los tests de reglas; falta el de Auth, y falta que la app en debug sepa
hablar con ellos.

## Objetivos

- Que una build de **debug** pueda apuntar a los emuladores de Auth y Firestore en local.
- Que el asistente pueda **crear un usuario de prueba, iniciar sesión y navegar la app entera** sin
  credenciales reales y sin tocar ningún proyecto de Firebase.
- Que haya **datos de partida reproducibles**: un par de usuarios y vehículos sembrados por script,
  iguales en cada arranque.
- Que activarlo sea **explícito**: una build de debug normal sigue hablando con `apark-dev` salvo
  que se le diga lo contrario.

## No-objetivos

- Tocar el comportamiento de release **en absoluto**. Si el código del emulador puede llegar a una
  build de release, la spec está mal resuelta.
- Sustituir tus pruebas en dispositivo. El emulador no reproduce Play Integrity, App Attest, el
  inicio de sesión con Google o Apple, ni cómo se ve el mapa en un iPhone real.
- Emular Functions. Las dos callables de invitaciones se quedan contra el proyecto real por ahora.
- Tests automatizados de UI. Esto es para poder *mirar*, no para afirmar sin mirar.

## Historias de usuario

- Como **asistente**, quiero entrar en la app con un usuario local para comprobar yo mismo lo que
  acabo de escribir, en vez de describírtelo y esperar.
- Como **Alberto**, quiero dejar de ser el único que puede ver nada, y que lo que llegue a mi móvil
  venga ya mirado una vez.
- Como **Alberto**, quiero probar casos incómodos —un vehículo compartido entre dos cuentas, una
  invitación caducada— sin fabricarlos a mano en `apark-dev`.

## Criterios de aceptación

1. **Dado** un emulador arrancado y una build de debug con los emuladores activados, **cuando** el
   asistente abre la app en el simulador, **entonces** puede registrarse o iniciar sesión con un
   usuario local y llegar al mapa.
2. **Dado** ese mismo arranque, **cuando** se consultan los datos, **entonces** salen los usuarios y
   vehículos sembrados por el script, idénticos a los de cualquier otro arranque.
3. **Dado** que los emuladores **no** están activados, **cuando** se compila y ejecuta una debug
   normal, **entonces** la app habla con `apark-dev` exactamente igual que hoy.
4. **Dado** un APK o IPA de **release**, **cuando** se inspecciona, **entonces** no contiene ninguna
   referencia al código de conexión a emuladores — comprobado como se comprobó App Check, mirando
   el binario y no el código fuente.
5. **Dado** un emulador recién arrancado, **cuando** se para y se vuelve a arrancar, **entonces** el
   estado vuelve a ser el sembrado, sin rastro de la sesión anterior.
6. **Dado** `docs/FIREBASE.md`, **cuando** alguien clona el repositorio de cero, **entonces**
   encuentra ahí cómo levantar los emuladores y sembrarlos.

## Preguntas abiertas

- **Cómo se activa.** ¿Una entrada en `local.properties` —que ya está gitignorada y es por máquina—,
  una variante de build propia, o una variable de entorno? La de `local.properties` suena a lo más
  barato, pero hay que ver cómo llega a iOS, que no la lee.
- **Dónde se conecta.** `Firebase.auth.useEmulator(...)` y su equivalente de Firestore tienen que
  ejecutarse **antes** del primer uso. En Android eso es la `Application`; en iOS, junto a
  `FirebaseApp.configure()` en `iOSApp.swift`. ¿Cabe en código común o acaba siendo `expect/actual`?
- **La dirección del host.** El simulador de iOS llega a la máquina por `localhost`; un emulador de
  Android necesita `10.0.2.2`, y un móvil físico la IP de la red. ¿Se resuelve por plataforma, se
  configura, o se deja documentado que el físico no usa emuladores?
- **Cómo se siembra.** ¿Un script de `tools/` con el Admin SDK contra el emulador —reaprovechando lo
  que ya existe de la migración de la spec 008— o el `--import/--export` del propio emulador? Lo
  primero es código que se lee; lo segundo es un volcado binario que nadie revisa.
- **Las reglas.** El emulador de Firestore carga `firestore.rules`. ¿Los datos sembrados las
  respetan, o el script las salta por usar el Admin SDK y acaba creando estados imposibles?
- **Qué pasa con las invitaciones.** Las callables no se emulan, así que *Unirme* no funcionará
  contra el emulador. ¿Se acepta y se documenta, o hay que sembrar los vehículos compartidos ya
  compartidos?

## Fuera de alcance

- Emular Cloud Functions, y con ello el flujo de invitaciones completo.
- Un panel o UI de emuladores (`"ui": { "enabled": false }` se queda como está salvo que estorbe).
- Que la CI use los emuladores para algo más que los tests de reglas, que ya lo hace.
- Cualquier cambio en cómo se prueban las builds de release, que siguen yendo a `apark-617fd`.
