package org.kerifoundation.fortandroid

/**
 * Entry point behind the "Fort Demo" launcher icon. Same hardened WebView host
 * as [MainActivity]; it only starts on the demo page. Declared with its own
 * task affinity so it never resurfaces an already-running wallet task.
 */
class DemoActivity : MainActivity()
