# Element Android (Classic) — authentification mTLS par certificat client

Cette branche (`feature/mtls-client-certificate`) ajoute à Element Android la possibilité de présenter un
**certificat client TLS** (mutual TLS) à un homeserver Matrix placé derrière un reverse proxy qui exige
`ssl_verify_client` (HAProxy `verify required`, nginx `ssl_verify_client on`, Traefik `RequireAndVerifyClientCert`…).

## Principe

- Le certificat est installé **dans le magasin d'identifiants Android** (KeyChain), pas dans l'application.
  La clé privée reste dans l'Android Keystore (matériel/TEE si disponible) : l'application n'y a jamais accès
  en clair, elle obtient seulement une référence qui sert à signer pendant le handshake TLS.
- L'utilisateur choisit son certificat via le sélecteur système Android. L'alias choisi est mémorisé
  (`SharedPreferences` dédié `im.vector.app.mtls`), et survit à la déconnexion.
- Un `X509ExtendedKeyManager` (`KeyChainClientKeyManager`) est injecté dans **tous** les `SSLContext` du SDK Matrix :
  client OkHttp de base (découverte `.well-known`, flows de login), clients liés au homeserver (API, sync,
  médias, uploads/downloads, Glide), identity server, WorkManager (sync en arrière-plan).
- Les WebView (login/inscription « fallback », captcha, widgets, pages web internes) répondent aussi
  aux demandes de certificat client (`onReceivedClientCertRequest`).
- L'alias est relu à chaque handshake : pas besoin de reconstruire les clients HTTP pour changer de certificat.

## Utilisation

1. Préparer un fichier PKCS#12 contenant la clé privée + le certificat client (+ chaîne) :
   ```bash
   openssl pkcs12 -export -inkey client.key -in client.crt -certfile ca.crt \
       -name "matrix-client" -out client.p12
   ```
   Sur Android récent, si l'import échoue avec un fichier généré par OpenSSL 3, ajouter `-legacy`.
2. Sur le téléphone : **Paramètres › Sécurité › Chiffrement et identifiants › Installer un certificat ›
   Certificat utilisateur d'appli et de VPN**, puis choisir le `.p12`.
3. Dans Element : écran de choix du serveur (« Modifier » le serveur à la connexion) → bouton
   **Choisir un certificat client** → sélectionner le certificat → saisir l'URL du homeserver → Continuer.
4. Une fois connecté, le certificat peut être changé ou retiré dans
   **Paramètres › Sécurité et vie privée › Certificat client (mTLS)**.

## Fichiers modifiés

SDK (`matrix-sdk-android`) :
- `api/MatrixConfiguration.kt` : nouveau paramètre `clientCertificateKeyManager: X509KeyManager?`.
- `api/Matrix.kt` : enregistre le KeyManager au démarrage.
- `internal/network/ssl/ClientCertificateHolder.kt` (nouveau) : porte le KeyManager pour les `SSLContext`.
- `internal/network/ssl/CertUtil.kt`, `TLSSocketFactory.kt` : `SSLContext.init(keyManagers, …)` au lieu de `null`.
- `internal/di/NetworkModule.kt` : le client OkHttp non authentifié présente aussi le certificat.

Application (`vector`, `vector-app`, `library/ui-strings`) :
- `features/mtls/` (nouveau) : `ClientCertificateStore`, `KeyChainClientKeyManager`,
  `ClientCertificateSelector`, `ClientCertificateWebView`.
- `core/di/SingletonModule.kt` : branche le KeyManager dans `MatrixConfiguration`.
- Onboarding : bouton dans `FtueAuthCombinedServerSelectionFragment` + layout.
- Paramètres : entrée dans `VectorSettingsSecurityPrivacyFragment` + `vector_settings_security_privacy.xml`.
- WebViews : `FtueAuthWebFragment`, `CaptchaWebview`, `VectorWebViewClient`.
- Chaînes EN/FR : `strings_mtls.xml`.

## Limites connues

- La connexion SSO/OIDC s'ouvre dans le navigateur (Custom Tab) : c'est le navigateur qui présente le
  certificat (Chrome sait utiliser le KeyChain Android et le proposera).
- Après un changement de certificat, les connexions déjà ouvertes et les sessions TLS en cache peuvent
  garder l'ancien certificat : redémarrer l'application (forcer l'arrêt) pour un effet immédiat.
- Android < 9 avec clé RSA et TLS 1.3 : certaines versions de l'Android Keystore ne gèrent pas RSA-PSS,
  requis par TLS 1.3 pour l'authentification client. En cas d'échec, utiliser une clé EC (P-256) ou TLS 1.2 côté serveur.
- Les appels Jitsi (SDK Jitsi embarqué) et les notifications push (passerelle Sygnal/FCM ou distributeur
  UnifiedPush) ne passent pas par ce mécanisme.
