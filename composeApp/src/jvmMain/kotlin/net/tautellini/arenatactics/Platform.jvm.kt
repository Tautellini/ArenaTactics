package net.tautellini.arenatactics

actual fun refreshWowheadTooltips() {}
actual fun openUrl(url: String) {}
actual fun pushNavigationState(path: String) {}
actual fun registerPopCallback(callback: () -> Unit) {}
actual fun getInitialPath(): String = "/"
actual fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float) {}
actual fun hideWowheadTooltip() {}
actual fun observeWowheadTooltips() {}
