package net.tautellini.arenatactics

expect fun openUrl(url: String)

/**
 * Pushes a new browser history entry for [path].
 * Stores a monotonically increasing position counter in the history state so
 * that [registerPopCallback] can detect back vs. forward navigation.
 * No-op on non-web platforms.
 */
expect fun pushNavigationState(path: String)

/**
 * Registers a callback invoked by the browser's popstate event (back/forward).
 * [isBack] is true when the user navigated backward, false for forward.
 * On non-web platforms this is a no-op and the callback is never invoked.
 */
expect fun registerPopCallback(callback: (isBack: Boolean) -> Unit)

/** Returns the current URL pathname for deep-link support. Returns "/" on JVM. */
expect fun getInitialPath(): String

/** Returns the current URL pathname (called from popstate callback). Returns "/" on JVM. */
expect fun getCurrentPath(): String
