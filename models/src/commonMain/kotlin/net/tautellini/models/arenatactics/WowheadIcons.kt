package net.tautellini.models.arenatactics

object WowheadIcons {
    private const val BASE = "https://render.worldofwarcraft.com/us/icons"

    /** 36px icon */
    fun medium(iconName: String) = "$BASE/36/$iconName.jpg"

    /** 56px icon */
    fun large(iconName: String) = "$BASE/56/$iconName.jpg"
}
