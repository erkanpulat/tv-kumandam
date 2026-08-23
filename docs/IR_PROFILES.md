# IR profilleri

`DEVICE_VERIFIED`, bu projede gerçek televizyonla test edilen profilleri belirtir. `SOURCE_VERIFIED`, kod tablosunun ve kaynağın incelendiğini ancak fiziksel TV testinin henüz yapılmadığını gösterir. Android'in sinyali başarıyla göndermesi, televizyonun komutu kabul ettiğini kanıtlamaz.

| Aile | Kanıt | Giriş | Kaynak |
|---|---|---|---|
| Arçelik 82-507 B 3HD / 32HDR RC-YC1 | Cihazda doğrulandı | Kaynak menüsü | [Arçelik](https://download.arcelik.com.tr/Download.UsageManuals/23594_3_GUF.pdf), [MIT IRDB](https://github.com/flipperdevices/IRDB/blob/f7b15366521cc81ba11b341538f0097bddbb748b/database/categories/TVs/Grundig/Grundig_1786_xm_1_3018/Grundig_1786_xm_1_3018.ir) |
| Beko F 82-507 B 3HD RC-YC1 | Kaynağı doğrulandı | Kaynak menüsü | [Beko](https://download.beko.com/Download.UsageManualsBeko/23593_GJC.pdf) |
| Grundig 1786 XM / 1 3018 RC-YC1 | Kaynağı doğrulandı | Kaynak menüsü | [MIT IRDB](https://github.com/flipperdevices/IRDB/blob/f7b15366521cc81ba11b341538f0097bddbb748b/database/categories/TVs/Grundig/Grundig_1786_xm_1_3018/Grundig_1786_xm_1_3018.ir) |
| LG 50PC1DR / 42LB1DR | Kaynağı doğrulandı | Doğrudan HDMI 1/2 | [LG](https://www.lg.com/us/support/products/documents/38289U0512Een_RevD.pdf) |
| LG 32LC50CB | Kaynağı doğrulandı | Doğrudan HDMI 1/2 | [LG](https://www.lg.com/us/support/products/documents/32LC50CB_Manual.pdf) |
| LG LC2D / PC3D / PC1D | Kaynağı doğrulandı | Doğrudan HDMI 1/2 | [LG](https://www.lg.com/us/support/products/documents/32lc2d_32lc2du_37lc2d_42lc2d_42pc3d_42pc3dv_50pc3d_60pc1d.pdf) |
| LG70 | Kaynağı doğrulandı | Doğrudan HDMI 1–4 | [LG](https://www.lg.com/us/tv-audio-video/pdf/LG70_manual.pdf) |
| Samsung AA59-00484A | Kaynağı doğrulandı | Yalnızca kaynak | [CC0](https://github.com/logickworkshop/Flipper-IRDB/blob/d126fb1b6f1e114c52b4a8c19839ea65e3a9c24d/TVs/Samsung/Samsung_AA59-00484A.ir) |
| Toshiba CT-8560 | Kaynağı doğrulandı | Yalnızca kaynak | [CC0](https://github.com/logickworkshop/Flipper-IRDB/blob/d126fb1b6f1e114c52b4a8c19839ea65e3a9c24d/TVs/Toshiba/Toshiba%20CT%208560.ir) |
| Hitachi CLE-1031 | Kaynağı doğrulandı | Yalnızca kaynak | [CC0](https://github.com/logickworkshop/Flipper-IRDB/blob/d126fb1b6f1e114c52b4a8c19839ea65e3a9c24d/TVs/Hitachi/Hitachi_CLE-1031.ir) |
| JVC LT-49HW97U / RM-C3311 | Kaynağı doğrulandı | Yalnızca kaynak | [CC0](https://github.com/logickworkshop/Flipper-IRDB/blob/d126fb1b6f1e114c52b4a8c19839ea65e3a9c24d/TVs/JVC/JVC_LT-49HW97U.ir) |

## RC-YC1

RC5 adresi `0x00`'dır. Güç komutu `0x0C`, Kaynak `0x38`, Menü `0x19`, gerçek OK/Enter komutu ise `0x35` kodunu kullanır. Doğrulanan Arçelik TV için kaynağı belgelenmiş bağımsız bir HDMI komutu bulunamadı. Kullanıcılar kendi TV'lerine uygun bir kaynak menüsü makrosu oluşturabilir; uygulama tek bir sabit menü sırasının her modelde güvenli çalışacağını iddia etmez.

Proje sahibi güç, kaynak, yön ve OK komutlarını 23 Ağustos 2026'da M2101K7BG telefon ve eski bir Arçelik LCD TV ile test etti. Bu test, Grundig veya Beko televizyonlar için fiziksel doğrulama sayılmaz.

Yeni profiller; sabitlenmiş kaynak bağlantısı, lisans bilgisi, tam TV/kumanda kimliği, protokol verisi ve fiziksel test kaydı içermelidir. Kaynağı olmayan HDMI özellikleri eklenmemelidir.
