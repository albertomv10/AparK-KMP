# Cómo trabajamos — Spec-Driven Development (SDD)

Esta guía describe el método de trabajo del proyecto AparK y cómo colaborar con el
asistente (Claude Code) siguiéndolo. La idea central de SDD es simple:

> **Primero acordamos _qué_ y _por qué_ (la spec). Luego _cómo_ (el diseño). Luego
> _los pasos_ (las tareas). Y solo entonces se escribe código.**

Escribir el código es la parte fácil y rápida; las decisiones caras son las de antes.
SDD las pone por escrito y las hace revisables **antes** de invertir esfuerzo.

---

## El bucle

Cada funcionalidad (o cambio no trivial) pasa por estas fases:

```
1. Spec   →  2. Design  →  3. Tasks  →  4. Implementación  →  5. Verificación  →  6. Documentación  →  7. PR
   (qué/       (cómo)       (pasos)      (código)              (contra criterios   (CHANGELOG)          (merge)
    por qué)                                                    de aceptación)
```

Cada fase produce un artefacto revisable y **no se avanza a la siguiente sin acuerdo
sobre la anterior**. Es normal volver atrás: si al diseñar aparece algo que cambia el
qué, se actualiza la spec.

---

## Artefactos y dónde viven

Por cada feature se crea una carpeta numerada:

```
docs/specs/NNN-nombre-corto/
├── spec.md     # QUÉ y POR QUÉ  (fase 1)
├── design.md   # CÓMO           (fase 2)
└── tasks.md    # PASOS          (fase 3, se va marcando durante la 4)
```

- `NNN` es un número correlativo de tres dígitos (`001`, `002`, …).
- Las plantillas están en [`docs/specs/_template/`](specs/_template/).
- El histórico de lo entregado se registra en [`CHANGELOG.md`](../CHANGELOG.md).

---

## Cómo mapea cada fase al trabajo con Claude Code

| Fase | Qué haces tú | Qué hace el asistente |
|------|--------------|------------------------|
| **1. Spec** | Describes la idea en una o dos frases. Revisas y corriges el borrador. | Redacta `spec.md`: problema, objetivos, historias de usuario, **criterios de aceptación**, preguntas abiertas y fuera de alcance. |
| **2. Design** | Entras en **modo plan** de Claude y respondes las preguntas abiertas. Apruebas con `ExitPlanMode`. | Investiga el código, propone el enfoque técnico y lo guarda en `design.md` (archivos afectados, cambios de datos/reglas, alternativas, riesgos). |
| **3. Tasks** | Revisas la lista. | Desglosa el diseño en `tasks.md` como checklist por capas. |
| **4. Implementación** | Supervisas. | Codifica siguiendo `tasks.md` y va marcando `[x]`. |
| **5. Verificación** | Pruebas en emulador/simulador. | Compila (Android + iOS-Kotlin), verifica datos con el MCP de Firebase y comprueba **cada criterio de aceptación** de la spec. |
| **6. Documentación** | — | Añade la entrada al `CHANGELOG.md`. |
| **7. PR** | Revisas y mergeas en GitHub. | Abre el PR con `gh`. |

> **Regla práctica**: el **modo plan** de Claude Code *es* la fase de Design. La
> aprobación del plan (`ExitPlanMode`) equivale a aprobar el `design.md`.

---

## Cómo arrancar una feature (tú)

Basta con una frase del tipo:

> "Quiero empezar la feature de _eliminar vehículo_. Redacta la spec."

El asistente creará `docs/specs/NNN-.../spec.md` y lo revisaréis juntos antes de diseñar.

---

## Definition of Done

Una feature está terminada cuando:

- [ ] `spec.md`, `design.md` y `tasks.md` existen y están acordados.
- [ ] Todas las tareas de `tasks.md` están marcadas.
- [ ] Se cumplen **todos** los criterios de aceptación de la spec.
- [ ] Compila en Android e iOS-Kotlin.
- [ ] Reglas de Firestore actualizadas y desplegadas (si aplica).
- [ ] Entrada añadida al `CHANGELOG.md`.
- [ ] PR abierto, revisado y mergeado.

---

## Flujo de trabajo con Git

