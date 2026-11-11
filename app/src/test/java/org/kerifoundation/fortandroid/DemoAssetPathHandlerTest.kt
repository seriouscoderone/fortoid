package org.kerifoundation.fortandroid

import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAssetPathHandlerTest {

    private class RecordingPathHandler : WebViewAssetLoader.PathHandler {
        val requestedPaths = mutableListOf<String>()

        override fun handle(path: String): WebResourceResponse? {
            requestedPaths += path
            return null
        }
    }

    @Test
    fun `demo files map under the demo asset folder`() {
        val delegate = RecordingPathHandler()
        val handler = DemoAssetPathHandler(delegate)

        handler.handle("index.html")
        handler.handle("js/fort-native.js")

        assertEquals(listOf("demo/index.html", "demo/js/fort-native.js"), delegate.requestedPaths)
    }

    @Test
    fun `empty path serves the demo index`() {
        val delegate = RecordingPathHandler()
        DemoAssetPathHandler(delegate).handle("")

        assertEquals(listOf("demo/index.html"), delegate.requestedPaths)
    }

    @Test
    fun `traversal outside the demo folder is refused`() {
        val delegate = RecordingPathHandler()
        val handler = DemoAssetPathHandler(delegate)

        for (bad in listOf("../android/runtime-origin-contract.json", "a/../../x", "/etc/passwd", "a\\b")) {
            handler.handle(bad)
        }

        assertTrue("delegate must never be called, got ${delegate.requestedPaths}", delegate.requestedPaths.isEmpty())
    }
}
