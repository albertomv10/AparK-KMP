# Navegación y arquitectura de información

Dónde vive cada pantalla, y por qué. Es la pregunta que se repite en cada spec, igual que
[`DATA-MODEL.md`](DATA-MODEL.md) responde a dónde van los datos nuevos.

---

## El principio

AparK tiene **dos modos de uso muy distintos**, y mezclarlos es lo que hace que una app se sienta
torpe:

| | Cuándo | Qué exige |
|---|---|---|
| **Diario, diez segundos** | *¿dónde está el coche?* · *acabo de aparcar* | Instantáneo, sin navegar |
| **Ocasional, de gestión** | reportar que hay que lavarlo, mirar la ITV, añadir a alguien, cambiar el tema | Puede costar dos toques |

**Todo lo que traen las fases 2 y 3 es del segundo tipo.** Si acaba en la Home, estropea el primero,
que es lo que la gente hace a diario. De ahí sale la estructura entera.

---

## El mapa de la app

```
Mapa ─────────── dónde están · aparcar
  mapa + carrusel + «Aparcar aquí»
  └── + Añadir vehículo  (crear · unirse con código)

Vehículos ────── cómo están · gestionar
  lista con el ESTADO de cada uno          chapita: pendientes totales
  └── Detalle del vehículo
       ├── Datos · Miembros · Compartir
       ├── Incidencias
       ├── Mantenimiento · ITV
       └── Ubicaciones guardadas

Cuenta ───────── quién eres · cómo se comporta
  foto · nombre · email
  Notificaciones · Tema · Idioma
  Cerrar sesión · Eliminar cuenta
```

---

## La regla que mantiene «Mapa» y «Vehículos» separados

> **El carrusel muestra *dónde está*. La lista muestra *cómo está*.**

Es la línea que impide que la segunda pestaña sea un duplicado de la primera, y hay que respetarla
a rajatabla. Una fila de la lista enseña lo que el mapa no puede:

```
Tiguan   5777GTJ      2 incidencias abiertas · ITV en 23 días
```

**Criterio para juzgarla cuando exista**: si acaba mostrando lo mismo que el carrusel, es redundante
y hay que quitarla.

El motivo de fondo es que la app es **compartida**. Su valor no es solo saber dónde está el coche,
sino que todos sepan **qué le pasa**. Una chapita con los pendientes es lo que convierte eso en algo
que se ve sin buscarlo — y eso el mapa no lo da.

---

## Cómo crece el detalle del vehículo

Hoy es un nombre y un botón de compartir. Va a acumular datos, miembros, incidencias, mantenimiento,
ITV y ubicaciones, que es demasiado para una pantalla.

**Cada área empieza como una fila de resumen y gana su propia subpantalla cuando tiene contenido.**

```
Tiguan · 5777GTJ
──────────────────────────────
Estado          2 incidencias  ›
Vencimientos    ITV en 23 días ›
Miembros        Edu, Mari, tú  ›
Mantenimiento   3 registros    ›
Ubicaciones     Garaje         ›
──────────────────────────────
Editar · Compartir · Eliminar
```

Así cada fase añade su sección sin rediseñar nada, y una sección vacía simplemente no se muestra.

---

## Qué pantallas no llevan pestañas

La barra acompaña a **todo lo que vive dentro de una pestaña**, incluido el detalle de un vehículo:
desaparecerla al entrar en un detalle da sensación de haberse salido de la app.

Se oculta solo en lo que es **una tarea de pantalla completa**, con principio y fin:

- Splash, login, registro, verificación de email, recuperar contraseña
- Añadir vehículo

---

## Decisiones tomadas, y lo que cuestan

**Tres pestañas y no dos.** Dos obligaría a meter la gestión detrás del mapa, y las incidencias no
tendrían dónde verse de un vistazo. El coste es una pestaña que, hasta que lleguen las fases 2 y 3,
será sobria.

**Cuenta es una sola pantalla**, no «Cuenta → Ajustes». Para cinco o seis ajustes, una pantalla
intermedia es ceremonia. *Notificaciones* sí abrirá su propia subpantalla cuando tenga varios
interruptores. Si los ajustes crecen mucho, se separa entonces.

**Fuera el menú de tres puntos de la Home.** Tenía el cierre de sesión, que es una acción de cuenta
y no de mapa. Se elimina entero en lugar de dejarlo vacío; si algún día aparece una opción que sí
sea del mapa —tipo satélite, por ejemplo— se reintroduce con sentido.

**Añadir vehículo se llega desde los dos sitios**: la tarjeta «+» del carrusel, que ya existe y es
donde se descubre, y la lista de *Vehículos*. Unirse con un código sigue siendo una pestaña dentro
de esa pantalla.

---

## Lo que esto deja fuera a propósito

- **Una pestaña de «Pendientes»** que agregue incidencias y vencimientos de todos los vehículos.
  Tentador, pero con dos o cuatro coches es trocear de más: la chapita en *Vehículos* y los
  pendientes arriba de esa lista cubren lo mismo con una pestaña menos.
- **Un cajón lateral.** No hay suficientes secciones para justificarlo, y compite con las pestañas.
- **Buscar.** Con un puñado de vehículos no hay nada que buscar.

Estas tres se reconsideran si algún día un usuario tiene diez vehículos, no antes.
