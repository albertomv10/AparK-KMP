# Firebase — dos proyectos, y cómo levantar uno desde cero

AparK usa **dos proyectos de Firebase separados**:

| Entorno | Proyecto | Base de datos | Quién lo usa |
|---------|----------|---------------|--------------|
| Desarrollo | `apark-dev` | `(default)` en `eur3` | compilaciones **debug** (`com.albertomedina.apark.debug`) |
| Producción | `apark-617fd` | `(default)` en `eur3` | compilaciones **release** (`com.albertomedina.apark`) |

**Qué entorno usa la app no se decide en tiempo de ejecución.** Lo decide el fichero de
configuración que entra en el binario:

- **Android**: el plugin de Google Services busca `composeApp/src/<variante>/google-services.json`
  antes que el de la raíz del módulo. `src/debug/` apunta a `apark-dev`; el de
  `composeApp/google-services.json` apunta a producción.
- **iOS**: la fase de build *Setup Firebase* copia
  `iosApp/FirebaseConfig/<configuración en minúsculas>/GoogleService-Info.plist` dentro del `.app`.

> **El client ID de Google en iOS no se declara en `Info.plist`.** Estuvo incrustado, y como
> apuntaba siempre al mismo valor, **la build de release usaba el client de debug**. Pasaba
> desapercibido porque los dos vivían en el mismo proyecto y Firebase Auth aceptaba el token igual;
> con proyectos separados deja de funcionar. Ahora se configura en `iOSApp.swift` desde
> `FirebaseApp.options.clientID`, que sigue al entorno solo. Lo que **sí** sigue en `Info.plist` es
> el `CFBundleURLSchemes` por el que Google devuelve el control a la app: iOS lo lee del binario
> antes de que exista Firebase, así que están registrados **los dos** entornos y cada build usa el
> que le toca.

> Antes existía un solo proyecto con dos bases de datos (`(default)` y `apark-at`), y la app elegía
> en caliente con un `AppConfig.isDebug` que venía de `BuildConfig.DEBUG` en Android y de un
> `#if DEBUG` en Swift. Toda esa cadena se ha eliminado: existía únicamente por compartir proyecto.

## Desplegar

`.firebaserc` define los alias, y **`default` apunta a desarrollo a propósito**: tocar producción
exige nombrarla.

```sh
npx firebase-tools@latest deploy --only firestore:rules            # dev, por el alias default
npx firebase-tools@latest deploy --only firestore:rules -P prod    # producción, explícito
npx firebase-tools@latest deploy --only functions -P prod
```

> **La trampa que esto elimina**: con un solo proyecto, `firebase.json` listaba las dos bases y
> `--only firestore:rules` las desplegaba **a las dos a la vez**. Una regla mal escrita llegaba a
> producción en el mismo comando en el que llegaba a desarrollo.

## Levantar un proyecto desde cero

Lo que se puede automatizar y lo que no. **Las filas marcadas como consola no tienen equivalente en
`firebase-tools`**: si no quedan escritas aquí, se pierden — que es exactamente lo que pasó con la
política TTL (ver [spec 007](specs/007-invite-cleanup/spec.md)).

| # | Paso | Cómo |
|---|------|------|
| 1 | Crear el proyecto | `firebase projects:create <id>` |
| 2 | Habilitar la API de Firestore y crear la base **`(default)`** (con paréntesis) en **`eur3`** | **Consola** — la API no está habilitada en un proyecto nuevo, y `firestore:databases:create` falla con 403 hasta que lo esté. **Ojo al nombre y a la región**: ver las trampas de abajo |
| 3 | Subir a plan **Blaze** | **Consola** — Cloud Functions v2 lo exige. Requiere método de pago |
| 4 | Registrar las apps Android e iOS | `firebase apps:create ANDROID/IOS --package-name/--bundle-id <id>` |
| 5 | Añadir las huellas SHA-1 y SHA-256 | `firebase apps:android:sha:create <appId> <hash>` |
| 6 | Habilitar los proveedores de Auth: email/contraseña, Google, Apple | **Consola** — y hasta que Google esté habilitado, el `google-services.json` sale **sin `oauth_client`** y el login con Google no funciona |
| 7 | Descargar las configuraciones | `firebase apps:sdkconfig ANDROID/IOS <appId> --out <ruta>` — **crea antes el directorio**: `--out` no lo hace, y si no existe el comando falla sin dejar fichero |
| 8 | Desplegar reglas y funciones | `firebase deploy --only firestore:rules,functions` |
| 9 | Crear la **política TTL** de `invites` sobre `expiresAt` | **Consola** — `firebase-tools` no tiene comando de TTL. Sin ella, las invitaciones se acumulan para siempre |

