# Aşama 1 – Teknoloji altyapısı güncellemesi

Tarih: 21 Kasım 2026

## Seçilen kararlı sürüm seti

| Bileşen | Sürüm |
|---|---:|
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.14.8 |
| JDK | 17 |
| Kotlin | 2.4.21 |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00 |
| Room | 2.8.5 |
| Navigation Compose | 2.9.8 |
| Hilt | 2.60.1 |
| AndroidX Hilt Lifecycle ViewModel Compose | 1.3.0 |
| WorkManager | 2.12.0 |
| DataStore | 1.3.0 |
| Core KTX | 1.19.1 |
| Lifecycle | 2.10.0 |
| Activity Compose | 1.13.0 |
| Fragment | 1.9.1 |
| Coroutines | 1.11.0 |
| Robolectric | 4.16 |
| AndroidX Test Core / Runner | 1.7.0 / 1.7.0 |
| AndroidX Test Ext JUnit | 1.3.0 |
| SQLCipher Android | 4.6.1 (bilerek korundu) |
| compileSdk / targetSdk | 36 / 36 |
| minSdk | 26 (korundu) |

## Uyumluluk gerekçesi

- Kotlin Gradle Plugin 2.4.21 için Kotlin'in yayımladığı tam destek matrisi Gradle 7.6.3–9.7.0 ve AGP 8.5.2–9.3.1 aralığını gösterir.
- KSP 2.3.12, Kotlin 2.3.20–2.4.x, AGP 8.13.0+ ve Gradle 8.13+ ile uyumludur.
- Bu nedenle AGP 8.13.2 + Gradle 8.14.8 + Kotlin 2.4.21 + KSP 2.3.12, mevcut `org.jetbrains.kotlin.android` eklentisini koruyan ve yayımlanmış destek aralıklarına uyan bir geçiş setidir.
- AGP 9.x, yerleşik Kotlin desteği ve yeni DSL geçişi gerektirdiğinden bu aşamada zorla uygulanmadı. Ayrı bir geçiş ve test aşamasında ele alınmalıdır.
- Compose BOM 2026.11.01, compileSdk 37.1 gerektirir; AGP 8.13 serisinin üst sınırı API 36.1 olduğundan bu sürüm bu yapılandırmaya alınmadı. Compose BOM 2026.09.00 seçildi.
- AndroidX Hilt 1.4.0 ve Lifecycle 2.11.x, Compose compileSdk 37 / AGP 9.2.0 tabanına yükseldi. Navigation 2.10.x de API 37 uyumluluk dalına geçti. AGP 8.13.2 + compileSdk 36 korunacağı için Hilt 1.3.0, Lifecycle 2.10.0 ve Navigation 2.9.8 kullanıldı. Hilt ViewModel Compose 1.3.0'ın yeni artifact ve package yapısına kaynak kod importları taşındı.
- Google Play'in 31 Ağustos 2026 sonrası yeni uygulama ve güncellemeler için API 36 hedef şartı nedeniyle targetSdk 36 yapıldı.
- SQLCipher sürümü değiştirilmedi. Veritabanı şifreleme katmanını teknoloji güncellemesiyle aynı anda değiştirmek, mevcut kullanıcı verileri açısından gereksiz risk oluşturur.

## Yapılan değişiklikler

- Sürümler `gradle/libs.versions.toml` içinde merkezi olarak güncellendi.
- `compileSdk` ve `targetSdk` 36 yapıldı; `minSdk` 26 korundu.
- CI iş akışlarındaki Gradle sürümü ortak kurulum eyleminde merkezileştirildi (8.14.8).
- Android SDK Platform 36 ve Build Tools 36.0.0 kurulumu ortak eyleme alındı.
- Wrapper dağıtım SHA-256 değeri `gradle-wrapper.properties` içine eklendi. Wrapper JAR ve başlatıcı betikleri ilk CI kurulumunda üretilecek.
- Kod kalitesi, cihaz testi ve Room şema üretimi iş akışları ortak Android kurulum eylemini kullanacak şekilde düzenlendi.
- Android Lint hataları artık sessizce geçilmeyecek.
- İmzalı release işi, birim testleri ve Android Lint başarılı olmadan çalışmayacak şekilde bağımlılıklandırıldı.
- AndroidX Test Core, Runner, Ext JUnit ve Robolectric sürümleri güncellendi.
- Hilt Compose importları 1.3.0'ın resmi `hilt-lifecycle-viewmodel-compose` artifact ve package yapısına geçirildi.
- README ve bu teknik rapor doğrulama sınırlarıyla eşitlendi.

## Bilinçli olarak yapılmayanlar

- Room 3'e geçilmedi.
- SQLCipher sürümü ve veritabanı anahtar yönetimi değiştirilmedi.
- JDK 21'e geçilmedi; JDK 17 bu yapılandırma için yeterlidir.
- AGP 9.x'in yerleşik Kotlin ve yeni DSL geçişi bu aşamaya dahil edilmedi.

## Doğrulama durumu

Bu çalışma ortamında Gradle kurulu değil ve dış ağ erişimi bulunmuyor. Bu nedenle Wrapper JAR/başlatıcı betikleri üretilemedi; Gradle senkronizasyonu, `assembleDebug`, birim testleri, lint ve emülatör testleri çalıştırılamadı. `gradle-wrapper.properties` ve SHA-256 dağıtım kontrolü eklendi. `Proje dosyalarını tamamla` GitHub Actions iş akışı ortak kurulum eylemi üzerinden wrapper dosyalarını ve Room şemalarını üretip depoya işleyecek şekilde düzenlendi.

Yerel doğrulama sırası:

1. `./gradlew --version`
2. `./gradlew help --stacktrace`
3. `./gradlew assembleDebug --stacktrace`
4. `./gradlew testDebugUnitTest --stacktrace`
5. `./gradlew connectedDebugAndroidTest --stacktrace`
6. `./gradlew lintDebug`
7. Actions → `Proje dosyalarını tamamla (Gradle wrapper + Room şemaları)` workflow'unu çalıştırıp `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` ve `app/schemas/**` dosyalarının depoya işlendiğini doğrulayın.
8. Actions → `Kod kalitesi (elle)` ve `Cihaz testleri (emülatör)` workflow'larını çalıştırın.

Özellikle Room 2.8.5 + SQLCipher 4.6.1 entegrasyonu ve Hilt/KSP kod üretimi cihaz/emülatör testleriyle doğrulanmadan üretim APK'sı yayımlanmamalıdır.
