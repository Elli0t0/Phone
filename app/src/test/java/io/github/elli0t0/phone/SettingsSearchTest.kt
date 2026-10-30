package io.github.elli0t0.phone

import io.github.elli0t0.phone.feature.settings.ResolvedSettingEntry
import io.github.elli0t0.phone.feature.settings.SettingEntry
import io.github.elli0t0.phone.feature.settings.SettingsDialogState
import io.github.elli0t0.phone.feature.settings.SettingsPage
import io.github.elli0t0.phone.feature.settings.allSettingEntries
import io.github.elli0t0.phone.feature.settings.normalizeSearchText
import io.github.elli0t0.phone.feature.settings.openSettingsPage
import io.github.elli0t0.phone.feature.settings.searchSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {

    private fun resolved(
        id: String,
        title: String,
        keywords: String = "",
        section: String = ""
    ): ResolvedSettingEntry {
        val entry = SettingEntry(
            id = id,
            titleRes = 0,
            sectionRes = 0,
            page = SettingsPage.MAIN
        )
        return ResolvedSettingEntry(entry, title, keywords, section)
    }

    @Test
    fun normalize_unifiesArabicYehAndKaf() {
        assertEquals("ی", normalizeSearchText("ي"))
        assertEquals("ک", normalizeSearchText("ك"))
    }

    @Test
    fun normalize_stripsTashkeelAndJoiners() {
        assertEquals("سلام", normalizeSearchText("سَلَام"))
        assertEquals("نیمفاصله", normalizeSearchText("نیم‌فاصله"))
    }

    @Test
    fun normalize_mapsArabicAndPersianDigits() {
        assertEquals("0123", normalizeSearchText("٠١٢٣"))
        assertEquals("0123", normalizeSearchText("۰۱۲۳"))
    }

    @Test
    fun search_requiresAllTokens() {
        val entries = listOf(
            resolved("a", "Glass bottom bar", section = "Liquid glass"),
            resolved("b", "Glass cards", section = "Liquid glass")
        )
        val results = searchSettings("glass bottom", entries)
        assertEquals(listOf("a"), results.map { it.entry.id })
    }

    @Test
    fun search_titlePrefixMatchesFirst() {
        val entries = listOf(
            resolved("a", "Reduce transparency", keywords = "glass"),
            resolved("b", "Glass cards", section = "Liquid glass")
        )
        val results = searchSettings("glass", entries)
        assertEquals(listOf("b", "a"), results.map { it.entry.id })
    }

    @Test
    fun search_matchesKeywordsAndSection() {
        val entries = listOf(
            resolved("a", "Pair", keywords = "wireless debugging wifi", section = "Call recording")
        )
        assertEquals(
            listOf("a"),
            searchSettings("wifi", entries).map { it.entry.id }
        )
        assertEquals(
            listOf("a"),
            searchSettings("recording pair", entries).map { it.entry.id }
        )
    }

    @Test
    fun search_emptyQueryMatchesNothing() {
        val entries = listOf(resolved("a", "Theme"))
        assertTrue(searchSettings("", entries).isEmpty())
        assertTrue(searchSettings("   ", entries).isEmpty())
    }

    @Test
    fun registry_idsAreUnique() {
        val ids = allSettingEntries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun registry_anchorsAreUniquePerPage() {
        val anchored = allSettingEntries.mapNotNull { entry ->
            entry.anchor?.let { entry.page to it }
        }
        assertEquals(anchored.size, anchored.toSet().size)
    }

    @Test
    fun registry_everyPageOpensAFlag() {
        for (page in SettingsPage.entries) {
            if (page == SettingsPage.MAIN) continue
            val dialogs = SettingsDialogState()
            openSettingsPage(dialogs, page)
            val opened = dialogs.showAppearancePage || dialogs.showGlassPage ||
                dialogs.showLayoutNavPage || dialogs.showCallsPage ||
                dialogs.showContactsPage || dialogs.showPrivacyPage ||
                dialogs.showAboutPage || dialogs.showAppStylePage ||
                dialogs.showKeypadStylePage ||
                dialogs.showGlassStudioPage || dialogs.showCallBackgroundPage ||
                dialogs.showBlockedNumbers || dialogs.showRecordingPage ||
                dialogs.showAnswerStylePage
            assertTrue("no flag opens for $page", opened)
        }
    }

    @Test
    fun registry_everyEntryHasATitleAndSection() {
        for (entry in allSettingEntries) {
            assertTrue("titleRes is 0 for ${entry.id}", entry.titleRes != 0)
            assertTrue("sectionRes is 0 for ${entry.id}", entry.sectionRes != 0)
        }
    }
}
