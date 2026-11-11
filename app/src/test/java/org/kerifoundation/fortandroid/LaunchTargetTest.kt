package org.kerifoundation.fortandroid

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchTargetTest {

    @Test
    fun `wallet launcher opens the FortWeb wallet`() {
        assertEquals(
            "https://appassets.androidplatform.net/app/index.html",
            LaunchTarget.startUrl("org.kerifoundation.fortandroid.MainActivity")
        )
    }

    @Test
    fun `demo launcher alias opens the native demo`() {
        assertEquals(
            "https://appassets.androidplatform.net/demo/index.html",
            LaunchTarget.startUrl("org.kerifoundation.fortandroid.DemoLauncher")
        )
    }

    @Test
    fun `unknown or missing component falls back to the wallet`() {
        assertEquals(LaunchTarget.WALLET_URL, LaunchTarget.startUrl(null))
        assertEquals(LaunchTarget.WALLET_URL, LaunchTarget.startUrl("com.evil.DemoLauncher"))
    }
}
