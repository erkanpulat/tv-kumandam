# Cihaz testi

## Kurulum

1. Telefonun USB hata ayıklamasını açın ve bilgisayara bağlayın.
2. `adb devices -l` çıktısında cihazın `device` olarak göründüğünü doğrulayın.
3. Debug APK oluşturun: `.\scripts\gradle.ps1 assembleDebug`.
4. APK'yı `adb install -r app/build/outputs/apk/debug/app-debug.apk` ile yükleyin. `-r` yalnızca telefondaki mevcut uygulama aynı anahtarla imzalanmışsa verileri koruyarak günceller. Farklı anahtarla imzalanmış yayın sürümü için ayrı bir test cihazı veya kullanıcı profili kullanın.

## IR kontrol listesi

Her komutta telefonun IR penceresini TV alıcısına doğrultun; telefon kılıfı sinyali kesiyorsa çıkarın.

1. Ayarlar ekranında IR durumunu kontrol edin.
2. Profil bulucuda TV kapalıyken Güç komutunu üç kez, TV açıkken Ses + komutunu üç kez test edin.
3. Varsa Kaynak komutunu üç kez test edin; platform aktarım başarısını TV tepkisiyle karıştırmayın.
4. Bir test makrosu oluşturun, ana kumandaya sabitleyin ve adımların doğru sırada/tekrar sayısında gittiğini doğrulayın. İşlem sürerken ikinci kez basılamadığını ve “Durdur”un kalan adımları iptal ettiğini kontrol edin.
5. D-pad, OK, Menü, Geri/Çıkış ve ses/kanal tuşlarını ayrı ayrı test edin.
6. Cihazlar ekranından ikinci bir TV ekleyin, seçin, uygulamayı kapatıp açın ve seçimin korunduğunu doğrulayın.
7. Ayarlar'dan tema, titreşim ve tuş yerleşimini değiştirin; uygulama yeniden açıldığında korunduğunu kontrol edin.

## Sonuç bildirimi

Bir profil çalışmıyorsa TV ve kumanda üzerindeki tam model numarasını, telefon modelini/Android sürümünü, test edilen komutu, telefon-TV mesafesini ve gözlenen sonucu paylaşın. Tahmin edilen kodlar `DEVICE_VERIFIED` olarak işaretlenmez.
