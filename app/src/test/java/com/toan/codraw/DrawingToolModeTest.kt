package com.toan.codraw

import com.toan.codraw.presentation.model.DrawingToolMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingToolModeTest {

    @Test
    fun `eraser is flagged correctly`() {
        assertTrue(DrawingToolMode.ERASER.isEraser)
        assertFalse(DrawingToolMode.PENCIL.isEraser)
    }

    @Test
    fun `preset opacity values stay consistent`() {
        assertEquals(0.35f, DrawingToolMode.PENCIL.defaultOpacity)
        assertEquals(1f, DrawingToolMode.PEN.defaultOpacity)
        assertEquals(0.65f, DrawingToolMode.MARKER.defaultOpacity)
    }
}