El ciclo completo, tal y como se aplicó en la feature `001-delete-vehicle`.

### 1. Partir de `main` actualizado

Siempre se ramifica desde la última versión integrada, nunca desde una rama vieja:

```sh
git checkout main
git pull origin main
```

### 2. Crear la rama de trabajo

```sh
git checkout -b feature/delete-vehicle
```

### 3. Commitear en unidades lógicas

No un commit gigante al final: **uno por idea**. La feature de borrado acabó con cuatro,
y cada uno se entiende y se revierte por separado:

```
feat: delete vehicle from an edit mode on the home carousel
fix: let a shared member leave a vehicle
fix: lock and dim the map while editing, and tap it to exit
feat: exit edit mode with the back gesture
```

El primero es la funcionalidad; los otros tres son correcciones que aparecieron **al
verificar**. Que la verificación genere commits es normal y sano.

### 4. Subir la rama

La primera vez, con `-u` para enlazarla con su rama remota:

```sh
git push -u origin feature/delete-vehicle
```

### 5. Abrir el Pull Request

```sh
gh pr create --base main --head feature/delete-vehicle --title "..." --body "..."
```

### 6. Seguir puliendo sobre la misma rama

Cada `git push` posterior **actualiza el PR automáticamente**. No se cierra ni se abre otro.

### 7. Tras mergear, sincronizar y limpiar

```sh
git checkout main
git pull origin main
git branch -d feature/delete-vehicle
git fetch --prune
```

`git branch -d` (minúscula) solo borra la rama si está realmente mergeada: es una red de
seguridad. Si se niega, es señal de que algo no se integró como esperabas — investiga antes
de forzar con `-D`.

---

### Comandos de diagnóstico

En el día a día se consulta el estado mucho más de lo que se escribe:

```sh
git status --short                      # qué he tocado
git log main..mi-rama --oneline         # qué llevo que main no tenga
git diff main -- ruta/al/archivo.kt     # cambios de un archivo frente a main
git log --oneline --graph               # forma del historial
```

La notación `A..B` significa "lo que tiene B y no tiene A". Es la forma fiable de saber
**qué vas a mergear realmente**.

---

### Lecciones aprendidas (casos reales de este proyecto)

**Diagnostica antes de forzar.** Al abrir el primer PR, `main` local y `origin/main` no
coincidían y parecía una divergencia. El diagnóstico lo aclaró:

```sh
git merge-base main origin/main    # devolvió exactamente origin/main
git log main..origin/main          # vacío
```

No había divergencia: `main` local simplemente llevaba 10 commits sin subir. Un
`push --force` o un rebase "para arreglarlo" habría destruido historia sana. **Cuando algo
no cuadra, `merge-base` y los rangos `A..B` dicen la verdad.**

**Un archivo puede repartirse entre varios commits.** El commit de la feature de añadir
vehículo y el de resiliencia tocaban ambos `FirestoreVehicleRepository.kt`. Se separaron
dejando el archivo en el estado del primer commit, commiteando, y restaurando después la
otra parte. Con `git add -p` (o el selector de cambios de Android Studio) puedes
**commitear solo unas líneas concretas** de un archivo.

**Las ramas abandonadas se dejan en paz.** Existía una `feature/add-vehicle` previa que no
convenía continuar. En vez de borrarla o mezclarla, se revisó qué merecía rescatarse y se
rehízo el trabajo limpio en una rama nueva:

```sh
git diff main..feature/add-vehicle --stat
```

**La forma del historial importa.** Los merges de PR crean un commit de merge que conserva
la rama como una burbuja visible en el grafo. Así se ve de un vistazo qué commits entraron
juntos, y se puede revertir un PR entero si hace falta.

**Mantén los PR pequeños.** Si una rama empieza a acumular cambios de temas distintos, la
señal es partirla, no seguir añadiendo.

---

## Convenciones

- **Ramas**: `feature/nombre`, `fix/nombre`, `docs/nombre`, `chore/nombre`.
- **Commits**: prefijo tipo Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`).
- **Integración**: siempre vía **Pull Request** a `main` (no push directo a `main`).
- **Numeración de specs**: correlativa; no se reutilizan números.
