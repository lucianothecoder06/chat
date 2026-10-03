package com.example.chat.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatTest {

    @Test
    fun idFor_noDependeDelOrden() {
        assertEquals(Chat.idFor("abc", "xyz"), Chat.idFor("xyz", "abc"))
        assertEquals("abc_xyz", Chat.idFor("xyz", "abc"))
    }

    @Test
    fun otherUserId_devuelveElOtroParticipante() {
        val chat = Chat(participants = listOf("abc", "xyz"))
        assertEquals("xyz", chat.otherUserId("abc"))
        assertEquals("abc", chat.otherUserId("xyz"))
    }

    @Test
    fun otherUserId_sinOtroParticipante_devuelveNull() {
        assertNull(Chat(participants = listOf("abc")).otherUserId("abc"))
    }
}
