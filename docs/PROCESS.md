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

## Convenciones

- **Ramas**: `feature/nombre`, `fix/nombre`, `docs/nombre`, `chore/nombre`.
- **Commits**: prefijo tipo Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`).
- **Integración**: siempre vía **Pull Request** a `main` (no push directo a `main`).
- **Numeración de specs**: correlativa; no se reutilizan números.