La huella SHA del paso 5 se saca del keystore de debug:

```sh
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

Y los ficheros del paso 7 se generan **en cada copia del repositorio**, porque están gitignorados y
no viajan con los commits. Para la variante debug de Android hay que crear el directorio primero:

```sh
mkdir -p composeApp/src/debug
firebase apps:sdkconfig ANDROID <appId> --project dev --out composeApp/src/debug/google-services.json
```

### Tres trampas del paso 2, aprendidas a base de pisarlas

**`default` no es `(default)`.** Si en la consola escribes `default` como nombre, obtienes una base
**con nombre** que se llama así, no la base por defecto del proyecto. Se parecen tanto que no se ve
hasta que algo falla.

**`firebase deploy` crea la base que le falta, y la crea en Estados Unidos.** Si `firebase.json` no
nombra base, el deploy apunta a `(default)`; si no existe, **no falla: la crea**, y en la región por
defecto, que es `nam5`. Eso rompe la regla de que las funciones vayan a `europe-west4`, y deja los
datos fuera de la UE. **Antes de cualquier deploy contra un proyecto nuevo, comprueba qué bases hay
y dónde**:

```sh
firebase firestore:databases:list --project <id>
```

**El free tier es de una sola base por proyecto.** La segunda factura desde el primer byte, así que
una base creada por accidente no solo estorba: cuesta.

Y un detalle operativo: al borrar una base, **su ID no se puede reutilizar durante unos cinco
minutos**. El error dice exactamente cuántos segundos faltan.

## Estado de la migración a `apark-dev`

**Hecho:**

- [x] Proyecto `apark-dev` creado
- [x] Apps Android (`com.albertomedina.apark.debug`) e iOS (`com.albertomedina.apark.debug`) registradas
- [x] SHA-1 y SHA-256 del keystore de debug registradas
- [x] `google-services.json` y `GoogleService-Info.plist` de desarrollo generados (están
      gitignorados: hay que regenerarlos en cada copia del repo con el paso 7)
- [x] `.firebaserc` con alias `dev` / `prod`, y `firebase.json` con una sola base
- [x] Eliminada la cadena `isDebug` completa (Swift → Koin → nombre de base y de función)
- [x] Compila en Android e iOS-Kotlin, y `functions` pasa `tsc`

**Pendiente, y todo de consola:**

- [x] Paso 2 — Firestore `(default)` en `eur3`, edición **Standard** (la edición es inmutable;
      Enterprise añade pipelines y joins, pero `dev.gitlive` no los expone a `commonMain`)
- [x] Paso 3 — Blaze
- [x] Paso 6 — proveedores de Auth: email, Google y Apple
- [x] Paso 9 — política TTL de `invites` sobre `expiresAt` (hubo que rehacerla: la primera se fue
      con la base que se borró). Tarda en pasar de «creando» a activa; Google admite hasta 24 h
- [x] Reglas desplegadas a `apark-dev` y verificadas contra el proyecto activo
- [x] Funciones desplegadas en `europe-west4`: `cleanupVehicleReferences` (trigger),
      `createVehicleInvite` y `joinVehicleWithCode` (callables)
- [x] Verificado que cada variante embebe su proyecto, mirando lo que el plugin inyecta en
      `composeApp/build/generated/res/process<Variante>GoogleServices/values/values.xml`:
      debug → `apark-dev`, release → `apark-617fd`, con `default_web_client_id` distinto en cada una
- [ ] Probar la app debug en ejecución contra `apark-dev` (registro, crear vehículo, aparcar,
      compartir)

> **El primer despliegue de funciones de 2ª generación falla, y es normal.** Firebase habilita
> Cloud Build, Artifact Registry, Eventarc, Run y Pub/Sub en la misma ejecución, y acto seguido
> intenta dar roles IAM a unas cuentas de servicio que Google todavía está creando. Sale
> `We failed to modify the IAM policy` o `Permission denied while using the Eventarc Service
> Agent`. **Se arregla esperando un par de minutos y reintentando**, no tocando permisos. Aquí
> hicieron falta tres pasadas.
>
> Y conviene desplegar con `--force` la primera vez para que configure la **política de limpieza
> de Artifact Registry**: sin ella, la imagen de contenedor de cada despliegue se queda ahí y
> factura. Ojo con esa bandera contra producción: además borra las funciones desplegadas que no
> estén en el código.

## Migrar la pertenencia de los vehículos (spec 008)

El orden importa, y saltárselo rompe cosas distintas en cada paso. Se hace **entero contra
`apark-dev` primero**, y solo después contra producción.

| # | Paso | Comando | Por qué en este orden |
|---|------|---------|------------------------|
| 1 | **Backfill** de `memberIds` | `npm run migrate --prefix tools -- --project <id> --dry-run` y luego sin `--dry-run` | Aditivo: no toca `sharedUsers` y ningún cliente se entera |
| 2 | **Desplegar las reglas** | `firebase deploy --only firestore:rules -P <alias>` | Leer sigue funcionando incluso para una app sin actualizar, porque lee documento a documento y ya pasa `uid in memberIds` |
| 3 | **Actualizar las apps** | — | Entre el 2 y el 3 una app vieja **lee pero no puede crear ni salirse** |
| 4 | **Limpieza** de `sharedUsers` | `npm run migrate --prefix tools -- --project <id> --drop-shared-users` | **Después** del 3: mientras queden reglas viejas desplegadas, ese campo aún da acceso de lectura |

El script es **idempotente** y exige `--project` siempre, para que ningún valor por defecto del
entorno decida por su cuenta contra qué base escribe.

> **Credenciales**: contra el emulador no hace falta ninguna, que es lo que permite probarlo de
> verdad. Contra un proyecto real usa las credenciales por defecto de aplicación, o sea una **cuenta
> de servicio** — la credencial más peligrosa del proyecto, porque se salta todas las reglas. Nunca
> debe entrar en el repositorio, y conviene borrarla cuando la migración termine.

## Tests de reglas

```sh
npm run test:rules --prefix tools
```

Arranca el emulador, pasa los tests y lo apaga. No toca ningún proyecto real. Prueban tanto lo que
**debe permitirse** como lo que **debe denegarse**, que es lo que de verdad demuestra algo: una regla
probada solo por el lado bueno pasaría igual siendo `allow read: if true`.

## Limpieza pendiente en el proyecto de producción

Nada de esto es urgente, y todo es destructivo, así que está sin tocar:

1. **La huella SHA-1 duplicada.** La app *Android AparK DEBUG* de producción todavía lleva
   `60de2513…`, el keystore de debug de este equipo, que ahora también está en `apark-dev`. Mismo
   paquete + misma huella en dos proyectos es lo que dispara el aviso *"otro proyecto contiene un
   cliente de OAuth 2.0 que usa esta misma combinación"*. Se quita con:

   ```sh
   firebase apps:android:sha:delete 1:251452774471:android:e0689490ed7407c3147962 4ecfd20f96dcc3a0 --project apark-617fd
   ```

   Las huellas de la app de *release* son otras distintas y no están implicadas.

2. **Las apps de debug** (`com.albertomedina.apark.debug`, Android e iOS) de producción ya no
   sirven para nada. Conviene borrarlas, pero **las dos apps Android de producción comparten la
   misma clave de API**, así que merece comprobarse que borrar una no toca la de release antes de
   hacerlo a lo bruto.

3. **La base `apark-at`** queda huérfana.

4. **Los usuarios de prueba siguen en el pool de Auth de producción.** La separación no los mueve
   retroactivamente: las cuentas con las que se probaba antes conviven con las reales.
