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
| 2 | Habilitar la API de Firestore y crear la base `(default)` en **`eur3`** | **Consola** — la API no está habilitada en un proyecto nuevo, y `firestore:databases:create` falla con 403 hasta que lo esté |
| 3 | Subir a plan **Blaze** | **Consola** — Cloud Functions v2 lo exige. Requiere método de pago |
| 4 | Registrar las apps Android e iOS | `firebase apps:create ANDROID/IOS --package-name/--bundle-id <id>` |
| 5 | Añadir las huellas SHA-1 y SHA-256 | `firebase apps:android:sha:create <appId> <hash>` |
| 6 | Habilitar los proveedores de Auth: email/contraseña, Google, Apple | **Consola** — y hasta que Google esté habilitado, el `google-services.json` sale **sin `oauth_client`** y el login con Google no funciona |
| 7 | Descargar las configuraciones | `firebase apps:sdkconfig ANDROID/IOS <appId> --out <ruta>` |
| 8 | Desplegar reglas y funciones | `firebase deploy --only firestore:rules,functions` |
| 9 | Crear la **política TTL** de `invites` sobre `expiresAt` | **Consola** — `firebase-tools` no tiene comando de TTL. Sin ella, las invitaciones se acumulan para siempre |

La huella SHA del paso 5 se saca del keystore de debug:

```sh
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

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

- [ ] Paso 2 — habilitar Firestore y crear `(default)` en `eur3`
- [ ] Paso 3 — Blaze
- [ ] Paso 6 — proveedores de Auth
- [ ] Paso 9 — política TTL de `invites`
- [ ] Y después: desplegar reglas y funciones, y verificar

**Decisión aparte**: la base `apark-at` del proyecto de producción queda huérfana. Conviene
borrarla para que nadie la use por error, pero es destructiva y no se ha tocado.
