# Spec: Reordenar vehículos

- **ID**: 003-reorder-vehicles
- **Estado**: En revisión
- **Fecha**: 2026-07-25

## Problema / Por qué

Los vehículos aparecen en el carrusel en el orden en que se añadieron, sin que el usuario
pueda cambiarlo. El coche que usas a diario puede acabar en la última posición mientras uno
que apenas tocas ocupa la primera.

No es teórico: al verificar la feature de borrado, llegar al último vehículo de una lista de
trece exigió **once deslizamientos**. Con la app en la mano, en la calle y con prisa, eso es
justo lo contrario de lo que se necesita.

El modo edición (pulsación larga sobre una tarjeta) se diseñó ya como punto de entrada para
esto; ahora solo permite eliminar.

## Objetivos

- Que el usuario pueda **cambiar el orden** de las tarjetas del carrusel.
- Que el orden elegido **persista** entre sesiones y dispositivos.
- Que reordenar sea **seguro**: sin perder vehículos ni dejar la lista inconsistente.

## No-objetivos

- Ordenación **automática** (por frecuencia de uso, cercanía, etc.).
- Agrupar vehículos en carpetas o categorías.
- Cambiar el orden para **otros** miembros de un vehículo compartido.

## Contexto técnico favorable

Dos hechos ya verificados que hacen esta feature especialmente barata:

- **El orden mostrado *es* el orden del array `userVehicles`** del documento del usuario:
  `getVehiclesForUser` construye la lista recorriendo ese array y `combine` conserva el orden.
- **Las reglas ya permiten** al usuario escribir su propio documento (`match /users/{userId}`).

Por tanto reordenar es **reescribir ese array**: sin cambios en el modelo de datos ni en las
reglas de Firestore.

Como consecuencia, el orden es **por usuario**: cada miembro de un vehículo compartido tiene
el suyo, y reordenar nunca afecta a los demás.

## Historias de usuario

- Como usuario con varios vehículos, quiero poner los que más uso al principio, para llegar a
  ellos sin deslizar media lista.
- Como usuario, quiero que ese orden siga igual la próxima vez que abra la app.

## Criterios de aceptación

1. **Dado** que estoy en modo edición con varios vehículos, **cuando** cambio uno de posición,
   **entonces** el carrusel refleja el nuevo orden inmediatamente.
2. **Dado** que he reordenado, **cuando** cierro y vuelvo a abrir la app, **entonces** el orden
   se mantiene.
3. **Dado** que he reordenado, **cuando** consulto Firestore, **entonces** el array
   `userVehicles` de mi documento refleja el nuevo orden.
4. **Dado** un vehículo compartido, **cuando** yo cambio su posición, **entonces** el orden de
   los demás miembros **no** se altera.
5. **Dado** un fallo al guardar el nuevo orden, **cuando** ocurre, **entonces** se muestra un
   mensaje de error y la lista no queda en un estado inconsistente.
6. **Dado** que reordeno, **cuando** se guarda, **entonces** no se pierde ningún vehículo de la
   lista (ver riesgo de sobrescritura más abajo).

## Decisión de interacción: flechas ◀ ▶

Arrastrar una tarjeta para reordenarla **choca de frente con el gesto horizontal del propio
`HorizontalPager`**, que ya usa el arrastre lateral para cambiar de vehículo. Se descarta por
ese conflicto.

Se opta por **botones ◀ ▶ en cada tarjeta durante el modo edición**, que mueven el vehículo una
posición. Sin ambigüedad de gestos, y el modo edición ya existe como contexto.

También se descarta reordenar desde la pestaña **"Mis Coches"**: la pantalla de inicio ya
permite recorrer y eliminar vehículos, y editarlos será cosa de la futura pantalla de detalle,
así que una pantalla adicional que liste vehículos no aporta nada por ahora. (Esa pestaña sigue
siendo cosmética; qué hacer con ella se decidirá aparte.)

## Preguntas abiertas

(A resolver en `design.md`.)

1. **Cuándo persistir**: ¿en cada pulsación de flecha, o una sola escritura al salir del modo
   edición? Lo segundo evita ráfagas de escrituras en Firestore, pero complica el manejo de
   errores y el estado intermedio.
2. **Feedback visual**: ¿basta con el modo edición ya existente, o conviene alguna señal de que
   las tarjetas se pueden mover? ¿Cómo se indica que la primera/última no puede ir más allá:
   ocultando la flecha o deshabilitándola?
3. **Alcance del movimiento**: ¿solo una posición por pulsación, o mantener pulsado para mover
   varias de golpe?

## Riesgo conocido: sobrescritura del array

Guardar el orden implica **reescribir `userVehicles` entero**, no un `arrayUnion`/`arrayRemove`
puntual. Eso abre una condición de carrera real: si entre que se lee la lista y se escribe el
nuevo orden se añade un vehículo (desde otro dispositivo, o en otra pantalla), la escritura
puede **borrarlo de la lista**.

No es hipotético: ese error ya ocurrió en este proyecto durante una limpieza manual de datos y
dejó dos vehículos huérfanos. El diseño debe abordarlo explícitamente (transacción, o partir
siempre del estado más reciente en lugar de una copia en memoria).

## Fuera de alcance

- Reordenar desde la pantalla de detalle (que aún no existe).
- Sincronizar el orden entre miembros de un vehículo compartido.
- Deshacer un reordenado.
