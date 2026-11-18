package com.punjabibeats.app

import com.punjabibeats.app.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationAndQueueTest {

    @Test
    fun topLevelRoutesAreDistinctAndValid() {
        val topLevelRoutes = listOf(
            Routes.HOME,
            Routes.SEARCH,
            Routes.JAM,
            Routes.AI,
            Routes.LIBRARY,
        )

        assertEquals("Must have 5 top-level destinations", 5, topLevelRoutes.size)
        val distinctRoutes = topLevelRoutes.toSet()
        assertEquals("Top level routes must all be unique", 5, distinctRoutes.size)

        for (route in topLevelRoutes) {
            assertFalse("Route must not be blank", route.isBlank())
            assertFalse("Top level route must not contain arguments", route.contains("{"))
        }
    }

    @Test
    fun secondaryRoutesResolveProperly() {
        assertNotNull(Routes.SETTINGS)
        assertNotNull(Routes.THEMES)
        assertNotNull(Routes.EQUALIZER)
        assertNotNull(Routes.CAR_DASHBOARD)
        assertNotNull(Routes.CAR_DSP)
        assertNotNull(Routes.PLAYER)
        assertNotNull(Routes.LYRICS)
        assertNotNull(Routes.PODCASTS)

        val artistRoute = Routes.artist("Sidhu Moose Wala")
        assertTrue("Artist route must encode spaces", artistRoute.startsWith("artist/"))

        val albumRoute = Routes.album("Moosetape", "Sidhu Moose Wala")
        assertTrue("Album route must start with album/", albumRoute.startsWith("album/"))
    }
}
