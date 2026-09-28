/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.mtls

import android.content.Context
import android.webkit.ClientCertRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlin.concurrent.thread

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ClientCertificateEntryPoint {
    fun clientCertificateKeyManager(): KeyChainClientKeyManager
}

/**
 * Answer a WebView client certificate request with the certificate selected for mTLS, if any.
 * To be called from [android.webkit.WebViewClient.onReceivedClientCertRequest].
 */
fun ClientCertRequest.proceedWithSelectedCertificate(context: Context) {
    val keyManager = EntryPointAccessors
            .fromApplication(context.applicationContext, ClientCertificateEntryPoint::class.java)
            .clientCertificateKeyManager()
    // KeyChain must not be accessed from the main thread
    thread(name = "mtls-webview") {
        val credentials = keyManager.currentCredentials()
        if (credentials != null) {
            proceed(credentials.privateKey, credentials.certificateChain)
        } else {
            ignore()
        }
    }
}
