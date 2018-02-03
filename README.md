# Cognito Protocol integration for Mycelium Wallet for Android

Building
========

To build everything from source, simply checkout the source and build using gradle
on the build system you need.

 * JDK 1.7

Then you need to use the Android SDK manager to install the following components:

 * `ANDROID_HOME` environment variable pointing to the directory where the SDK is installed
 * Android SDK Tools 22.6.4
 * SDK Platform Tools 19.0.2
 * Android SDK build Tools 19.1.0
 * Android 4.4.2 (API 19) (at least SDK Platform Rev. 3)
 * Android Extras:
    * Android Support Repository rev 5
    * Android Support Library rev 19.1
    * Google Play services for Froyo rev 12
    * Google Play services rev 17
    * Google Repository rev 8


The project layout is designed to be used with a recent version of Android Studio (currently 1.1.0)

#### Build commands

On the console type:

    git clone https://github.com/cognitoprotocol/cognitoprotocol-mycelium-wallet-android-integration.git
    cd cognitoprotocol-mycelium-wallet-android-integration

Linux/Mac type:

    ./gradlew build

Windows type:

    gradlew.bat build

 - Voila, look into `wallet/public/mbw/build/apk` to see the generated apk. 
   There are versions for both prodnet and testnet.

Alternatively you will be install the latest version from the Google Play Store.

If you cannot access the Play Store, you will be able to download a copy of the .apk file directly.

Deterministic builds
====================

The Cognito Protocol Wallet for Android does not support reproducible builds at this point. For this reason, *you should only use the original Mycelium wallet* until reproducible builds are available in the future. Hopefully, they changes will be merged into the main Mycelium branch, making things easier for everyone.

This work on Mycelium reproducible builds is based on WhisperSystems Signal:

* https://whispersystems.org/blog/reproducible-android/
* https://github.com/WhisperSystems/Signal-Android/wiki/Reproducible-Builds


Feedback
========

This application's source is published at https://github.com/cognitoprotocol/cognitoprotocol-mycelium-wallet-android-integration
We need your feedback. If you have a suggestion or a bug to report open an issue at: https://github.com/cognitoprotocol/cognitoprotocol-mycelium-wallet-android-integration/issues


Original Authors
================
 - Jan Møller
 - Andreas Petersson
 - Daniel Weigl
 - Jan Dreske
 - Dmitry Murashchik
 - Constantin Vennekel
 - Leo Wandersleb
 - Daniel Krawisz
 - Jerome Rousselot
 - Elvis Kurtnebiev
 - Sergey Dolgopolov

Credits
=======
Original code by Mycelium.

Thanks to all collaborators who provided Mycelium with code or helped Mycelium with integrations!

Just to name a few:

 - Nicolas Bacca from Ledger
 - Sipa, Marek and others from Trezor
 - Jani and Aleš from Cashila
 - Kalle Rosenbaum, Bip120/121
 - David and Alex from Glidera
 - [Wiz](https://twitter.com/wiz) for helping us with KeepKey
 - Tom Bitton and Asa Zaidman from Simplex
 - (if you think you should be mentioned here, just notify us)

Thanks to Jethro for tirelessly testing the app during beta development.

Thanks to our numerous volunteer translators who provide high-quality translations in many languages.

Thanks to Johannes Zweng for his testing and providing pull requests for fixes.

Thanks to all beta testers to provide early feedback.
