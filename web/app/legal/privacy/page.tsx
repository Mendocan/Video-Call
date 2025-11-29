import Link from 'next/link'
import Header from '@/components/Header'

export const metadata = {
  title: 'Gizlilik Politikası - Video Call',
  description: 'Video Call uygulaması gizlilik politikası ve veri koruma bilgileri.',
}

export default function PrivacyPage() {
  return (
    <main className="min-h-screen bg-navy">
      <Header />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <h1 className="font-bold text-teal mb-8" style={{ fontSize: '18px' }}>
          Gizlilik Politikası
        </h1>
        <p className="text-slate mb-8">
          Son Güncelleme: {new Date().toLocaleDateString('tr-TR', { year: 'numeric', month: 'long', day: 'numeric' })}
        </p>

        <div className="prose prose-lg max-w-none space-y-8 text-slate">
          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>1. Gizliliğinize Verdiğimiz Önem</h2>
            <p>
              Video Call olarak, gizliliğinize büyük önem veriyoruz. Bu gizlilik politikası, uygulamamızı 
              kullanırken toplanan bilgilerin nasıl işlendiğini açıklamaktadır.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>2. Toplanan Bilgiler</h2>
            <p>
              Video Call, görüşmelerinizi hiçbir sunucuda saklamaz. Uygulama peer-to-peer (P2P) teknoloji 
              kullanarak çalışır ve görüşmeler doğrudan cihazlarınız arasında gerçekleşir.
            </p>
            <p className="mt-4">
              <strong>Toplanan Minimum Bilgiler:</strong>
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-2">
              <li>Telefon numaranız (görüşme başlatmak için)</li>
              <li>Rehber bilgileri (izin verdiğiniz takdirde)</li>
              <li>Uygulama kullanım verileri (hata raporları, performans metrikleri)</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>3. Verilerin Kullanımı</h2>
            <p>
              Toplanan bilgiler yalnızca aşağıdaki amaçlar için kullanılır:
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li>Video görüşmelerinin gerçekleştirilmesi</li>
              <li>Uygulamanın iyileştirilmesi ve hata düzeltmeleri</li>
              <li>Güvenlik ve kötüye kullanımın önlenmesi</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>4. Veri Güvenliği</h2>
            <p>
              Tüm görüşmeleriniz end-to-end şifreleme ile korunur. Görüşmeler hiçbir sunucuda saklanmaz 
              ve yalnızca görüşme yaptığınız kişiyle paylaşılır.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>5. Üçüncü Taraf Paylaşımı</h2>
            <p>
              Video Call, kişisel verilerinizi üçüncü taraflarla paylaşmaz. Uygulama tamamen bağımsız 
              çalışır ve reklam veya analitik servisleri kullanmaz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>6. KVKK Haklarınız</h2>
            <p>
              6698 sayılı Kişisel Verilerin Korunması Kanunu kapsamında aşağıdaki haklara sahipsiniz:
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li>Kişisel verilerinizin işlenip işlenmediğini öğrenme</li>
              <li>İşlenen kişisel verileriniz hakkında bilgi talep etme</li>
              <li>Kişisel verilerinizin silinmesini veya yok edilmesini isteme</li>
              <li>İşlenen verilerin münhasıran otomatik sistemler ile analiz edilmesi nedeniyle aleyhinize bir sonucun ortaya çıkmasına itiraz etme</li>
            </ul>
            <p className="mt-4">
              Bu haklarınızı kullanmak için: <strong>kvkk@videocall.app</strong> adresine e-posta gönderebilirsiniz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>7. Çocukların Gizliliği</h2>
            <p>
              Video Call, 13 yaş altındaki çocuklardan bilerek veri toplamaz. Eğer bir çocuğun kişisel 
              verilerini topladığımızı fark edersek, bu verileri derhal sileriz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>8. Politika Değişiklikleri</h2>
            <p>
              Bu gizlilik politikasını zaman zaman güncelleyebiliriz. Önemli değişiklikler uygulama 
              içinde bildirilecektir. Bu sayfayı düzenli olarak kontrol etmenizi öneririz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>9. İletişim</h2>
            <p>
              Gizlilik politikası hakkında sorularınız için bizimle iletişime geçebilirsiniz:
            </p>
            <p className="mt-2">
              <strong>E-posta:</strong> kvkk@videocall.app
            </p>
          </section>
        </div>

        <div className="mt-12 pt-8 border-t border-teal/20">
          <Link
            href="/"
            className="text-teal hover:text-accent font-medium"
          >
            ← Ana Sayfaya Dön
          </Link>
        </div>
      </div>
    </main>
  )
}

