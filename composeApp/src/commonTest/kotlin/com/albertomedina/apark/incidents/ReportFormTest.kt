package com.albertomedina.apark.incidents

import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.IncidentType
import com.albertomedina.apark.presentation.incidents.ReportForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** El formulario de reportar (spec 019): cuándo se puede reportar, y los límites. */
class ReportFormTest {

    @Test
    fun `sin elegir nada no se puede reportar`() {
        assertFalse(ReportForm().canSubmit)
        assertTrue(ReportForm().withType(IncidentType.LOW_FUEL).canSubmit)
    }

    @Test
    fun `volver a tocar el elegido no lo quita y tocar otro lo cambia`() {
        val fuel = ReportForm().withType(IncidentType.LOW_FUEL)
        assertEquals(IncidentType.LOW_FUEL, fuel.withType(IncidentType.LOW_FUEL).type)
        assertEquals(IncidentType.OIL, fuel.withType(IncidentType.OIL).type)
    }

    @Test
    fun `una personalizada necesita nombre`() {
        val custom = ReportForm().withType(IncidentType.CUSTOM)
        assertTrue(custom.isCustom)
        assertFalse(custom.canSubmit)
        assertFalse(custom.withTitle("   ").canSubmit)
        assertTrue(custom.withTitle("Silla del bebé").canSubmit)
    }

    @Test
    fun `el nombre escrito se conserva al cambiar de tipo y volver`() {
        val back = ReportForm()
            .withType(IncidentType.CUSTOM)
            .withTitle("Silla del bebé")
            .withType(IncidentType.CLEANING)
            .withType(IncidentType.CUSTOM)
        assertEquals("Silla del bebé", back.title)
        assertTrue(back.canSubmit)
    }

    @Test
    fun `el nombre y la nota no pasan del límite`() {
        val form = ReportForm()
            .withTitle("x".repeat(Incident.TITLE_MAX_LENGTH + 5))
            .withNote("x".repeat(Incident.NOTE_MAX_LENGTH + 20))
        assertEquals(Incident.TITLE_MAX_LENGTH, form.title.length)
        assertEquals(Incident.NOTE_MAX_LENGTH, form.note.length)
    }
}
