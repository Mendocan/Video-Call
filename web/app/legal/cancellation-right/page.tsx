import Link from 'next/link'
import Header from '@/components/Header'

export const metadata = {
  title: 'Cayma Hakkı - Video Call',
  description: 'Video Call uygulaması cayma hakkı ve iptal koşulları.',
}

export default function CancellationRightPage() {
  return (
    <main className="min-h-screen bg-navy w-full flex flex-col items-center">
      <Header />
      <div className="w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <h1 className="font-bold text-teal mb-8" style={{ fontSize: '18px' }}>
          Cayma Hakkı
        </h1>
        <p className="text-slate mb-8" style={{ fontSize: '14px' }}>
          Son Güncelleme: {new Date().toLocaleDateString('tr-TR', { year: 'numeric', month: 'long', day: 'numeric' })}
        </p>

        <div className="space-y-8 text-slate" style={{ fontSize: '14px' }}>
          <section className="bg-slate/30 p-6 rounded-lg" style={{ borderRadius: '4px', border: '2px solid #00B8D4' }}>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>
              ⚠️ ÖNEMLİ BİLGİ
            </h2>
            <p className="mb-4">
              <strong className="text-teal">6502 sayılı Tüketicinin Korunması Hakkında Kanun</strong> ve{' '}
              <strong className="text-teal">Mesafeli Sözleşmeler Yönetmeliği</strong> uyarınca, dijital içerik ve hizmetlerde 
              cayma hakkı, <strong>hizmetin ifasına başlanmadan önce</strong> kullanılabilir.
            </p>
            <p>
              Abonelik aktif hale geldikten ve hizmet kullanılmaya başlandıktan sonra cayma hakkı kullanılamaz. 
              Ancak, hizmetin ifasına başlanmadan önce cayma hakkınızı kullanabilirsiniz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>1. Cayma Hakkının Kapsamı</h2>
            <p className="mb-4">
              Video Call uygulaması abonelik hizmeti, dijital içerik ve hizmet kapsamındadır. 
              Bu nedenle, cayma hakkı yalnızca hizmetin ifasına başlanmadan önce kullanılabilir.
            </p>
            <div className="bg-navy p-4 rounded-lg" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p className="font-semibold text-teal mb-2">Cayma Hakkı Kullanılabilir Durumlar:</p>
              <ul className="list-disc list-inside space-y-1 ml-4">
                <li>Abonelik satın alındıktan sonra, hizmet aktif hale gelmeden önce</li>
                <li>APK indirilmeden ve uygulama kullanılmaya başlanmadan önce</li>
                <li>Ödeme yapıldıktan sonra 14 gün içinde (hizmet kullanılmadıysa)</li>
              </ul>
            </div>
            <div className="bg-navy p-4 rounded-lg mt-4" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p className="font-semibold text-teal mb-2">Cayma Hakkı Kullanılamaz Durumlar:</p>
              <ul className="list-disc list-inside space-y-1 ml-4">
                <li>Abonelik aktif hale geldikten sonra</li>
                <li>APK indirildikten ve uygulama kullanılmaya başlandıktan sonra</li>
                <li>Hizmetin ifasına başlandıktan sonra</li>
              </ul>
            </div>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>2. Cayma Hakkının Kullanılması</h2>
            <p className="mb-4">
              Cayma hakkınızı kullanmak istediğinizde, aşağıdaki yöntemlerden biriyle bize ulaşabilirsiniz:
            </p>
            <div className="bg-slate/30 p-6 rounded-lg" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p className="font-semibold text-teal mb-4">İletişim Bilgileri:</p>
              <p className="mb-2">E-posta: info@videocall.app</p>
              <p className="mb-2">Telefon: [Telefon Numarası]</p>
              <p className="mb-4">Adres: [Şirket Adresi]</p>
              <p className="text-teal/70 text-sm">
                Cayma talebinizde adınız, soyadınız, e-posta adresiniz ve abonelik numaranızı belirtmeniz gerekmektedir.
              </p>
            </div>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>3. İade İşlemleri</h2>
            <ul className="list-disc list-inside space-y-2 ml-4">
              <li>
                <strong className="text-teal">Tam İade:</strong> Hizmetin ifasına başlanmadan önce yapılan iptal talepleri için, 
                ödemenin tamamı iade edilir.
              </li>
              <li>
                <strong className="text-teal">Kısmi İade:</strong> Hizmetin ifasına başlandıktan sonra iptal talepleri için, 
                kullanılan süre oranında hesaplanır ve kalan süre iade edilir.
              </li>
              <li>
                <strong className="text-teal">İade Süresi:</strong> İade işlemleri, ödeme yöntemine göre 5-10 iş günü içinde tamamlanır.
              </li>
              <li>
                <strong className="text-teal">İade Yöntemi:</strong> İade, ödeme yapılan yönteme (kredi kartı, banka kartı vb.) geri yapılır.
              </li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>4. Cayma Hakkının Süresi</h2>
            <p className="mb-4">
              Cayma hakkı, sözleşmenin kurulduğu tarihten itibaren <strong className="text-teal">14 gün</strong> içinde kullanılabilir. 
              Ancak, hizmetin ifasına başlanmışsa bu süre geçerli değildir.
            </p>
            <div className="bg-slate/30 p-4 rounded-lg" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p className="text-sm text-teal/70">
                <strong>Not:</strong> Hizmetin ifasına başlanması, APK'nın indirilmesi ve uygulamanın kullanılmaya başlanması anlamına gelir.
              </p>
            </div>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>5. Abonelik İptali</h2>
            <p className="mb-4">
              Abonelik süresi dolduğunda otomatik olarak yenilenmez. Yeni bir abonelik satın almak için tekrar ödeme yapmanız gerekir.
            </p>
            <p>
              Mevcut aboneliğinizi iptal etmek isterseniz, yukarıda belirtilen iletişim bilgileri üzerinden bize ulaşabilirsiniz. 
              İptal işlemi, abonelik süresinin sonuna kadar geçerlidir.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>6. Tüketici Hakları</h2>
            <p className="mb-4">
              Bu sözleşmeden kaynaklanan uyuşmazlıkların çözümünde, Türkiye Cumhuriyeti yasaları uygulanır. 
              Uyuşmazlıklar öncelikle müzakere yoluyla çözülmeye çalışılır.
            </p>
            <p className="mb-4">
              Çözülemediği takdirde, aşağıdaki mercilere başvurabilirsiniz:
            </p>
            <ul className="list-disc list-inside space-y-2 ml-4">
              <li><strong className="text-teal">Tüketici Hakem Heyetleri:</strong> İl ve ilçe tüketici hakem heyetleri</li>
              <li><strong className="text-teal">Tüketici Mahkemeleri:</strong> Tüketici sorunları hakimlikleri</li>
              <li><strong className="text-teal">Tüketici Sorunları Hakem Heyeti:</strong> Online başvuru: tüketici.gov.tr</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>7. İletişim</h2>
            <div className="bg-slate/30 p-6 rounded-lg" style={{ borderRadius: '4px', border: '1px solid #00B8D4' }}>
              <p className="font-semibold text-teal mb-4">Video Call - Müşteri Hizmetleri</p>
              <p className="mb-2">E-posta: info@videocall.app</p>
              <p className="mb-2">Telefon: [Telefon Numarası]</p>
              <p className="mb-2">Adres: [Şirket Adresi]</p>
              <p className="text-sm text-teal/70 mt-4">
                Cayma talepleriniz için yukarıdaki iletişim bilgilerini kullanabilirsiniz. 
                Talebiniz en geç 1 iş günü içinde değerlendirilir.
              </p>
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

