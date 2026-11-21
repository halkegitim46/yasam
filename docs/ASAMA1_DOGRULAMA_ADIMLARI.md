# Aşama 1 – Windows üzerinde doğrulama

## 1. Ön koşullar

- Android Studio'nun güncel kararlı sürümü
- JDK 17 (Gradle JDK olarak seçili)
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0

Bu sürüm seti bilinçli olarak AGP 8.13.2 + compileSdk 36 üzerinde tutulmuştur. API 37 isteyen AndroidX sürümlerini ayrıca yükseltmeyin; bunlar AGP 9.2+ geçişi gerektirir.
- İnternet bağlantısı (ilk Gradle senkronizasyonunda bağımlılıklar indirilecek)

## 2. Gradle Wrapper ve Room şemalarını üretme

En kolay yol, projeyi GitHub'a gönderip **Actions → Proje dosyalarını tamamla (Gradle wrapper + Room şemaları) → Run workflow** adımlarını çalıştırmaktır. Workflow `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` ve `app/schemas/**` dosyalarını üretir. Sonuçta oluşan commit'i çekin.

Yerel Gradle kurulumu mevcutsa proje kökünde PowerShell ile: `gradle wrapper --gradle-version 8.14.8 --distribution-type bin` çalıştırılabilir. Ardından wrapper dağıtım SHA-256 değerinin `gradle/wrapper/gradle-wrapper.properties` içinde bulunduğunu kontrol edin.

## 3. PowerShell doğrulama komutları

Proje kök klasöründe çalıştırın:

```powershell
.\gradlew.bat --version
.\gradlew.bat help --stacktrace
.\gradlew.bat assembleDebug --stacktrace
.\gradlew.bat testDebugUnitTest --stacktrace
.\gradlew.bat lintDebug --stacktrace
```

Emülatör veya cihaz bağlıyken ayrıca:

```powershell
.\gradlew.bat connectedDebugAndroidTest --stacktrace
```

## 4. Başarı ölçütleri

- Gradle sürümü 8.14.8 ve JVM 17 görünmeli.
- `assembleDebug`, `testDebugUnitTest` ve `lintDebug` başarılı olmalı.
- `app/schemas/com.kisisel.finans.data.local.KisiselFinansDatabase/3.json` (Room'un gerçek ürettiği klasör adı esas alınır) oluşmalı ve sürüm kontrolüne alınmalı.
- Emülatör testlerinde SQLCipher gerçek şifreli veritabanı ve Android Keystore testleri geçmeli.
- Testler tamamlanmadan release APK yayımlanmamalı.

## 5. Hata raporu

Herhangi bir komut başarısız olursa hata çıktısının tamamını, özellikle `What went wrong`, `Caused by` ve ilk `e:` satırlarını paylaşın. Bir sonraki düzeltme yalnızca gerçek hata mesajına göre yapılmalıdır; tahmini bağımlılık değişikliği yapılmamalıdır.
