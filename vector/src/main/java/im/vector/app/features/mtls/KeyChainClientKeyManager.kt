/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.mtls

import android.content.Context
import android.os.Looper
import android.security.KeyChain
import android.security.KeyChainException
import timber.log.Timber
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager

/**
 * X509 KeyManager backed by the Android KeyChain (system credential storage).
 *
 * The private key never leaves the Android KeyStore: we only get a handle on it, and the signature
 * during the TLS handshake is done by the system (hardware-backed if available).
 *
 * The alias is read at each handshake, so choosing another certificate is taken into account for new connections.
 */
@Singleton
class KeyChainClientKeyManager @Inject constructor(
        private val context: Context,
        private val store: ClientCertificateStore,
) : X509ExtendedKeyManager() {

    class ClientCredentials(
            val alias: String,
            val privateKey: PrivateKey,
            val certificateChain: Array<X509Certificate>,
    )

    @Volatile
    private var cache: ClientCredentials? = null

    /**
     * Must be called when the selected alias changes.
     */
    fun invalidate() {
        cache = null
    }

    /**
     * Get the credentials of the currently selected certificate.
     * Must NOT be called from the main thread.
     */
    fun currentCredentials(): ClientCredentials? {
        val alias = store.alias ?: return null
        return load(alias)
    }

    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String? {
        return currentCredentials()?.alias
    }

    override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?): String? {
        return currentCredentials()?.alias
    }

    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? {
        return currentCredentials()?.let { arrayOf(it.alias) }
    }

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
        return alias?.let { load(it) }?.certificateChain
    }

    override fun getPrivateKey(alias: String?): PrivateKey? {
        return alias?.let { load(it) }?.privateKey
    }

    // This is a client only KeyManager
    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null

    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = null

    override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String? = null

    @Synchronized
    private fun load(alias: String): ClientCredentials? {
        cache?.takeIf { it.alias == alias }?.let { return it }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Timber.w("KeyChainClientKeyManager: cannot load the client certificate from the main thread")
            return null
        }

        return try {
            val privateKey = KeyChain.getPrivateKey(context, alias)
            val chain = KeyChain.getCertificateChain(context, alias)
            if (privateKey == null || chain == null || chain.isEmpty()) {
                Timber.w("KeyChainClientKeyManager: client certificate '$alias' not available (removed or access revoked?)")
                null
            } else {
                ClientCredentials(alias, privateKey, chain).also { cache = it }
            }
        } catch (e: KeyChainException) {
            Timber.e(e, "KeyChainClientKeyManager: unable to load client certificate '$alias'")
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: Exception) {
            Timber.e(e, "KeyChainClientKeyManager: unexpected error loading client certificate '$alias'")
            null
        }
    }
}
