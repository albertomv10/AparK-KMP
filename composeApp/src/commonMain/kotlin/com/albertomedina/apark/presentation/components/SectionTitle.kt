package com.albertomedina.apark.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * El título de una sección: pequeño y en el color primario, como en *Cuenta* y en el detalle. Para
 * el lector de pantalla es un encabezado, así que se puede saltar de sección en sección.
 *
 * @param start lo que se separa del borde: 24 en las pantallas de ajustes, 16 en las listas, para
 *   alinearse con sus filas.
 * @param action lo que se hace con la sección, a la derecha del título: «Ordenar» en *Vehículos*
 *   (spec 020). Con ella, la fila mide lo que un botón, tenga botón en ese momento o no, para que el
 *   título no salte al aparecer o desaparecer.
 */
@Composable
fun SectionTitle(text: String, start: Dp = 24.dp, action: (@Composable RowScope.() -> Unit)? = null) {
    if (action == null) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = start, top = 20.dp, bottom = 4.dp)
                .semantics { heading() }
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = start, end = 8.dp, top = 8.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        action()
    }
}
