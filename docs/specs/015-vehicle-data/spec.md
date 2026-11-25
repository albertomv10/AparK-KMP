# Spec: datos del vehículo y editarlo

- **ID**: 015-vehicle-data
- **Estado**: Aprobado — diseño en [design.md](design.md)
- **Fecha**: 2026-11-24

## Problema / Por qué

Un vehículo es hoy un nombre y una matrícula, y **no se pueden cambiar** después de crearlo:
`UpdateVehicleUseCase` existe y nada lo llama. `Vehicle.model` y `Vehicle.color` existen desde el
principio y nadie los escribe.

Con más de un vehículo eso se nota: en el mapa todos parecen iguales, y no hay dónde mirar si uno era
diésel o gasolina, o qué etiqueta ambiental tiene para saber dónde se puede aparcar.

Son los puntos 2.1, 2.2 y 2.5 del [ROADMAP](../../ROADMAP.md). **Qué atributos entran, con qué
valores y por qué ya está decidido** en el [catálogo de DATA-MODEL.md](../../DATA-MODEL.md) (§9).
Esta spec lo convierte en pantallas.

## Objetivos

- **Editar el nombre y la matrícula** de un vehículo, que hoy no se puede.
- **Añadirle sus datos**:
  - tipo;
  - sistema y combustible;
  - marca y modelo, con un buscador que filtra mientras se escribe;
  - color;
  - etiqueta ambiental de la DGT.
- **Enseñarlos donde ayudan**: el detalle los muestra todos, y la tarjeta ayuda a reconocer cada
  vehículo de un vistazo, con el tipo y el color.
- Que **solo el dueño** pueda editarlos, y que las reglas lo demuestren y validen los valores.
- Que una versión antigua de la app **no se rompa** al leer un valor que todavía no conoce.

## No-objetivos

- Fecha de matriculación, seguro, notas o foto: llegan con la función que los usa (catálogo §9).
- Versiones o generaciones de un modelo: solo marca y modelo.
- Actualizar la lista de marcas y modelos sin publicar la app.
- Calcular la etiqueta ambiental: se elige, no se deduce.

## Historias de usuario

- Como **dueño**, quiero corregir el nombre o la matrícula de un vehículo sin borrarlo y crearlo de
  nuevo.
- Como **dueño**, quiero apuntar qué tipo de vehículo es, qué combustible usa, su marca, su modelo, su
  color y su etiqueta.
- Como **cualquier miembro**, quiero ver esos datos en la ficha, para no dudar al repostar.
- Como **quien tiene varios vehículos**, quiero distinguirlos de un vistazo.

## Criterios de aceptación

1. **Dado** que soy el dueño, **cuando** abro el detalle, **entonces** puedo editar el vehículo: el
   nombre, la matrícula, el tipo, el sistema, el combustible, la marca, el modelo, el color y la
   etiqueta. Al guardar, todos los miembros ven el cambio sin recargar.
2. **Dado** que soy miembro y no dueño, **cuando** abro el detalle, **entonces** no veo cómo editar. Y
   si un cliente lo intenta, Firestore lo deniega, con tests de reglas que lo prueban.
3. **Dado** que elijo el sistema, **cuando** paso al combustible, **entonces** solo se ofrece lo que
   tiene sentido:
   - un eléctrico no pregunta qué le echas;
   - uno de pila de combustible es de hidrógeno sin preguntar;
   - un híbrido es de gasolina o de diésel.
4. **Dado** que escribo en el campo de la marca, **cuando** voy tecleando, **entonces** debajo aparecen
   las marcas que coinciden, sin importar mayúsculas ni tildes: «skoda» encuentra «Škoda». El modelo
   solo ofrece los de la marca elegida.
5. **Dado** que mi marca o mi modelo no están en la lista, **cuando** lo escribo a mano, **entonces**
   se guarda tal cual. La lista ayuda, pero nunca impide guardar.
6. **Dado** cualquier valor de las listas cerradas, **cuando** se guarda, **entonces** se guarda su
   código y no su texto, y las reglas rechazan un código que no existe o un texto demasiado largo.
7. **Dado** un vehículo con un código que esta versión de la app no conoce, **cuando** se muestra,
   **entonces** sale como «Otro», sin errores. El color y la etiqueta no tienen «Otro»: un valor
   desconocido no se muestra.
8. **Dado** un vehículo con datos, **cuando** lo veo en el detalle, **entonces** aparecen todos; y en
   la tarjeta, al menos el tipo y el color.
9. **Dado** un vehículo sin datos, como cualquiera de los que ya existen, **cuando** lo veo, **entonces**
   no aparecen huecos ni rayas: simplemente no se muestra lo que no hay.
10. **Dado** cualquier idioma de los tres, **cuando** uso la pantalla, **entonces** todo está
    traducido, y cada color de la paleta tiene su nombre para los lectores de pantalla.
11. **Dado** que la lista de marcas y modelos viene de json-car-list, **cuando** se publica la app,
    **entonces** la acompaña su aviso de licencia MIT con la atribución.

## Preguntas abiertas — resueltas

Todas se cerraron en el diseño. El razonamiento completo está en [design.md](design.md).

1. **¿Pantalla o diálogo?** → **Pantalla completa**, sin barra de pestañas.
2. **¿Datos al crear?** → **Sí, opcionales.** **Primero la marca y el modelo**, y el nombre se rellena
   solo con «Marca Modelo», que sigue siendo editable. En el detalle, si un vehículo no tiene datos,
   el dueño ve «Añade los datos del vehículo».
3. **¿El tipo cambia el marcador del mapa?** → **No.** Se ve en la tarjeta, en la lista y en el detalle.
4. **¿Dónde se ven el tipo y el color?** → En la **tarjeta del mapa**, la **lista de *Vehículos*** y el
   **detalle**.
5. **¿Motos con modelos?** → **Solo las marcas**; el modelo se escribe a mano.
6. **¿Dónde se ve la licencia?** → **En un `NOTICE` dentro de la app**, junto al JSON. La pantalla de
   «Licencias» con todas las librerías queda en el ROADMAP para antes de publicar.

## Fuera de alcance

- La ITV y la fecha de matriculación (3.2), el seguro (con las incidencias) y el mantenimiento.
- Enriquecer la lista con los datos de la DGT: es la mejora natural si se queda corta.
- La etiqueta ambiental de otros países, como la Crit'Air francesa.
