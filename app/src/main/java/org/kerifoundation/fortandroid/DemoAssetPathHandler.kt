package org.kerifoundation.fortandroid

import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader

/**
 * Serves the bundled native-capabilities demo from `assets/demo/` at
 * `https://appassets.androidplatform.net/demo/`. Paths that could escape the
 * demo folder are refused.
 */
internal class DemoAssetPathHandler(
    private val delegate: WebViewAssetLoader.PathHandler
) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse? {
        if (path.startsWith("/") || path.contains('\\') || path.split('/').any { it == ".." }) {
            return null
        }

        val relative = path.ifEmpty { "index.html" }
        return delegate.handle("demo/$relative")
    }
}
