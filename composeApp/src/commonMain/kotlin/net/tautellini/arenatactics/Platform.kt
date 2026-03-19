package net.tautellini.arenatactics

expect fun refreshWowheadTooltips()
expect fun openUrl(url: String)
expect fun pushNavigationState(path: String)
// Registers a callback invoked by the browser's popstate event (back button).
// On non-web platforms this is a no-op.
expect fun registerPopCallback(callback: () -> Unit)
// Returns the current URL pathname for deep-link support. Returns "/" on JVM.
expect fun getInitialPath(): String

// Shows a Wowhead tooltip for the given item ID at the cursor position.
// On web: creates/updates a 1×1 invisible DOM anchor at the cursor, triggers WH tooltip.
// On JVM: no-op.
expect fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float)
expect fun hideWowheadTooltip()
