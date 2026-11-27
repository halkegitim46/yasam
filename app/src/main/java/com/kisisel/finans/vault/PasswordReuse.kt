package com.kisisel.finans.vault

/** Yeni şifrenin mevcut/eski şifrelerle karşılaştırması (yeniden kullanım uyarısı için). Saf işlev. */
object PasswordReuse {
    enum class Result {
        /** Daha önce bu kayıtta kullanılmamış. */
        NONE,

        /** Şu anki şifreyle aynı (değişiklik yok). */
        CURRENT,

        /** Eski şifrelerden biriyle aynı: yeniden kullanım. */
        PREVIOUS
    }

    fun check(candidate: String, current: String?, previous: List<String>): Result = when {
        candidate.isEmpty() -> Result.NONE
        current != null && candidate == current -> Result.CURRENT
        candidate in previous -> Result.PREVIOUS
        else -> Result.NONE
    }
}
