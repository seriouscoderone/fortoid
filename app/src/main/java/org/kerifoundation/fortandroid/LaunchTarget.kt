package org.kerifoundation.fortandroid

/** Chooses the first page from the activity the user launched. */
internal object LaunchTarget {
    const val WALLET_URL = "https://appassets.androidplatform.net/app/index.html"
    const val DEMO_URL = "https://appassets.androidplatform.net/demo/index.html"

    /** Fully qualified name of the activity behind the "Fort Demo" icon. */
    const val DEMO_ACTIVITY_CLASS = "org.kerifoundation.fortandroid.DemoActivity"

    fun startUrl(activityClassName: String?): String =
        if (activityClassName == DEMO_ACTIVITY_CLASS) DEMO_URL else WALLET_URL
}
