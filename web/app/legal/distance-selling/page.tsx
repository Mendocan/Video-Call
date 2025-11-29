import Link from 'next/link'
import Header from '@/components/Header'

export const metadata = {
  title: 'Mesafeli Satış Sözleşmesi - Video Call',
  description: 'Video Call uygulaması mesafeli satış sözleşmesi ve koşulları.',
}

export default function DistanceSellingPage() {
  return (
    <main className="min-h-screen bg-navy w-full flex flex-col items-center">
      <Header />
      <div className="w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <h1 className="font-bold text-teal mb-8" style={{ fontSize: '18px' }}>
          Mesafeli Satış Sözleşmesi
        </h1>
        <p className="text-slate mb-8" style={{ fontSize: '14px' }}>
          Son Güncelleme: {new Date().toLocaleDateString('tr-TR', { year: 'numeric', month: 'long', day: 'numeric' })}
        </p>

        <div className="space-y-8 text-slate" style={{ fontSize: '14px' }}>
          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>1. Taraflar</h2>
            <p className="mb-4">
              Bu mesafeli satış sözleşmesi ("Sözleşme"), aşağıdaki taraflar arasında aşağıda belirtilen hüküm ve şartlara göre feshedilmiş bulunmaktadır.
            </p>
            <div className="bg-slate/30 p-4 rounded-lg mb-4" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p className="font-semibold text-teal mb-2">SATICI:</p>
              <p>Video Call</p>
              <p className="mt-2">Adres: [Şirket Adresi]</p>
              <p>E-posta: info@videocall.app</p>
              <p>Telefon: [Telefon Numarası]</p>
            </div>
            <div className="bg-slate/30 p-4 rounded-lg" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p className="font-semibold text-teal mb-2">ALICI:</p>
              <p>Bu sözleşme, Video Call uygulamasına abonelik satın alan gerçek veya tüzel kişi müşterileri kapsamaktadır.</p>
            </div>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>2. Konu</h2>
            <p>
              Bu sözleşmenin konusu, Alıcı'nın satın aldığı Video Call uygulaması yıllık abonelik hizmetidir. 
              Abonelik kapsamında Alıcı'ya sunulan hizmetler, uygulama içinde belirtilen özelliklerle sınırlıdır.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>3. Sipariş ve Ödeme</h2>
            <ul className="list-disc list-inside space-y-2 ml-4">
              <li>Alıcı, web sitesi üzerinden abonelik satın alırken gerekli bilgileri eksiksiz ve doğru olarak doldurmakla yükümlüdür.</li>
              <li>Ödeme, kredi kartı, banka kartı veya diğer kabul edilen ödeme yöntemleri ile yapılabilir.</li>
              <li>Ödeme işlemi tamamlandıktan sonra abonelik aktif hale gelir.</li>
              <li>Tüm fiyatlar KDV dahildir ve Türk Lirası (TRY) cinsindendir.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>4. Cayma Hakkı</h2>
            <p className="mb-4">
              6502 sayılı Tüketicinin Korunması Hakkında Kanun ve Mesafeli Sözleşmeler Yönetmeliği uyarınca, 
              dijital içerik ve hizmetlerde cayma hakkı, hizmetin ifasına başlanmadan önce kullanılabilir.
            </p>
            <p className="mb-4">
              <strong className="text-teal">ÖNEMLİ:</strong> Abonelik aktif hale geldikten ve hizmet kullanılmaya başlandıktan sonra cayma hakkı kullanılamaz. 
              Ancak, hizmetin ifasına başlanmadan önce cayma hakkınızı kullanabilirsiniz.
            </p>
            <p>
              Cayma hakkını kullanmak isteyen Alıcı, aşağıdaki iletişim bilgileri üzerinden Satıcı'ya başvuruda bulunabilir:
            </p>
            <div className="bg-slate/30 p-4 rounded-lg mt-4" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p>E-posta: info@videocall.app</p>
              <p>Telefon: [Telefon Numarası]</p>
            </div>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>5. İade ve İptal</h2>
            <ul className="list-disc list-inside space-y-2 ml-4">
              <li>Hizmetin ifasına başlanmadan önce yapılan iptal talepleri, ödemenin tamamı iade edilir.</li>
              <li>Hizmetin ifasına başlandıktan sonra iptal talepleri, kullanılan süre oranında hesaplanır ve kalan süre iade edilir.</li>
              <li>İade işlemleri, ödeme yöntemine göre 5-10 iş günü içinde tamamlanır.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>6. Hizmetin İfası</h2>
            <p>
              Abonelik aktif hale geldikten sonra, Alıcı Video Call uygulamasını indirip kullanmaya başlayabilir. 
              Abonelik süresi boyunca, uygulama içinde belirtilen tüm özelliklerden yararlanabilir.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>7. Sorumluluklar</h2>
            <ul className="list-disc list-inside space-y-2 ml-4">
              <li>Satıcı, hizmetin eksiksiz ve zamanında ifasından sorumludur.</li>
              <li>Alıcı, uygulamayı yalnızca yasal amaçlarla kullanmakla yükümlüdür.</li>
              <li>Sunucusuz P2P mimari nedeniyle, görüşmeler doğrudan cihazlar arasında gerçekleşir ve sunucuda saklanmaz.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>8. Gizlilik ve Veri Koruma</h2>
            <p>
              Kişisel verilerin korunması hakkında detaylı bilgi için lütfen{' '}
              <Link href="/legal/privacy" className="text-teal hover:text-accent underline">
                Gizlilik Politikası
              </Link>{' '}
              ve{' '}
              <Link href="/legal/kvkk" className="text-teal hover:text-accent underline">
                KVKK Aydınlatma Metni
              </Link>{' '}
              sayfalarını inceleyiniz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>9. Uyuşmazlıkların Çözümü</h2>
            <p>
              Bu sözleşmeden kaynaklanan uyuşmazlıkların çözümünde, Türkiye Cumhuriyeti yasaları uygulanır. 
              Uyuşmazlıklar öncelikle müzakere yoluyla çözülmeye çalışılır. Çözülemediği takdirde, 
              Tüketici Hakem Heyetleri ve Tüketici Mahkemeleri yetkilidir.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>10. İletişim</h2>
            <div className="bg-slate/30 p-4 rounded-lg" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p className="font-semibold text-teal mb-2">Video Call</p>
              <p>E-posta: info@videocall.app</p>
              <p>Telefon: [Telefon Numarası]</p>
              <p>Adres: [Şirket Adresi]</p>
            </div>
          </section>
        </div>

        <div className="mt-12 pt-8 text-center" style={{ borderTop: '1px solid #00B8D4' }}>
          <Link
            href="/"
            className="text-teal hover:text-accent font-medium"
            style={{ fontSize: '14px' }}
          >
            ← Ana Sayfaya Dön
          </Link>
        </div>
      </div>
    </main>
  )
}

