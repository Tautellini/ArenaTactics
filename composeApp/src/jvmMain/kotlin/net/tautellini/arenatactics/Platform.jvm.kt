package net.tautellini.arenatactics

actual fun openUrl(url: String) {}
actual fun pushNavigationState(path: String) {}
actual fun registerPopCallback(callback: (isBack: Boolean) -> Unit) {}
actual fun getInitialPath(): String = "/"
actual fun getCurrentPath(): String = "/"
