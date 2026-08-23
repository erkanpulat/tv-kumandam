# Katkı rehberi

Katkılar memnuniyetle karşılanır. Lütfen değişiklikleri küçük, test edilebilir ve tek amaca odaklı tutun.

## Geliştirme akışı

1. Repoyu fork edin ve açıklayıcı bir dal oluşturun.
2. Davranış değişikliği için önce kırılan bir test ekleyin.
3. `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` çalıştırın.
4. Pull request açıklamasında değişikliğin amacını, çalıştırılan testleri ve varsa fiziksel cihaz sonucunu belirtin.

## IR profili katkıları

Yeni veya değiştirilmiş IR kodu aşağıdakileri içermelidir:

- TV'nin tam model numarası
- Orijinal/uyumlu kumandanın model numarası
- Protokol, cihaz adresi, taşıyıcı frekansı ve komut kodları
- Bilginin kaynağına bağlantı
- IR blaster'lı telefon modeli ve Android sürümüyle gerçek cihaz test sonucu

Tahmine dayalı profilleri doğrulanmış olarak işaretlemeyin. Belirsiz yedek kodlarla mevcut profili genişletmek yerine yeni bir profil ekleyin.

## Kod stili

- Kullanıcı metinlerini açık ve tutarlı Türkçe ile, IR verisini `data/remote` katmanında tutun.
- Magic number yerine adlandırılmış sabitler kullanın.
- Fonksiyonları küçük, sınıfları tek sorumluluklu tutun.
- Yeni bağımlılık eklemeden önce standart Android/Compose çözümünün yeterli olup olmadığını değerlendirin.
- Ağ, Bluetooth, konum veya izleme izni ekleyen değişiklikleri açıkça gerekçelendirin.
