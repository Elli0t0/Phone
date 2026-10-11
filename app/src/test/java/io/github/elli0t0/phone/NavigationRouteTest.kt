package io.github.elli0t0.phone

import io.github.elli0t0.phone.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRouteTest {

    @Test
    fun bottomNavItems_containsExactlyThreeDestinations() {
        val items = Screen.bottomNavItems
        assertEquals(3, items.size)
        assertEquals(listOf(Screen.Recents, Screen.Contacts, Screen.Dialpad), items)
    }

    @Test
    fun screens_haveUniqueNonBlankRoutes() {
        val routes = Screen.bottomNavItems.map { it.route }
        assertEquals(3, routes.distinct().size)
        routes.forEach { route ->
            assertTrue("Route should not be blank", route.isNotBlank())
        }
    }

    @Test
    fun screens_haveValidResourceBindings() {
        Screen.bottomNavItems.forEach { screen ->
            assertTrue("Screen title resource should be non-zero", screen.titleRes != 0)
            assertTrue("Screen icon resource should be non-zero", screen.iconRes != 0)
            assertTrue("Screen contentDescription resource should be non-zero", screen.contentDescriptionRes != 0)
        }
    }
}
