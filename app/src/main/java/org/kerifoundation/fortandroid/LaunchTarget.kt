package org.kerifoundation.fortandroid

/** Chooses the first page from the launcher entry the user tapped. */
internal object LaunchTarget {
    const val WALLET_URL = "https://appassets.androidplatform.net/app/index.html"
    const val DEMO_URL = "https://appassets.androidplatform.net/demo/index.html"

    /** Fully qualified name of the `<activity-alias>` behind the "Fort Demo" icon. */
    const val DEMO_LAUNCHER_COMPONENT = "org.kerifoundation.fortandroid.DemoLauncher"

    fun startUrl(componentClassName: String?): String =
        if (componentClassName == DEMO_LAUNCHER_COMPONENT) DEMO_URL else WALLET_URL
}
