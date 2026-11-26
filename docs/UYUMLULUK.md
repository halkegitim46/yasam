# Sürüm uyumluluğu rehberi (Aşama B: AGP 9 / Kotlin 2.4 / Android 16)

Android derlemesinde en sık kırılma nedeni **birbirine bağlı sürümlerin ayrı ayrı yükseltilmesidir**. CI'daki
**preflight → "Sürüm uyumluluğu"** adımı aşağıdaki kuralları derlemeden ÖNCE (saniyeler içinde, nedeniyle birlikte) denetler.

## Güncel küme
| Bileşen | Sürüm | Not |
|---|---|---|
| Android Gradle Plugin | **9.5.2** | yerleşik Kotlin; en çok API 37; Gradle ≥ 9.6 |
| Gradle | **9.7.1** | AGP alt sınırı (9.6) ile Kotlin Gradle eklentisinin test ettiği en yeni sürümün (9.7) kesişimi; 9.9.x sonra denenir |
| JDK | 17 | AGP 9.5 ve Gradle 9 için asgari |
| Kotlin | **2.4.21** | AGP ≥ 9.1 ve Hilt ≥ 2.60 ister |
| KSP | **2.3.14** | Kotlin'den bağımsız sürüm; ≥ 2.3.12 için AGP ≥ 8.12 |
| Hilt / Dagger | **2.60.1** | ≥ 2.59 yalnızca AGP 9+ ile çalışır |
| androidx.hilt | 1.3.0 | `hilt-lifecycle-viewmodel-compose` (`hiltViewModel` yeni paketi) |
| Room | **2.8.5** | 2.x serisinin güncel sürümü (Room 3 ayrı bir geçiş) |
| Compose BOM | **2026.11.01** | Compose 1.13 / Material 3 1.5; **compileSdk 37.1** + AGP 9 ister |
| Navigation | 2.10.2 | |
| WorkManager | 2.12.0 | |
| DataStore | 1.3.0 | |
| Lifecycle | 2.11.1 | |
| Activity Compose | **1.13.0** | (1.14.0 henüz yalnızca alfa) |
| Core KTX / Fragment | 1.19.1 / 1.9.1 | |
| Coroutines | 1.11.0 | |
| Robolectric | 4.17 | testler `sdk = [34]` ile koşar; android-all jar'ı CI'da önceden indirilir (aşağıya bakın) |
| SQLCipher | 4.6.1 (korundu) | 16 KB sayfa desteği belgeli; veri biçimi 4.x boyunca uyumlu. Güncelleme ayrı ve cihaz testiyle |
| compileSdk / targetSdk / minSdk | **37.1 / 36 / 26** | targetSdk 36: Android 16 davranışları; compileSdk yalnızca derleme zamanı API'leri |

## Bilinçli sapmalar (neden "en yeni" değil)
- **Gradle 9.9.x değil 9.7.1:** Kotlin Gradle eklentisi 2.4.21 en çok Gradle 9.7 ile test edilir; AGP 9.5'in alt sınırı 9.6'dır. Yeşil çıkınca 9.9.x ayrı bir adımda denenir.
- **Compose BOM:** 2026.11.01 (Compose 1.13) kullanılır; bu BOM compileSdk **37.1** ister (`compileSdk { version = release(37) { minorApiLevel = 1 } }`). Sorun çıkarsa geri dönüş: BOM 2026.09.00 + compileSdk 37.
- **Activity 1.14.0 değil 1.13.0**, **KSP 2.3.12 değil 2.3.14**: sürüm tablosundaki 1.14.0 yalnızca alfa; 2.3.14 en yeni kararlı KSP.
- **Room 3 / Navigation 3 / AGP 10 hazırlığı:** ayrı, büyük geçişler; bu aşamaya alınmadı.

## AGP 9 geçişinde yapılan yapısal değişiklikler
1. `org.jetbrains.kotlin.android` eklentisi kaldırıldı (kök ve `app`); Kotlin yerleşik. Compose eklentisi (sürümü = Kotlin) ve KSP kalır; Compose eklentisi Kotlin 2.4.x'i derleme sınıf yoluna taşır.
2. `kotlin { jvmToolchain(17) }` `android {}` bloğunun dışına alındı.
3. `resourceConfigurations` → `android { androidResources { localeFilters += "tr" } }`.
4. `compileSdk 37`, `targetSdk 36`; manifestte `android:enableOnBackInvokedCallback="true"` (tahmini geri hareketi).
5. `hiltViewModel` yeni pakete (`androidx.hilt.lifecycle.viewmodel.compose`); `TabRow`/`ScrollableTabRow` → `PrimaryTabRow`/`PrimaryScrollableTabRow` (Material 3 1.4 önerisi).
6. Kullanılmayan `ui-tooling-preview` ve `room-testing` bağımlılıkları kaldırıldı.
7. Gradle belleği 4 GB.
8. **Robolectric android-all jar'ı CI'da önceden indirilir**: Maven Central, GitHub runner'larına `HTTP 429` verip (Gradle'ın kendi indirmesi dahil) testleri düşürüyordu. CI sırasıyla önbelleği, Google'ın Maven Central yansımasını ve Maven Central'ı dener (yeniden deneme + SHA-1 doğrulaması) ve jar'ı `app/build/robolectric-deps` altına koyar; `build.gradle.kts` jar varsa Robolectric'i çevrimdışı çalıştırır, yoksa (yerel geliştirme) Robolectric kendisi indirir. Robolectric yükseltilince `libs.versions.toml` içindeki `robolectricAndroidAll` sürümü de güncellenmelidir (Robolectric günlüğündeki `android-all-instrumented:<sürüm>` ile aynı olmalı).

