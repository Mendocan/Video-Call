import Link from 'next/link'
import Header from '@/components/Header'

export const metadata = {
  title: 'Kullanım Koşulları - Video Call',
  description: 'Video Call uygulaması kullanım koşulları ve şartları.',
}

export default function TermsPage() {
  return (
    <main className="min-h-screen bg-navy">
      <Header />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <h1 className="font-bold text-teal mb-8" style={{ fontSize: '18px' }}>
          Kullanım Koşulları
        </h1>
        <p className="text-slate mb-8">
          Son Güncelleme: {new Date().toLocaleDateString('tr-TR', { year: 'numeric', month: 'long', day: 'numeric' })}
        </p>

        <div className="bg-teal/10 border-l-4 border-teal p-4 mb-8 rounded">
          <p className="text-teal font-semibold mb-2">Önemli Güncelleme:</p>
          <p className="text-slate text-sm">
            Kullanım Koşulları, merkeziyetsiz sistem ve kullanıcı sorumluluğu konularında güncellenmiştir. 
            Lütfen <strong>"5. Kullanıcı Sorumluluğu ve Yasadışı Kullanım"</strong> bölümünü dikkatle okuyunuz.
          </p>
        </div>

        <div className="prose prose-lg max-w-none space-y-8 text-slate">
          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>1. Genel Hükümler</h2>
            <p>
              Video Call uygulamasını ("Uygulama") kullanarak, aşağıdaki kullanım koşullarını kabul etmiş sayılırsınız. 
              Bu koşulları kabul etmiyorsanız, lütfen uygulamayı kullanmayın.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>2. Hizmetin Kullanımı</h2>
            <p>
              Video Call, kullanıcıların güvenli ve gizlilik odaklı video görüşmeler yapmasını sağlayan bir uygulamadır. 
              Uygulama peer-to-peer (P2P) teknoloji kullanarak çalışır.
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li>Uygulamayı yalnızca yasal amaçlar için kullanabilirsiniz.</li>
              <li>Uygulamayı başkalarının haklarını ihlal edecek şekilde kullanamazsınız.</li>
              <li>Uygulamayı zararlı içerik paylaşmak için kullanamazsınız.</li>
              <li>Uygulamanın güvenliğini veya işleyişini bozmaya çalışamazsınız.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>3. Gizlilik ve Veri Güvenliği</h2>
            <p>
              Video Call, görüşmelerinizi hiçbir sunucuda saklamaz. Tüm görüşmeler doğrudan cihazlarınız arasında 
              gerçekleşir ve end-to-end şifreleme ile korunur. Detaylı bilgi için Gizlilik Politikamızı inceleyebilirsiniz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>4. Sorumluluk Reddi</h2>
            <p>
              Video Call uygulaması "olduğu gibi" sağlanmaktadır. Uygulamanın kesintisiz çalışması veya hatasız 
              olması garanti edilmez. Uygulama kullanımından doğabilecek herhangi bir zarardan sorumlu değiliz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>5. Kullanıcı Sorumluluğu ve Yasadışı Kullanım</h2>
            <p className="mb-4">
              <strong>Merkeziyetsiz Sistem ve Kullanıcı Sorumluluğu:</strong> Video Call uygulaması, merkeziyetsiz 
              (decentralized) bir peer-to-peer (P2P) mimari kullanmaktadır. Görüşmeler doğrudan kullanıcıların 
              cihazları arasında gerçekleşir ve hiçbir sunucuda saklanmaz veya izlenmez.
            </p>
            <p className="mb-4">
              Bu nedenle, kullanıcılar arasında gerçekleşen görüşmelerde veya iletişimlerde meydana gelen her türlü 
              eylemden (tehdit, şantaj, hakaret, yasadışı içerik paylaşımı, kişisel veri ihlali, dolandırıcılık, 
              cinsel taciz, çocuk istismarı içeriği, terör propagandası ve benzeri suç teşkil eden veya hukuka aykırı 
              eylemler) tamamen ve münhasıran <strong>kullanıcılar sorumludur</strong>.
            </p>
            <p className="mb-4">
              <strong>Şirket Sorumluluğu:</strong> Video Call uygulaması sağlayıcısı, yukarıda belirtilen kullanıcılar 
              arası eylemlerden, görüşme içeriklerinden, kullanıcıların birbirlerine karşı gerçekleştirdiği eylemlerden 
              ve bu eylemlerden kaynaklanan her türlü zarardan <strong>hiçbir şekilde sorumlu tutulamaz</strong>. 
              Şirket, görüşme içeriklerini saklamadığı, izlemediği veya kontrol etmediği için, bu tür eylemlerden 
              haberdar olma veya müdahale etme imkanına sahip değildir.
            </p>
            <p className="mb-4">
              <strong>Yasal Sorumluluk:</strong> Kullanıcılar, uygulamayı kullanırken Türk Ceza Kanunu, Kişisel Verilerin 
              Korunması Kanunu, İnternet Ortamında Yapılan Yayınların Düzenlenmesi Hakkında Kanun ve diğer ilgili 
              mevzuat hükümlerine uymakla yükümlüdür. Suç teşkil eden kullanımlar, ilgili kullanıcıyı sorumlu tutar 
              ve kullanıcı bu tür eylemlerden dolayı cezai ve hukuki sorumluluğu kabul eder.
            </p>
            <p className="mb-4">
              <strong>Bildirim ve İşbirliği:</strong> Yasadışı kullanım tespit edildiğinde veya yasal makamlardan 
              talep geldiğinde, şirket mevcut teknik imkanlar dahilinde (cihaz ID, telefon numarası gibi metadata) 
              yasal makamlarla işbirliği yapabilir. Ancak görüşme içerikleri saklanmadığı için bu tür içerikler 
              sağlanamaz.
            </p>
            <p className="mb-4">
              <strong>Uygulama Kullanımı:</strong> Bu maddeyi kabul etmeyen kullanıcıların uygulamayı kullanmaması 
              gerekmektedir. Uygulamayı kullanmaya devam etmeniz, bu sorumlulukları kabul ettiğiniz anlamına gelir.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>6. Abonelik ve Cihaz Yönetimi</h2>
            <p>
              Abonelik, kişisel kullanım içindir ve devredilemez. Bir abonelik ile maksimum <strong>2 cihazda</strong> 
              uygulamayı kullanabilirsiniz. Bu limit, telefonunuzu yenileme hakkınızı korumak için belirlenmiştir.
            </p>
            <p className="mt-4">
              <strong>Güncellemeler:</strong> Abonelik süresi boyunca, tüm uygulama güncellemelerinden (yeni özellikler, 
              güvenlik güncellemeleri, performans iyileştirmeleri) ücretsiz olarak faydalanabilirsiniz. 
              Abonelik süreniz sona erdiğinde, güncellemelerden faydalanamazsınız.
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li>Abonelik, yalnızca abonelik oluşturan kişi tarafından kullanılabilir.</li>
              <li>Abonelik başka bir kişiye devredilemez veya satılamaz.</li>
              <li>APK dosyasını başka birine paylaşmanız, bu koşulları ihlal eder.</li>
              <li>Cihaz limiti, aboneliğin kötüye kullanılmasını önlemek ve adil kullanımı sağlamak için uygulanmaktadır.</li>
              <li>Telefonunuzu yenilediğinizde, eski cihazı kaldırarak yeni cihazınızı kaydedebilirsiniz.</li>
            </ul>
            <p className="mt-4">
              Bu uygulama, 6502 sayılı Tüketicinin Korunması Hakkında Kanun ve Mesafeli Sözleşmeler Yönetmeliği 
              çerçevesinde yasaldır. Detaylı bilgi için <Link href="/faq" className="text-accent hover:underline">SSS</Link> sayfamızı inceleyebilirsiniz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>7. Satıcı Hakları ve İhlal Durumları</h2>
            <p>
              Aşağıdaki durumlarda, satıcı aboneliği iptal etme ve yasal işlem başlatma hakkına sahiptir:
            </p>
            <ul className="list-disc pl-6 space-y-2 mt-4">
              <li>APK dosyasının başka birine paylaşılması veya dağıtılması</li>
              <li>Aboneliğin başka bir kişiye devredilmesi veya satılması</li>
              <li>Aboneliğin kötüye kullanılması (örneğin, ticari amaçlarla kullanım)</li>
              <li>Kullanım Koşullarının ihlali</li>
              <li>Uygulamanın güvenliğini veya işleyişini bozmaya yönelik girişimler</li>
              <li>Sahte veya hileli ödeme işlemleri</li>
            </ul>
            <p className="mt-4">
              Bu haklar, 6502 sayılı Tüketicinin Korunması Hakkında Kanun ve Mesafeli Sözleşmeler Yönetmeliği 
              çerçevesinde yasaldır. İhlal durumunda, abonelik anında iptal edilir ve iade yapılmaz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>8. Fikri Mülkiyet</h2>
            <p>
              Video Call uygulaması ve tüm içeriği telif hakkı ve diğer fikri mülkiyet yasaları ile korunmaktadır. 
              Uygulamanın içeriğini izinsiz kopyalayamaz, dağıtamaz veya değiştiremezsiniz.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>9. Değişiklikler</h2>
            <p>
              Bu kullanım koşullarını istediğimiz zaman değiştirme hakkını saklı tutarız. Önemli değişiklikler 
              uygulama içinde bildirilecektir. Değişikliklerden sonra uygulamayı kullanmaya devam etmeniz, 
              güncellenmiş koşulları kabul ettiğiniz anlamına gelir.
            </p>
          </section>

          <section>
            <h2 className="font-bold text-teal mb-4" style={{ fontSize: '16px' }}>10. İletişim</h2>
            <p>
              Kullanım koşulları hakkında sorularınız için bizimle iletişime geçebilirsiniz:
            </p>
            <p className="mt-2">
              <strong>E-posta:</strong> info@videocall.app
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

