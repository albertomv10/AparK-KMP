# Design: la ubicación predeterminada de un vehículo

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

La ubicación vive **dentro del propio vehículo**, en un campo `defaultPlace`. Aparcar en ella deja una
marca en `lastLocation`. No hace falta colección nueva ni escuchas nuevas: el vehículo ya se escucha
en todas partes.

Se elige en una pantalla nueva con un mapa que se mueve bajo un marcador fijo. En la tarjeta del
mapa, un segundo botón aparca en ella sin GPS y sin esperar al servidor, así que funciona en un
garaje sin cobertura.

## Lo que dijo el código antes de decidir nada

1. **`Vehicle` se lee directamente del documento**, con kotlinx.serialization. El decodificador de
   gitlive recorre los campos de la clase, así que las versiones anteriores ignoran un campo nuevo,
   como ya pasó con los de la spec 015.
2. **Aparcar reescribe `lastLocation` entero.** Una versión anterior, al aparcar, borra cualquier
   campo nuevo de `lastLocation`, que es justo lo que hace falta (criterio 11).
3. **Las funciones solo tocan `lastLocation.user.*`**, así que no les afecta.
4. **Las reglas** dejan al dueño cambiar lo que quiera, validado campo a campo; un miembro solo
   aparca o sale.
5. **`UpdateVehicleLocationUseCase` ya aparca en un punto dado** sin pedir el GPS. `HomeViewModel`, en
   cambio, espera a que termine la escritura, y sin conexión no termina nunca.
6. **Los marcadores:**
   - Android usa el pin por defecto (`defaultMarker`), un `BitmapDescriptor` opaco sobre el que no se
     puede dibujar;
   - iOS los pinta en Swift con `GMSMarker.markerImage(with:)`, que sí devuelve una imagen.
7. **El mapa de iOS sale de una fábrica de Swift** y es único: un segundo mapa necesita la suya.
   Compose ya dibuja encima del mapa nativo, así que el marcador fijo del centro va en código común.

## Datos

`vehicles/{id}`:
- **`defaultPlace: {name, latitude, longitude}`**: no está si no hay ubicación. El nombre va de 1 a
  40 caracteres, sin contar los espacios de los bordes.
- **`lastLocation.atDefaultPlace: true`**: solo si se aparcó con su botón. Aparcar con el GPS,
  arrastrar el marcador o aparcar desde una versión anterior la quitan.

**Una marca, y no una copia del nombre.** Con una sola ubicación en el mismo documento, el nombre ya
está a mano:
- si alguien la renombra, la tarjeta lo dice al momento;
- si la quita, el vehículo deja de estar «en su ubicación»;
- si la mueve con el coche ahí, el pin con la casa se queda donde está el coche, que es donde de
  verdad está.

El `ROADMAP.md` proponía copiar el nombre, pero pensando en varias ubicaciones en una subcolección,
donde leer el nombre costaba otra lectura.

## Reglas

- **`defaultPlaceOk`:** el campo no está, o es un mapa con exactamente nombre, latitud y longitud,
  cada uno dentro de su rango.
- **El dueño** la valida como un campo más (`changedVehicleDataOk`), también al crear el vehículo.
- **Una rama nueva deja a cualquier miembro** cambiar `defaultPlace` y `updatedAt`, y nada más
  (criterio 4). Quitarla es borrar el campo.
- **Al aparcar**, `atDefaultPlace`, si está, tiene que ser booleano.

Hay que desplegarlas a `apark-dev` antes de probar en un móvil, y a producción antes de publicar.

## Interfaz

- **La pantalla de elegir el punto** (`DefaultPlace`, sin barra de pestañas):
  - un mapa entero con el marcador fijo en el centro, el nombre («Garaje» por defecto), «Guardar» y
    «Quitar»;
  - empieza en la ubicación que ya tenga, o donde está aparcado, o donde estás tú, o en España;
  - guardar y quitar no esperan al servidor.

  El mapa es `PlacePickerMap`, `expect`/`actual`:
  - en Android, `GoogleMap` con su estado de cámara;
  - en iOS, una fábrica de Swift nueva y su propio controlador, que avisa al pararse la cámara.
- **El detalle**: en «Dónde está», una fila «Ubicación predeterminada». La frase larga dice dónde:
  «Aparcado hace 5 min en Garaje por Edu».
- **La tarjeta**: con ubicación, «Aquí» y el botón de la ubicación, del mismo tamaño. El de la
  ubicación confirma al momento, con «Deshacer», y lanza la escritura sin esperarla.
- **La frase corta**, si está en su ubicación: «Garaje · hace 5 min», sin quién, que no cabe junto a
  los iconos. El lector de pantalla dice la larga.
- **El marcador:**
  - en iOS, el pin de siempre con una casa blanca dibujada en la cabeza;
  - en Android, todos los marcadores pasan a `MarkerComposable` con un pin propio, para que el normal
    y el de la casa tengan la misma forma. Cambia un poco el aspecto de los pines de Android.

## Riesgos

- **Los pines de Android cambian de aspecto**, todos. Lo tiene que ver Alberto.
- **Si alguien quita la ubicación con el coche dentro**, el coche deja de contar como «en su
  ubicación» aunque no se haya movido. Es lo esperable: ya no hay ubicación que nombrar.
- **Las reglas tienen que llegar antes que la app.** Una app nueva contra reglas viejas no puede
  guardar la ubicación, y un miembro que no es el dueño tampoco puede aparcar con la marca: la rama de
  aparcar no la admite hasta que se despliegan.

## Lo que salió al probarla

- **El pin de Android se dibuja a mano.** Primero fue el icono `Place` de Material: más pequeño que el
  de Google, y con un agujero en la cabeza por el que se veía el mapa, así que la casa blanca no se
  distinguía. Ahora es un `Canvas` con la forma y el tamaño del de Google, la cabeza sólida, el borde
  y el punto oscuros, y anclado en la punta.
- **Una pulsación larga sobre un pin no aparca.** Los dos SDK avisan al mapa también cuando el dedo
  está sobre un pin: iOS siempre, y Android si el pin no se puede arrastrar. El elegido saltaba al
  dedo, o encima de otro vehículo, y en iOS el arrastre se cortaba. Cada plataforma comprueba primero
  si la pulsación cae sobre un pin (`isOnMarker` en Swift, `isOnPin` en Kotlin).
- **Solo se arrastra el pin del vehículo elegido**, para no mover otro sin querer.
- **El destino se llama `DefaultPlaceEditor`**, porque `DefaultPlace` ya es la clase del modelo.
