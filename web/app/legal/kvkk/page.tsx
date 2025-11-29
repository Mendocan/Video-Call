import Link from 'next/link'
import Header from '@/components/Header'

export const metadata = {
  title: 'KVKK Aydınlatma Metni - Video Call',
  description: 'Video Call uygulaması KVKK aydınlatma metni ve kişisel veri işleme bilgileri.',
}

export default function KVKKPage() {
  return (
    <main className="min-h-screen bg-navy">
      <Header />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <h1 className="font-bold text-teal mb-8" style={{ fontSize: '18px' }}>
          KVKK Aydınlatma Metni
        </h1>
        <p className="text-slate mb-8">
          Son Güncelleme: {new Date().toLocaleDateString('tr-TR', { year: 'numeric', month: 'long', day: 'numeric' })}
        </p>

        <div className="prose prose-lg max-w-none space-y-8 text-slate">
          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>1. Veri Sorumlusu</h2>
            <p>
              6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") uyarınca, kişisel verileriniz 
              aşağıdaki şekilde işlenmektedir:
            </p>
            <div className="bg-slate/50 p-4 rounded-lg mt-4 border border-teal/10">
              <p><strong>Veri Sorumlusu:</strong> Video Call</p>
              <p><strong>E-posta:</strong> kvkk@videocall.app</p>
            </div>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>2. İşlenen Kişisel Veriler</h2>
            <p>
              Video Call uygulaması kapsamında aşağıdaki kişisel verileriniz işlenmektedir:
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li><strong>Kimlik Bilgileri:</strong> Telefon numarası</li>
              <li><strong>İletişim Bilgileri:</strong> Rehber bilgileri (izin verdiğiniz takdirde)</li>
              <li><strong>Teknik Veriler:</strong> Cihaz bilgileri, IP adresi, uygulama kullanım verileri</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>3. Kişisel Verilerin İşlenme Amaçları</h2>
            <p>
              Kişisel verileriniz aşağıdaki amaçlarla işlenmektedir:
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li>Video görüşmelerinin gerçekleştirilmesi</li>
              <li>Uygulamanın teknik işleyişinin sağlanması</li>
              <li>Güvenlik ve kötüye kullanımın önlenmesi</li>
              <li>Uygulama performansının iyileştirilmesi</li>
              <li>Yasal yükümlülüklerin yerine getirilmesi</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>4. Kişisel Verilerin İşlenme Hukuki Sebepleri</h2>
            <p>
              Kişisel verileriniz KVKK'nın 5. ve 6. maddelerinde belirtilen aşağıdaki hukuki sebeplere dayanarak işlenmektedir:
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li>Açık rızanız</li>
              <li>Sözleşmenin kurulması veya ifası ile doğrudan ilgili olması</li>
              <li>Yasal yükümlülüğün yerine getirilmesi</li>
              <li>Meşru menfaatlerimiz</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>5. Kişisel Verilerin Aktarılması</h2>
            <p>
              Video Call, kişisel verilerinizi üçüncü kişilere aktarmaz. Görüşmeleriniz peer-to-peer 
              (P2P) teknoloji kullanılarak doğrudan cihazlarınız arasında gerçekleşir ve hiçbir sunucudan geçmez.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>6. Kişisel Verilerin Saklanma Süresi</h2>
            <p>
              Kişisel verileriniz, işlenme amaçlarının gerektirdiği süre boyunca saklanır. Görüşmeleriniz 
              hiçbir sunucuda saklanmaz. Uygulama kullanım verileri, yasal saklama süreleri ve meşru 
              menfaatlerimiz çerçevesinde saklanır.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>7. KVKK Kapsamındaki Haklarınız</h2>
            <p>
              KVKK'nın 11. maddesi uyarınca aşağıdaki haklara sahipsiniz:
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li>Kişisel verilerinizin işlenip işlenmediğini öğrenme</li>
              <li>İşlenmişse buna ilişkin bilgi talep etme</li>
              <li>İşlenme amacını ve amacına uygun kullanılıp kullanılmadığını öğrenme</li>
              <li>Yurt içinde veya yurt dışında aktarıldığı üçüncü kişileri bilme</li>
              <li>Eksik veya yanlış işlenmişse düzeltilmesini isteme</li>
              <li>KVKK'da öngörülen şartlar çerçevesinde silinmesini veya yok edilmesini isteme</li>
              <li>Düzeltme, silme ve yok edilme işlemlerinin aktarıldığı üçüncü kişilere bildirilmesini isteme</li>
              <li>Münhasıran otomatik sistemler ile analiz edilmesi nedeniyle aleyhinize bir sonucun ortaya çıkmasına itiraz etme</li>
              <li>Kanuna aykırı işlenmesi sebebiyle zarara uğramanız halinde zararın giderilmesini talep etme</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>8. Başvuru Hakkı</h2>
            <p>
              KVKK kapsamındaki haklarınızı kullanmak için <strong>kvkk@videocall.app</strong> adresine 
              e-posta gönderebilirsiniz. Başvurunuz en geç 30 gün içinde sonuçlandırılacaktır.
            </p>
            <p className="mt-4">
              Ayrıca, Kişisel Verileri Koruma Kurulu'na şikayette bulunma hakkınız da bulunmaktadır.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>9. Güvenlik</h2>
            <p>
              Kişisel verilerinizin güvenliği için teknik ve idari önlemler alınmıştır. Tüm görüşmeleriniz 
              end-to-end şifreleme ile korunur ve hiçbir sunucuda saklanmaz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>10. İletişim</h2>
            <p>
              KVKK kapsamındaki haklarınız ve kişisel verilerinizin işlenmesi hakkında sorularınız için:
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

