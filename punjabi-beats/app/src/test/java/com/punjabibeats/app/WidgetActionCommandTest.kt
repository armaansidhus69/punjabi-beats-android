package com.punjabibeats.app

import com.punjabibeats.app.playback.MediaPlaybackService
import com.punjabibeats.app.widget.BasePunjabiBeatsWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WidgetActionCommandTest {

    @Test
    fun widgetActions_matchConstants() {
        assertEquals("com.punjabibeats.app.ACTION_WIDGET_PLAY_PAUSE", BasePunjabiBeatsWidgetProvider.ACTION_PLAY_PAUSE)
        assertEquals("com.punjabibeats.app.ACTION_WIDGET_NEXT", BasePunjabiBeatsWidgetProvider.ACTION_NEXT)
        assertEquals("com.punjabibeats.app.ACTION_WIDGET_PREV", BasePunjabiBeatsWidgetProvider.ACTION_PREV)
    }

    @Test
    fun playbackServiceCommand_actionAndExtraConstantsMatch() {
        assertEquals("com.punjabibeats.app.ACTION_PLAYBACK_COMMAND", MediaPlaybackService.ACTION_PLAYBACK_COMMAND)
        assertEquals("command", MediaPlaybackService.EXTRA_COMMAND)
    }
}