## Android 16 (targetSdk 36) davranış kontrolü
| Davranış | Durum |
|---|---|
| Kenardan kenara zorunlu (opt-out kaldırıldı) | `enableEdgeToEdge()` ve `Scaffold` ek alanları zaten kullanılıyor; tema opt-out kullanmıyor |
| Tahmini geri hareketi varsayılan | `BackHandler` kullanılıyor (`onBackPressed` yok); manifestte açıkça etkin |
| Büyük ekranda yön/yeniden boyutlandırma kısıtları yok sayılır | Manifestte yön/`resizeableActivity` kısıtı yok |
| İş zamanlama kotaları | WorkManager kullanılıyor, tam zamanlı alarm yok |
| 16 KB bellek sayfası | SQLCipher 4.6.1 belgeli destek; cihazda doğrulanmalı |

## Birlikte değişen gruplar
1. **Kotlin + KSP + Hilt + AGP**: Kotlin 2.4 → AGP ≥ 9.1, Hilt ≥ 2.60; Hilt ≥ 2.59 → AGP 9+; KSP ≥ 2.3.12 → AGP ≥ 8.12.
2. **AGP + Gradle**: 8.13 → 8.13; 9.0–9.4 → ≥ 9.1; 9.5 → ≥ 9.6. (`GRADLE_VERSION` iş akışında tek yerde.)
3. **compileSdk + Compose**: BOM ≥ 2026.09 → compileSdk 37; BOM ≥ 2026.11 → 37.1.

## Hata iletisi → neden → çözüm
| İleti (özet) | Neden | Çözüm |
|---|---|---|
| `only compatible with AGP version 9.0.0 or higher` | Hilt ≥ 2.59, AGP 8.x ile | AGP 9'a geçin ya da Hilt 2.58 |
| `Minimum supported Gradle version is X` | AGP, Gradle'dan yeni | `GRADLE_VERSION` yükseltin |
| `The Android Gradle plugin supports only Kotlin Gradle plugin version ...` | Kotlin ↔ AGP uyumsuz | Kotlin'i tabloya göre ayarlayın |
| `requires libraries ... compile against version 37 or later` | Kütüphane daha yeni compileSdk ister | compileSdk'i yükseltin (AGP desteği dahilinde) |
| `Provided Metadata instance has version 2.4...` | Hilt/Dagger, Kotlin'den eski | Hilt ≥ 2.60 |
| `'kotlin-android' ... built-in Kotlin` | AGP 9'da eski eklenti | `kotlin-android` eklentisini kaldırın |
| `checkDebugAarMetadata` … `compile against version 37.1 or later` | Compose 1.13 (BOM 2026.11.01) compileSdk 37.1 ister | `compileSdk { version = release(37) { minorApiLevel = 1 } }` |
| `Failed to fetch maven artifact … android-all-instrumented … HTTP response code: 429` | Robolectric jar'ı testte Maven Central'dan indirirken hız sınırı | CI jar'ı yansıma + yeniden deneme + önbellekle önceden indirir (yukarıdaki madde 8); test adımı ayrıca bir kez yeniden dener |
| `tasks --all` Compose Preview görevinde hata (AGP 9.5) | AGP 9.5 hatası | `android.onlyEnableUnitTestForTheTestedBuildType=false` (gradle.properties) |

## Geri dönüş
- **Aşama A (ara küme):** agp 8.13.2, kotlin 2.2.21, ksp 2.3.14, hilt 2.58, room 2.8.4, Gradle 8.13, compileSdk 36. (Hilt `hiltViewModel` yeni paketi için `androidx.hilt:hilt-lifecycle-viewmodel-compose` bu kümede de çalışır; `kotlin-android` eklentisi geri eklenmeli.)
- **Son CI'da kanıtlı eski küme:** agp 8.7.3, kotlin 2.0.21, ksp 2.0.21-1.0.28, hilt 2.52, room 2.6.1, Gradle 8.9, compileSdk 35.

## Güvenli yükseltme yöntemi
1. Yalnızca bir grubu değiştirin; ayrı bir dalda push edin.
2. Preflight uyumluluk adımı, 369 test ve imzalı release yeşil olmalı.
3. Telefonda release APK'yı deneyin (açılış, veritabanı, yedek/geri yükleme, bildirim, kasa).
4. Ancak sonra ana dala birleştirin.
