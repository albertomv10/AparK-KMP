# Design: Limpieza de referencias con Cloud Function

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado
- **Fecha**: 2026-08-03

## Enfoque

Una Cloud Function v2 se dispara al borrarse un documento de `vehicles` y quita ese id del
`userVehicles` de **todos** sus miembros, algo que el cliente no puede hacer porque las reglas
le impiden escribir en el documento de otro usuario.

### Decisiones tomadas
- **TypeScript** (compilación antes de desplegar).
- Desplegada en **ambas bases de datos**: `(default)` y `apark-at`.
- **La app no se toca**: ni Kotlin, ni reglas de Firestore.

## Hallazgos que condicionan el diseño

- ⚠️ **Ambas bases de datos están en `eur3`** (multirregión Europa). Un trigger de Firestore v2
  debe desplegarse en una región compatible con la ubicación de su base de datos, así que se fija
  **`europe-west4`**; el valor por defecto (`us-central1`) sería rechazado.
- **No quedan IDs colgantes** en los datos actuales, así que **no se implementa** la función de
  mantenimiento puntual que la spec dejaba como pregunta abierta.
- El proyecto no tenía backend: se crea `functions/` y se añade el bloque `functions` a
  `firebase.json`.

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `functions/src/index.ts` | **Nuevo**: manejador + los dos triggers |
| `functions/package.json`, `tsconfig.json`, `.gitignore` | **Nuevos**: proyecto TypeScript (Node 22) |
| `firebase.json` | Bloque `functions` con `predeploy` que compila |

## La función

Un manejador compartido, exportado **dos veces** (una por base de datos):

1. Lee el documento borrado (`event.data?.data()`) para sacar `ownerId` y `sharedUsers`.
2. Compone los miembros: `[ownerId, ...sharedUsers]`, sin duplicados ni vacíos.
3. Escribe **en la misma base de datos que disparó el evento** (`getFirestore(databaseId)`).
4. Lee los documentos con `getAll` y **solo actualiza los que existen**.
5. Un `batch` con `arrayRemove(vehicleId)` por miembro.

### Por qué esos dos cuidados del paso 3 y 4

- **La base de datos correcta**: escribir en la otra no daría error — limpiaría el sitio
  equivocado y la función reportaría éxito. Un fallo silencioso.
- **Solo documentos existentes**: un `update` sobre un documento inexistente hace fallar el
  **batch entero**, y basta con que un miembro haya borrado su cuenta para que no se limpie nada.

**Idempotencia**: `arrayRemove` sobre un id que ya no está no hace nada, así que un reintento de
la función (Cloud Functions puede reintentar) es inofensivo.

**Permisos**: el Admin SDK salta las reglas, que es exactamente lo que se necesita aquí.

## Problema encontrado al desplegar: dependencia rota

El primer despliegue falló con `Cannot find module '@firebase/app'`. La cadena era:

```
firebase-functions/v2  →  provider de Realtime Database  →  firebase-admin/database  →  @firebase/database-compat
```

`@firebase/database-compat` declara `@firebase/app` como *peer dependency* y npm no la instala
(ni siquiera tras un `npm install` limpio). El barril `firebase-functions/v2` carga **todos** los
providers, incluido uno de Realtime Database que este proyecto no usa.

Solución: importar el logger desde su entrada propia (`firebase-functions/logger`) en lugar del
barril. Además de arreglarlo, es mejor práctica: se carga solo lo que se usa.

## Riesgos

- **Primer despliegue**: al ser las primeras funciones de 2ª generación del proyecto, el agente
  de servicio de Eventarc tarda unos minutos en propagar permisos y el primer intento falla con
  un aviso explícito que pide reintentar. Es transitorio.
- **Límite de 500 escrituras por batch**: irrelevante en la práctica (los miembros por vehículo
  se cuentan con los dedos), pero queda documentado.
- **Coste**: la capa gratuita de Blaze cubre de sobra una función que solo se ejecuta al borrar
  un vehículo.

## Resolución de las preguntas abiertas de la spec

1. **Plan Blaze** → confirmado por el usuario.
2. **Una función por base de datos** → **dos exports** desde el mismo manejador, cada uno con su
   opción `database`. Un único despliegue los publica ambos.
3. **Limitar el fan-out / paginar** → **no**. Los miembros por vehículo están muy lejos del
   límite de 500 de un batch; queda documentado por si algún día cambia.
4. **Función de mantenimiento puntual** → **no hace falta**: se comprobó que no hay IDs colgantes
   en los datos actuales.
