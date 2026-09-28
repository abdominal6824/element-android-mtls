/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.mtls

import android.app.Activity
import android.net.Uri
import android.security.KeyChain
import org.matrix.android.sdk.api.extensions.tryOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Let the user choose a client certificate (mTLS) among the ones installed in the Android credential storage.
 * To install a certificate: Android Settings > Security > Encryption & credentials > Install a certificate
 * > "VPN & app user certificate", then select the .p12 / .pfx file.
 */
@Singleton
class ClientCertificateSelector @Inject constructor(
        private val store: ClientCertificateStore,
        private val keyManager: KeyChainClientKeyManager,
) {

    val currentAlias: String?
        get() = store.alias

    /**
     * Open the system certificate picker.
     *
     * @param activity the current activity
     * @param serverUrl optional url of the server, displayed by the system picker and used to preselect a certificate
     * @param onSelected called on the main thread with the selected alias, or null if the user cancelled
     */
    fun select(activity: Activity, serverUrl: String?, onSelected: (String?) -> Unit) {
        val uri = serverUrl?.let { tryOrNull { Uri.parse(it) } }
        val host = uri?.host
        val port = uri?.port?.takeIf { it > 0 } ?: if (host != null) 443 else -1
        KeyChain.choosePrivateKeyAlias(
                activity,
                { alias ->
                    activity.runOnUiThread {
                        if (alias != null) {
                            store.alias = alias
                            keyManager.invalidate()
                        }
                        onSelected(alias)
                    }
                },
                null, // any key type
                null, // any issuer
                host,
                port,
                store.alias,
        )
    }

    fun clear() {
        store.alias = null
        keyManager.invalidate()
    }
}
