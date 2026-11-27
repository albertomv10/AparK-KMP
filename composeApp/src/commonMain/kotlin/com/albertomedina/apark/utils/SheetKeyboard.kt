package com.albertomedina.apark.utils

/**
 * Si el teclado encoge el diálogo de una `ModalBottomSheet` en lugar de llegarle como margen.
 *
 * En iOS, Compose Multiplatform monta la hoja en un `Dialog` con `useSoftwareKeyboardInset` activo, su
 * valor por defecto: el diálogo entero se encoge hasta el borde del teclado, y dentro `WindowInsets.ime`
 * vale 0. Pero el margen del indicador de inicio (34 pt) sigue llegando, aunque el teclado lo tape, y la
 * hoja lo reserva debajo del contenido: un hueco entre el formulario y el teclado. La propia librería
 * desactiva esa opción en `BasicEdgeToEdgeDialog`, pero no en la hoja. En Android, el teclado llega como
 * margen y la hoja lo descuenta bien.
 */
expect val sheetShrinksAboveKeyboard: Boolean
