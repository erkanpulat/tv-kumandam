# Mimari

## Akış

```text
Compose UI → ViewModel → RemoteTransmissionCoordinator → RemoteController
                                                     → profile/encoder → ConsumerIrManager
```

IR profilleri UI'dan bağımsızdır. `RemoteProfile`, mantıksal komutları protokole özgü kodlara; `RemoteLayoutSpec` ise o profile uygun ekrana çevirir. `RemoteTransmissionCoordinator`, fiziksel IR emitter üzerinde aynı anda yalnız bir komut ya da dizinin çalışmasını sağlar.

## Katmanlar

- `domain/model`: komutlar, profiller, kanıt seviyesi, kullanıcı makroları ve kalıcı TV modeli.
- `domain/remote`: platformdan bağımsız IR aktarım sözleşmesi, kumanda denetleyicisi ve eşzamanlılık sınırı.
- `data/remote`: kodlayıcılar ve kaynakları belgelenmiş profil aileleri.
- `data/preferences`: atomik DataStore ayar saklama ve savunmacı veri normalizasyonu.
- `presentation`: Compose ekranları ve tek yönlü ViewModel durumları.

Uygulama küçük bir `AppContainer` kullanır; ağır bir DI çerçevesi yoktur. Ağ izni yoktur. IR bulunmayan telefonlarda uygulama açılır, ancak aktarım başlatılmaz.

## Kalıcılık ve güvenlik

`RemotePreferences.update` son kanonik ayarı atomik dönüştürür. Cihaz ekleme, seçme, silme, makro/hızlı erişim düzenleme ve Ayarlar bu sınırı kullanır; birbirlerinin TV listesini veya tercihlerini ezmez. Makrolar güç komutunu kabul etmez; adım, tekrar ve bekleme süreleri sınırlıdır.

Bir profil `DEVICE_VERIFIED`, `SOURCE_VERIFIED` veya `EXPERIMENTAL` kanıt seviyesine sahiptir. Kullanıcı onayı bu seviyeyi yükseltmez; doğrulama kayıtları dokümantasyonda tutulur.
