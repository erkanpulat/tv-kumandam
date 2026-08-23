# Android Studio olmadan derleme

Bu proje yalnızca komut satırı araçlarıyla derlenebilir; Android Studio gerekli değildir.

## Gereksinimler

- Git
- JDK 17
- Android SDK Command-line Tools
- Android SDK Platform 37.0
- Android SDK Build Tools 36.0.0

Android'in resmi Command-line Tools paketini indirin, lisansları kabul edin ve gerekli paketleri yükleyin:

```bash
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
```

`android sdk install` komutu Android SDK Command-line Tools'a ait değildir; paket kurulumu için resmi `sdkmanager` aracını kullanın.

`ANDROID_HOME` ortam değişkenini SDK kök dizinine, `JAVA_HOME` değişkenini JDK 17 dizinine ayarlayın. Gerekirse repo kökünde, Git'e alınmayan bir `local.properties` oluşturun:

```properties
sdk.dir=C\:\\Android\\Sdk
```

## Derleme

macOS/Linux:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows PowerShell:

```powershell
.\scripts\gradle.ps1 testDebugUnitTest lintDebug assembleDebug
```

PowerShell yardımcı betiği Türkçe karakter içeren Windows klasörlerinde Gradle worker classpath sorununu önlemek için doğrulanmış bir ASCII junction kullanır. Proje yolu zaten ASCII ise doğrudan Gradle wrapper'ı çalıştırır.

## APK yükleme

Telefonu USB debugging ile bağladıktan sonra:

```text
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

IR işlevi Android emülatöründe doğrulanamaz. Gerçek test için `ConsumerIrManager.hasIrEmitter()` sonucu `true` olan fiziksel telefon gerekir.
