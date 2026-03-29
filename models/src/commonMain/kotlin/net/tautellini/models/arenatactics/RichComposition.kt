package net.tautellini.models.arenatactics

data class RichComposition(
    val composition: Composition,
    val specs: List<WowSpec>,
    val classes: List<WowClass>
)
