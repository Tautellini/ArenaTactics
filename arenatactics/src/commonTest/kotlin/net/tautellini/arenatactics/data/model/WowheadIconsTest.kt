package net.tautellini.arenatactics.data.model

import net.tautellini.models.arenatactics.WowheadIcons
import kotlin.test.Test
import kotlin.test.assertEquals

class WowheadIconsTest {
    @Test
    fun mediumUrlIsCorrect() {
        assertEquals(
            "https://render.worldofwarcraft.com/us/icons/36/ability_stealth.jpg",
            WowheadIcons.medium("ability_stealth")
        )
    }

    @Test
    fun largeUrlIsCorrect() {
        assertEquals(
            "https://render.worldofwarcraft.com/us/icons/56/ability_stealth.jpg",
            WowheadIcons.large("ability_stealth")
        )
    }
}
