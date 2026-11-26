# Tasks: que nada se mueva al cambiar de tema en iPhone

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Antes de tocar nada
- [ ] Capturas de referencia sin el arreglo: cada pantalla del shell al abrirla, en iPhone y Android

## Presentation
- [ ] `TabShellDefaults.contentWindowInsets` en `TabShell.kt`, con su KDoc; el comentario del `consumeWindowInsets` del shell
- [ ] Los siete `Scaffold` del shell lo usan; el comentario del detalle, corregido

## Documentación
- [ ] `AGENTS.md` y `NAVIGATION.md`
- [ ] `CHANGELOG.md`

## Verificación
- [ ] Tests en la JVM; compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [ ] **(1)** El mapa no se mueve al cambiar la apariencia del sistema, con el tema de la app en Automático, Claro y Oscuro
- [ ] **(2)** *Cuenta* no gana un hueco al cambiar el tema desde ella
- [ ] **(3)** *Vehículos*, el detalle, añadir, editar, cambiar email, cambiar contraseña y borrar cuenta no se mueven al cambiar la apariencia
- [ ] **(4)** Al abrirlas, todas las pantallas están donde estaban, en iPhone y en Android
- [ ] **(5)** Los formularios suben con el teclado como antes
- [ ] **(6)** Android, igual que antes
- [ ] El aspecto en el móvil — **Alberto**
