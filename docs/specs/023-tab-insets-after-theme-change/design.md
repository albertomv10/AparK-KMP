# Design: que nada se mueva al cambiar de tema en iPhone

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

**El shell es el dueño del margen inferior, y lo dice.** Los `Scaffold` de dentro de `TabShell`
dejan de pedir el inset de abajo: piden solo el de arriba y los de los lados. Así no queda nada que
descontar, y da igual que el `Scaffold` rehaga sus márgenes tras una recomposición completa: los
rehace bien.

Hoy esos `Scaffold` piden el inset completo y confían en descontar lo que el shell ya consumió. Ese
descuento es justo lo que falla en iOS (ver la spec: la identidad de `WindowInsets.systemBars`, el
`remember` de Material y `ConsumedInsetsModifierNode.update()` en foundation-layout 1.10.1). Subir
Compose a 1.12, que lo arregla en la librería, queda para una spec propia.

## Lo que dijo el código antes de decidir nada

1. **Hay siete `Scaffold` dentro del shell, para nueve pantallas**: *Inicio*, *Vehículos*, el
   detalle, añadir, editar, *Cuenta* y `AccountFormScaffold`, que comparten Cambiar email, Cambiar
   contraseña y Borrar cuenta.
2. **`TabShell` solo consume abajo**: `consumeWindowInsets(PaddingValues(bottom = …))`. Arriba y a
   los lados nadie consume nada, así que para esos lados el descuento ya no hacía nada, con el fallo
   o sin él.
3. **Con barra de pestañas y sin ella, el shell ya deja libre el indicador de inicio.** Con barra, el
   `Scaffold` del shell reserva su alto, que lo incluye; sin barra, reserva el inset del sistema.
   Ninguna pantalla de dentro necesita pedirlo otra vez.
4. **Los modificadores de padding no tienen el problema**: el nodo de `imePadding` o
   `navigationBarsPadding` se recalcula cuando recibe insets nuevos. Solo falla el camino de
   `onConsumedWindowInsetsChanged`, que es el que usa el `Scaffold`.
5. **Los `imePadding()` de los formularios sí dependen de lo que consume el shell**: es lo que evita
   que el teclado sume otra vez la barra o el indicador.

## El contrato: `TabShellDefaults`

En `presentation/navigation/TabShell.kt`, junto al shell y al estilo de `ScaffoldDefaults`:

```kotlin
object TabShellDefaults {
    val contentWindowInsets: WindowInsets
        @Composable get() = ScaffoldDefaults.contentWindowInsets
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
}
```

- Parte de `ScaffoldDefaults.contentWindowInsets`, no de `systemBars`, para quedarse con lo que
  Material usa por defecto en cada plataforma.
- KDoc con el porqué y la spec, porque es lo que verá quien abra una pantalla.

## Los cambios

| Fichero | Cambio |
|---|---|
| `navigation/TabShell.kt` | `TabShellDefaults`; el comentario del `consumeWindowInsets` del shell dice que se queda por el teclado |
| `home/HomeScreen.kt` | `Scaffold(contentWindowInsets = …)`. `paddingValues.calculateBottomPadding() + 24.dp` se queda: ahora es siempre 0 + 24, pero dice lo que significa |
| `vehicles/VehiclesScreen.kt` | ídem; el botón flotante se coloca con este inset y queda donde está |
| `vehicledetail/VehicleDetailScreen.kt` | ídem; su comentario daba `consumeWindowInsets(padding)` como el arreglo del indicador de inicio, y se corrige: le cuenta a lo de dentro el alto de la barra superior |
| `addvehicle/AddVehicleScreen.kt` · `editvehicle/EditVehicleScreen.kt` · `account/AccountScreen.kt` | ídem |
| `account/AccountForm.kt` | ídem, en `AccountFormScaffold` |

**No se tocan**: las pantallas de autenticación (fuera del shell, nadie consume por encima); el
`consumeWindowInsets` del shell; la hoja de reportar incidencia.

## Alternativas descartadas

- **Subir Compose a 1.12.** Es el arreglo de fondo, pero también una spec entera: el ciclo de vida
  está atado a la versión de Compose, Navigation 3 está en alpha, y hay que revisar Compottie,
  maps-compose y los recursos. Queda como spec propia, y este contrato sigue siendo correcto después.
- **Un envoltorio `TabScaffold`.** Se heredaría al copiar una pantalla, pero esconde un parámetro de
  Material. La constante es una línea visible por pantalla.
- **Insets estables con `remember`** (el experimento de la spec). Imita el arreglo de la librería,
  pero se queda viejo si los insets cambian de verdad, y es más críptico.
- **Una comprobación automática** que busque `Scaffold(` sin el parámetro. El proyecto no tiene
  detekt ni lint propio, y montarlo para esto pesa más que la regla escrita.

## Riesgo

Una pantalla nueva dentro del shell que olvide el parámetro vuelve a fallar, solo en iPhone y solo
tras cambiar el tema. Se mitiga con la regla en `AGENTS.md` y `NAVIGATION.md`, y con que todas las
pantallas existentes la lleven: se copia sola.
