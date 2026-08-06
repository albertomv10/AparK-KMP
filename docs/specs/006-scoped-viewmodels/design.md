# Design: ViewModels con ámbito de pantalla

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado
- **Fecha**: 2026-08-06

## Enfoque

Añadir el decorador que falta a `NavDisplay`. Todo lo demás sale gratis.

```kotlin
entryDecorators = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator()
)
```

Dos cosas que no son evidentes y que costarían una tarde si se pasan por alto:

- **Pasar la lista sustituye a la de por defecto**, no la amplía. Si se pasa solo el decorador de
  `ViewModelStore`, se pierde el de `SaveableStateHolder` y con él todo el `rememberSaveable` de la
  app.
- **El de `ViewModelStore` depende del de `SaveableStateHolder`** para poder entregar
  `SavedStateHandle`s; lo dice su propia documentación. El orden importa.

Con el decorador puesto, cada `NavEntry` recibe su `ViewModelStoreOwner`, `koinViewModel()` lo
resuelve por `LocalViewModelStoreOwner` sin cambiar una sola línea de las pantallas, y al sacar la
entrada de la pila se llama a `clear()` sobre su store.

## La restricción que mandaba: la versión

El artefacto `lifecycle-viewmodel-navigation3` **no existe** para la 2.9.6 que usaba el proyecto.
La versión de Compose Multiplatform aparece por primera vez en `2.10.0-alpha03`.

Lo que decide la versión es a qué runtime de Compose apunta cada una:

| lifecycle | `compose.runtime` que arrastra | Sirve |
|-----------|-------------------------------|-------|
| 2.9.6 | 1.9.3 | el artefacto no existe |
| **2.10.0** | **1.9.3** | ✅ misma que usa el proyecto |
| 2.11.0 | 1.10.2 | obligaría a subir Compose Multiplatform |

Así que **2.10.0**: es estable y encaja exactamente con Compose Multiplatform 1.9.3. La 2.11.0
arrastraría una subida de Compose que no toca ahora.

## Archivos afectados

| Archivo | Cambio |
|---------|--------|
| `gradle/libs.versions.toml` | `androidx-lifecycle` 2.9.6 → **2.10.0**; nuevo alias del decorador |
| `composeApp/build.gradle.kts` | Dependencia en `commonMain` |
| `presentation/navigation/BasicNavigationWrapper.kt` | Los dos decoradores |
| `presentation/addvehicle/*` | **Se borra** el apaño: evento `ScreenOpened` y la bandera del formulario |
| `presentation/vehicledetail/VehicleDetailViewModel.kt` | Se conserva el estado limpio en `load`, ahora por su propio mérito |

## Qué cambia de comportamiento, además de lo buscado

Al cerrar sesión se hace `backStack.clear()`, así que ahora el `HomeViewModel` **se destruye** en
lugar de sobrevivir al cambio de usuario. Va en la dirección correcta: el proyecto ya arrastra la
cicatriz de una fuga de datos entre cuentas (ver el aviso en [`AGENTS.md`](../../../AGENTS.md)),
y esto la hace menos probable, no más. Aun así hay que **probar el ciclo de sesión**, porque es el
camino que más cambia.

Rotar no pierde estado: en Android el decorador consulta `isChangingConfigurations` antes de
limpiar.

## Riesgos

- **Subida de versión transversal**: `lifecycle-viewmodel-compose` y `lifecycle-runtime-compose` los
  usa toda la app, y Koin 4.1.1 se apoya en ellos. Se cubre compilando ambas plataformas y
  ejecutando.
- **Ciclo de sesión**: es el flujo que más cambia y el que hay que ejecutar de verdad, no deducir.
