# Tasks: el splash de Android, como el de iOS

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Android
- [ ] `Theme.AparK.Starting` con `Theme.SplashScreen`; `forceDarkAllowed=false` en los dos temas
- [ ] El manifest y `installSplashScreen()` en `MainActivity`
- [ ] `splash_logo.xml`: el vector cuadrado de 288 dp con el logo centrado
- [ ] Fuera `splash.png`, `splash_image_sin_fondo.png`, `playstore.png`, `ic_launcher_playstore.png` y `logosinfondosvg.xml`

## iOS
- [ ] El logo centrado con restricciones en `Launch Screen.storyboard`

## Documentación
- [ ] `AGENTS.md` y `CHANGELOG.md`

## Verificación
- [ ] Compila Android y Xcode
- [ ] **(1)(2)** Emulador API 36: splash blanco y logo nítido, en modo claro y en modo oscuro
- [ ] **(5)(6)** iPhone 16e y 17 Pro Max: el logo centrado e igual que antes
- [ ] **(3)** Android 7 a 11
- [ ] **(2)(4)** El Xiaomi con el modo oscuro forzado — **Alberto**
