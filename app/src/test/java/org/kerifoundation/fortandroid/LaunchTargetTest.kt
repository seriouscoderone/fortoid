package org.kerifoundation.fortandroid

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchTargetTest {

    @Test
    fun `wallet activity opens the FortWeb wallet`() {
        assertEquals(
            "https://appassets.androidplatform.net/app/index.html",
            LaunchTarget.startUrl(MainActivity::class.java.name)
        )
    }

    @Test
    fun `demo activity opens the native demo`() {
        assertEquals(
            "https://appassets.androidplatform.net/demo/index.html",
            LaunchTarget.startUrl(DemoActivity::class.java.name)
        )
    }

    @Test
    fun `unknown or missing class falls back to the wallet`() {
        assertEquals(LaunchTarget.WALLET_URL, LaunchTarget.startUrl(null))
        assertEquals(LaunchTarget.WALLET_URL, LaunchTarget.startUrl("com.evil.DemoActivity"))
    }

    @Test
    fun `demo activity is a MainActivity so it gets the same hardened WebView`() {
        assertEquals(MainActivity::class.java, DemoActivity::class.java.superclass)
    }
}
