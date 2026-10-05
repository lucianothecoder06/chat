package com.example.chat.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageUtilsTest {

    @Test
    fun imagenChica_noSeReduce() {
        assertEquals(1, ImageUtils.sampleSizeFor(400, 300, 512))
    }

    @Test
    fun fotoGrande_seReducePeroNoPorDebajoDelMaximo() {
        // 4000 / 4 = 1000 >= 512, 4000 / 8 = 500 < 512 → 4
        assertEquals(4, ImageUtils.sampleSizeFor(4000, 3000, 512))
    }

    @Test
    fun usaElLadoMasLargo() {
        assertEquals(ImageUtils.sampleSizeFor(3000, 4000, 512), ImageUtils.sampleSizeFor(4000, 3000, 512))
    }
}
