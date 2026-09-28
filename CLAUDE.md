# Element Android — fork avec authentification mTLS

Ce dépôt est un fork d'Element Android (Classic) avec le support du certificat client TLS (mTLS).
Documentation fonctionnelle : `docs/mtls.md`.

## Préparer l'environnement (session cloud)

```bash
bash tools/claude/setup-android-sdk.sh
export ANDROID_HOME=$HOME/android-sdk
```

Le script installe le SDK Android (platform 36, build-tools 36.0.0) et écrit `local.properties`.
JDK 21 obligatoire.

## Compiler

```bash
./gradlew assembleFdroidDebug      # APK dans vector-app/build/outputs/apk/fdroid/debug/
./gradlew assembleGplayDebug       # variante Google Play (FCM)
```

Le premier build est long (téléchargement des dépendances) : lancer Gradle avec un timeout élevé,
ou en arrière-plan, et utiliser `--no-daemon` si la mémoire est juste.

## Vérifications

```bash
./gradlew ktlintCheck detekt
./gradlew :matrix-sdk-android:testDebugUnitTest
bash tools/check/check_code_quality.sh
```

## Code mTLS

- SDK : `matrix-sdk-android/.../internal/network/ssl/` (`ClientCertificateHolder`, `CertUtil`, `TLSSocketFactory`),
  `internal/di/NetworkModule.kt`, `api/MatrixConfiguration.kt` (`clientCertificateKeyManager`).
- App : `vector/src/main/java/im/vector/app/features/mtls/`, branché dans `vector-app/.../core/di/SingletonModule.kt`.
- UI : `FtueAuthCombinedServerSelectionFragment` (bouton de choix), `VectorSettingsSecurityPrivacyFragment` (paramètres).
- Ne pas stocker de clé privée dans l'app : tout passe par Android KeyChain.
