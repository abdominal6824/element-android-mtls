/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.mtls

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the alias of the Android KeyChain client certificate used for mutual TLS (mTLS).
 * Stored in a dedicated SharedPreferences file, so it survives sign out (the certificate is needed to sign in again).
 */
@Singleton
class ClientCertificateStore @Inject constructor(
        context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var alias: String?
        get() = prefs.getString(KEY_ALIAS, null)
        set(value) {
            prefs.edit {
                if (value == null) remove(KEY_ALIAS) else putString(KEY_ALIAS, value)
            }
        }

    companion object {
        private const val PREFS_NAME = "im.vector.app.mtls"
        private const val KEY_ALIAS = "client_certificate_alias"
    }
}
