package com.punjabibeats.app

import com.punjabibeats.app.domain.model.AiMessage
import com.punjabibeats.app.domain.model.AiRole
import com.punjabibeats.app.ui.screens.ai.AiStudioUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiStudioScreenTest {

    @Test
    fun defaultUiStateHasEmptyInputsAndMessages() {
        val state = AiStudioUiState()
        assertTrue(state.prompt.isEmpty())
        assertTrue(state.chatInput.isEmpty())
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isThinking)
        assertFalse(state.savingPlaylist)
    }

    @Test
    fun chatInputCanBeUpdatedAndFormatted() {
        var state = AiStudioUiState()
        val testInput = "Best Sidhu Moose Wala folk songs"
        state = state.copy(chatInput = testInput)
        assertEquals(testInput, state.chatInput)

        val userMessage = AiMessage(
            role = AiRole.USER,
            content = testInput,
            timestampMs = System.currentTimeMillis(),
        )
        val assistantMessage = AiMessage(
            role = AiRole.ASSISTANT,
            content = "Here are recommendations: 295, So High, The Last Ride.",
            timestampMs = System.currentTimeMillis() + 100,
        )

        state = state.copy(
            chatInput = "",
            messages = listOf(userMessage, assistantMessage),
        )

        assertEquals(2, state.messages.size)
        assertEquals(AiRole.USER, state.messages[0].role)
        assertEquals(AiRole.ASSISTANT, state.messages[1].role)
        assertTrue(state.chatInput.isEmpty())
    }
}
