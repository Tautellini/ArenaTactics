package net.tautellini.arenatactics.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WowheadIconsTest {
    @Test
    fun mediumUrlIsCorrect() {
        assertEquals(
            "https://wow.zamimg.com/images/wow/icons/medium/ability_stealth.jpg",
            WowheadIcons.medium("ability_stealth")
        )
    }

    @Test
    fun largeUrlIsCorrect() {
        assertEquals(
            "https://wow.zamimg.com/images/wow/icons/large/ability_stealth.jpg",
            WowheadIcons.large("ability_stealth")
        )
    }
}
