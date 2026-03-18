package net.tautellini.arenatactics

expect fun refreshWowheadTooltips()
expect fun openUrl(url: String)
expect fun pushNavigationState(path: String)
// Registers a callback invoked by the browser's popstate event (back button).
// On non-web platforms this is a no-op.
expect fun registerPopCallback(callback: () -> Unit)
