# Tasks: que nada se mueva al cambiar de tema en iPhone

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Antes de tocar nada
- [x] Capturas de referencia sin el arreglo: cada pantalla del shell al abrirla, en iPhone y Android

## Presentation
- [x] `TabShellDefaults.contentWindowInsets` en `TabShell.kt`, con su KDoc; el comentario del `consumeWindowInsets` del shell
- [x] Los siete `Scaffold` del shell lo usan; el comentario del detalle, corregido

## Documentación
- [x] `AGENTS.md` y `NAVIGATION.md`
- [x] `CHANGELOG.md`

## Verificación

Las capturas se compararon píxel a píxel, sin la barra de estado. Con el tema de la app fijo, un
cambio del sistema recompone toda la app sin cambiar ningún color, así que «idéntica» quiere decir
que nada se ha movido.

- [x] Tests en la JVM (108, todos en verde); compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [x] **Control**: la misma prueba, sobre la build sin el arreglo, sí lo detecta. El carrusel del mapa se mueve, y también el botón flotante de *Vehículos*. Los formularios cortos (cambiar contraseña) no lo delatan ni sin el arreglo, porque no llegan abajo
- [x] **(1)** El mapa no se mueve al cambiar la apariencia del sistema, con el tema de la app en Claro y Oscuro (idéntico) y en Automático (misma posición al pasar a oscuro; al volver a claro solo cambian 5 px de una tesela)
- [x] **(2)** *Cuenta* no gana un hueco al cambiar el tema desde ella: tras dos cambios, idéntica a recién abierta
- [x] **(3)** *Vehículos* (arriba y abajo, con el botón flotante), el detalle, editar, añadir, cambiar email, cambiar contraseña y borrar cuenta: idénticas antes y después de cambiar la apariencia
- [x] **(4)** Al abrirlas, las mismas pantallas son idénticas con el arreglo y sin él: 12 en iPhone, 7 en Android (en el mapa de Android solo cambia el indicador de carga)
- [x] **(5)** Editar, con el teclado abierto en el nombre: idéntico con el arreglo y sin él
- [x] **(6)** Android: las 7 pantallas al abrirlas, y *Cuenta* y *Vehículos* al cambiar el modo noche con el tema de la app fijo, idénticas
- [x] El aspecto en el simulador — **Alberto**: se ve bien
