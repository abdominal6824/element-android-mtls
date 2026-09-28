/*
 * Copyright 2026 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.network.ssl

import javax.net.ssl.KeyManager
import javax.net.ssl.X509KeyManager

/**
 * Holds the KeyManager used to present a TLS client certificate (mutual TLS).
 * It is set once from [org.matrix.android.sdk.api.MatrixConfiguration.clientCertificateKeyManager]
 * and used by every SSLContext created by the SDK.
 */
internal object ClientCertificateHolder {

    @Volatile
    var keyManager: X509KeyManager? = null

    /**
     * @return the KeyManagers to give to [javax.net.ssl.SSLContext.init], or null if mTLS is not configured.
     */
    fun keyManagers(): Array<KeyManager>? = keyManager?.let { arrayOf(it) }
}
