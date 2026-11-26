# Tasks: las incidencias de un vehículo

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Reglas y backend
- [x] Las reglas de `incidents`, con sus tests en `tools`
- [x] `deleteVehicleIncidents` en `cleanupVehicleReferences`

## Dominio y datos
- [x] `IncidentCategory`, `IncidentTag`, `fromCode` y `appliesTo`
- [x] `Incident` e `incidentsView`
- [x] `IncidentRepository`, `FirestoreIncidentRepository`, los casos de uso y Koin
- [x] Tests en `commonTest`

## Interfaz
- [x] `ParkedText` generalizado para las frases de tiempo
- [x] `IncidentsViewModel`
- [x] La sección del detalle, con resolver, Deshacer, Resueltas y borrar
- [x] El formulario de reportar
- [x] Textos en `values`, `values-en` y `values-fr`
- [x] La siembra con incidencias, con los disparadores de Functions apagados mientras siembra: si no, la limpieza de los vehículos borrados se llevaba las incidencias recién sembradas

## Después de la spec 021
- [x] Actualizar con `main` y recoger el error de la escucha con `catchListenerFailure`; el repositorio deja de tragárselo con `emptyList()`. Con la lectura denegada solo en el emulador, la sección dice «No se han podido cargar las incidencias» y se queda con el caché (simulador)

## Cambio después de probarla (2026-11-26)
- [x] Reglas: `type` y `title` en lugar de `category` y `tag`, con sus tests (27)
- [x] Dominio y datos: `IncidentType` con `CUSTOM`, `IncidentCategory` solo agrupa; `Incident.type` y `title`
- [x] Formulario: elegir el tipo es obligatorio; «Personalizada» con nombre de hasta 40
- [x] Detalle: sin la línea de la categoría; el nombre con peso, la nota más suave y quién y cuándo separado
- [x] Textos en es, en y fr; la siembra con una personalizada
- [x] Simulador y emulador de Android: el formulario, una personalizada, un tipo desconocido como «Incidencia» y el aspecto nuevo
- [x] Segunda prueba: todos los tipos a la vez, separados por categorías, y un color por categoría en claro y en oscuro (simulador)
- [x] El aspecto en el móvil — **Alberto**, contra `apark-dev`

## Documentación
- [x] AGENTS, DATA-MODEL, FIREBASE (la caducidad, paso 10), ROADMAP y CHANGELOG

## Verificación
- [x] Tests de `tools` y `commonTest`; compila Android, iOS-Kotlin y Xcode
- [x] **(1)(2)(3)** Ver: las abiertas, quién, sin nombre, sin incidencias (simulador)
- [x] **(4)(5)(6)** Reportar, con las etiquetas según el vehículo: en un híbrido de gasolina ni «Poca carga» ni «AdBlue». Las de otro miembro aparecen solas (simulador)
- [x] **(7)(8)(9)(10)** Resolver, Deshacer (quita los tres campos en el servidor), Resueltas, y la de hace 40 días que no sale (simulador)
- [x] **(11)** Borrar: el dueño en cualquiera, con confirmación; Edu solo en las suyas (simulador)
- [x] **(12)(13)(14)** Reglas (23 tests), y una categoría desconocida sale como «Otro», sin la etiqueta desconocida (simulador)
- [x] **(15)** Borrar el vehículo borra sus incidencias (función, en el emulador)
- [x] **(16)** Sin conexión: con los emuladores parados, resolver y reportar se ven al momento y sin error, y llegan al volver (simulador)
- [x] **(17)** En inglés y en francés en iOS; en Android, contra los emuladores: ver, resolver con Undo, reportar con etiqueta y nota, y borrar con confirmación
- [x] El aspecto y Android — **Alberto**. La caducidad real depende de la política TTL, que se crea a mano (FIREBASE.md, paso 10)
