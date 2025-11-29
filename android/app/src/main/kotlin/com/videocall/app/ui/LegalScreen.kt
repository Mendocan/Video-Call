package com.videocall.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class LegalSection(
    val title: String,
    val content: String
)

@Composable
fun LegalScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sections = listOf(
        LegalSection(
            "Gizlilik Politikası",
            """
Video Call olarak, gizliliğinize büyük önem veriyoruz. Video Call, görüntülü görüşmelerinizi hiçbir sunucuda saklamaz. Uygulama peer-to-peer (P2P) teknoloji kullanarak çalışır ve görüşmeler doğrudan cihazlarınız arasında gerçekleşir.

Toplanan Minimum Bilgiler:
• Telefon numaranız (görüşme başlatmak için)
• Rehber bilgileri (izin verdiğiniz takdirde)
• Uygulama kullanım verileri (hata raporları, performans metrikleri)

Tüm görüşmeleriniz end-to-end şifreleme ile korunur. Görüşmeler hiçbir sunucuda saklanmaz ve yalnızca görüşme yaptığınız kişiyle paylaşılır.

Video Call, kişisel verilerinizi üçüncü taraflarla paylaşmaz. Uygulama tamamen bağımsız çalışır ve reklam veya analitik servisleri kullanmaz.

KVKK kapsamındaki haklarınızı kullanmak için kvkk@videocall.app adresine e-posta gönderebilirsiniz.
            """.trimIndent()
        ),
        LegalSection(
            "Kullanıcı Sözleşmesi",
            """
Video Call uygulamasını ("Uygulama") kullanarak, aşağıdaki kullanım koşullarını kabul etmiş sayılırsınız. Bu koşulları kabul etmiyorsanız, lütfen uygulamayı kullanmayın.

Hizmetin Kullanımı:
• Uygulamayı yalnızca yasal amaçlar için kullanabilirsiniz.
• Uygulamayı başkalarının haklarını ihlal edecek şekilde kullanamazsınız.
• Uygulamayı zararlı içerik paylaşmak için kullanamazsınız.
• Uygulamanın güvenliğini veya işleyişini bozmaya çalışamazsınız.

Gizlilik ve Veri Güvenliği:
Video Call, görüşmelerinizi hiçbir sunucuda saklamaz. Tüm görüşmeler doğrudan cihazlarınız arasında gerçekleşir ve end-to-end şifreleme ile korunur.

Sorumluluk Reddi:
Video Call uygulaması "olduğu gibi" sağlanmaktadır. Uygulamanın kesintisiz çalışması veya hatasız olması garanti edilmez. Ancak kritik hata durumlarında 7/24 destek ekibi müdahale eder.

Bu kullanım koşullarını istediğimiz zaman değiştirme hakkını saklı tutarız. Değişikliklerden sonra uygulamayı kullanmaya devam etmeniz, güncellenmiş koşulları kabul ettiğiniz anlamına gelir.
            """.trimIndent()
        ),
        LegalSection(
            "KVKK Aydınlatma Metni",
            """
6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") uyarınca, kişisel verileriniz aşağıdaki şekilde işlenmektedir:

Veri Sorumlusu: Video Call
E-posta: kvkk@videocall.app

İşlenen Kişisel Veriler:
• Kimlik Bilgileri: Telefon numarası
• İletişim Bilgileri: Rehber bilgileri (izin verdiğiniz takdirde)
• Teknik Veriler: Cihaz bilgileri, IP adresi, uygulama kullanım verileri

Kişisel Verilerin İşlenme Amaçları:
• Video görüşmelerinin gerçekleştirilmesi
• Uygulamanın teknik işleyişinin sağlanması
• Güvenlik ve kötüye kullanımın önlenmesi
• Uygulama performansının iyileştirilmesi
• Yasal yükümlülüklerin yerine getirilmesi

Kişisel Verilerin Aktarılması:
Video Call, kişisel verilerinizi üçüncü kişilere aktarmaz. Görüşmeleriniz peer-to-peer (P2P) teknoloji kullanılarak doğrudan cihazlarınız arasında gerçekleşir ve hiçbir sunucudan geçmez.

KVKK Kapsamındaki Haklarınız:
Kişisel verilerinizin işlenip işlenmediğini öğrenme, bilgi talep etme, silme veya yok etme, itiraz etme ve zararın giderilmesini talep etme haklarınız bulunmaktadır.

Bu haklarınızı kullanmak için kvkk@videocall.app adresine e-posta gönderebilirsiniz.
            """.trimIndent()
        ),
        LegalSection(
            "Sıkça Sorulan Sorular",
            """
S: Görüşmeler kaydediliyor mu?
C: Hayır, medya akışı cihazdan cihaza iletilir, merkezi kaydı yoktur.

S: Kişi bilgileri nerede saklanıyor?
C: Rehberden okunan bilgiler yalnızca davet ekranında gösterilir, sunucuya yazılmaz.

S: İzinleri geri alırsam ne olur?
C: Uygulama rehbere erişemez, ancak mevcut oda kodları ile çalışmaya devam eder.
            """.trimIndent()
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sections) { section ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = section.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = section.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

