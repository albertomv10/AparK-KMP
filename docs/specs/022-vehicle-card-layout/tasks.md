# Tasks: la tarjeta del mapa, una fila para cada cosa

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Presentation
- [x] `ParkedText.kt`: `shortParkedLine`, `CardTimeSentences` y `ShortParkedText`
- [x] `HomeScreen.VehicleCard`: el nombre solo, la segunda fila con la frase y los iconos, fuera la flecha; `CardMinHeight` a 160 (el botón ocupa 48, no 40)
- [x] Textos en es, en y fr
- [x] Test de `shortParkedLine`

## Documentación
- [x] `AGENTS.md` y `NAVIGATION.md`
- [x] `CHANGELOG.md`

## Verificación
- [x] Tests en la JVM; compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [x] Los textos nuevos, en el `.cvr` de los tres idiomas
- [x] **(1)(2)** «Coche compartido» entero; «El 01 ene · Edu», tres iconos y «+1» en la segunda fila
- [x] **(3)** Sin flecha, y tocar la tarjeta abre el detalle
- [x] **(4)** El mismo alto al deslizar, también con la tarjeta «+». Con 152 la «+» se quedaba más baja; con 160, igual
- [x] **(5)** «Aún no aparcado», con el icono de AdBlue a la derecha
- [x] **(6)** El lector de pantalla: la tarjeta enseña «01 Jan · you» y anuncia «Parked on 01 Jan by you»; los iconos, «AdBlue»
- [x] **(7)** iOS y Android; en inglés y con la letra al 130 % en Android, todo cabe y las tarjetas miden lo mismo; en tema oscuro en iOS. Aparcar da «Hace un momento · tú»
- [x] El aspecto en el móvil — **Alberto**: va bien
