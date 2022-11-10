package crackers.kobots.utilities

/**
 * Ignore exceptions on non-returning functions.
 */
fun ignoreErrors(block: () -> Unit) =
    try {
        block()
    } catch (_: Throwable) {
        // ignored
    }
