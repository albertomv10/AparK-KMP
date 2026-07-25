# Tasks: <Nombre de la feature>

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

Marca cada tarea al completarla. Agrupadas por capa.

## Domain
- [ ] <caso de uso / interfaz de repositorio>

## Data
- [ ] <implementación en repositorio / Firestore>

## Presentation
- [ ] <ViewModel / pantalla / componentes>

## Reglas Firestore
- [ ] <cambio en `firestore.rules` + despliegue a ambas BDs>

## Documentación
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] Compila Android (`./gradlew :composeApp:compileDebugKotlinAndroid`)
- [ ] Compila iOS-Kotlin (`./gradlew :composeApp:compileKotlinIosSimulatorArm64`)
- [ ] Se cumplen todos los criterios de aceptación de la spec
- [ ] Prueba manual en emulador/simulador
