# Tasks: el splash de Android, como el de iOS

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Android
- [x] `Theme.AparK.Starting` con `Theme.SplashScreen`; `forceDarkAllowed=false` en los dos temas
- [x] El manifest y `installSplashScreen()` en `MainActivity`
- [x] `splash_logo.xml`: el vector cuadrado de 288 dp con el logo centrado
- [x] Fuera `splash.png`, `splash_image_sin_fondo.png`, `playstore.png`, `ic_launcher_playstore.png` y `logosinfondosvg.xml`

## iOS
- [x] El logo centrado con restricciones en `Launch Screen.storyboard`

## Documentación
- [x] `AGENTS.md` y `CHANGELOG.md`

## Verificación
- [x] Compila Android y Xcode
- [x] **(1)(2)** Emulador API 36: splash blanco y logo nítido, en modo claro y en modo oscuro
- [x] **(5)(6)** iPhone 16e y 17 Pro Max: el logo centrado e igual que antes
- [ ] **(3)** Android 7 a 11: no hay emulador de esas versiones; lo cubre la librería, que existe para eso
- [ ] **(2)(4)** El Xiaomi con el modo oscuro forzado — **Alberto**
