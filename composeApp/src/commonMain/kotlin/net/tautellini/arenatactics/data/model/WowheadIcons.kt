package net.tautellini.arenatactics.data.model

object WowheadIcons {
    private const val BASE = "https://wow.zamimg.com/images/wow/icons"
    fun medium(iconName: String) = "$BASE/medium/$iconName.jpg"
    fun large(iconName: String) = "$BASE/large/$iconName.jpg"
}
