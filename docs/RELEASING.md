# Release hazırlama

Bu belge, GitHub Releases için imzalı APK hazırlama sürecini açıklar. CI iş akışı test, lint ve derleme yapar; uzun ömürlü imzalama anahtarına erişmez ve `app-release-unsigned.apk` üretir.

## 1. Sürümü belirleyin

`app/build.gradle.kts` içindeki `versionCode` değerini artırın ve `versionName` değerini yayımlanacak sürümle eşleştirin. Örneğin `1.0.0` sürümü için Git etiketi `1.0.0` olmalıdır.

## 2. Temiz doğrulama yapın

Windows PowerShell:

```powershell
.\scripts\gradle.ps1 clean testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest --no-daemon
```

macOS/Linux:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest --no-daemon
```

İmzasız girdi `app/build/outputs/apk/release/app-release-unsigned.apk` konumunda oluşur.

## 3. Yayın anahtarı oluşturun

Bu işlem her uygulama için yalnızca bir kez yapılır. Anahtarı deponun dışında, yedeklenen ve erişimi sınırlandırılmış bir klasörde tutun:

```powershell
keytool -genkeypair -v `
  -keystore C:\secure\tv-kumandam-release.jks `
  -alias tv-kumandam `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Parolaları komut satırı parametresi, `gradle.properties`, GitHub issue veya depo dosyası olarak kaydetmeyin. `keytool` ve `apksigner` parola istediğinde etkileşimli istemi kullanın. Anahtar kaybolursa mevcut kullanıcıların aynı uygulamanın yeni sürümünü yüklemesi mümkün olmayabilir.

## 4. APK'yı hizalayın ve imzalayın

Örnek PowerShell komutları Android SDK Build Tools 36.0.0 içindir:

```powershell
$buildTools = Join-Path $env:ANDROID_HOME "build-tools\36.0.0"
$unsignedApk = "app\build\outputs\apk\release\app-release-unsigned.apk"
$alignedApk = "app\build\outputs\apk\release\app-release-aligned.apk"
$signedApk = "app\build\outputs\apk\release\tv-kumandam-1.0.0.apk"

& (Join-Path $buildTools "zipalign.exe") -p -f 4 $unsignedApk $alignedApk
& (Join-Path $buildTools "apksigner.bat") sign `
  --ks C:\secure\tv-kumandam-release.jks `
  --ks-key-alias tv-kumandam `
  --out $signedApk `
  $alignedApk
```

macOS/Linux üzerinde aynı araçların `.exe`/`.bat` uzantısız karşılıklarını kullanın.

## 5. İmzayı ve özeti doğrulayın

```powershell
& (Join-Path $buildTools "apksigner.bat") verify --verbose --print-certs $signedApk
Get-FileHash $signedApk -Algorithm SHA256
```

`apksigner verify` başarılı olmalı ve sertifika özeti daha önce yayımlanan sürümle aynı olmalıdır. SHA-256 çıktısını sürüm notlarına veya APK yanında yayımlanan bir checksum dosyasına ekleyin. APK'yı mümkünse IR blaster'lı gerçek telefonda kurup POWER ve SOURCE komutlarıyla son kez deneyin.

## 6. GitHub Release yayımlayın

1. Doğrulanmış commit üzerinde imzalı `X.Y.Z` etiketi oluşturun.
2. Aynı adla GitHub Release açın.
3. Yalnızca imzalı, sürüm adını taşıyan APK'yı ve SHA-256 checksum'u yükleyin; `app-release-unsigned.apk` dosyasını yayımlamayın.
4. Sürüm notlarında doğrulanan TV/telefon modellerini, bilinen uyumsuzlukları ve fiziksel IR test durumunu açıkça yazın.

Yayın anahtarını veya parolalarını GitHub Release varlıklarına, Actions artifact'larına ya da kaynak denetimine hiçbir zaman eklemeyin.
