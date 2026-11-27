package com.kisisel.finans.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Şifre kasası kaydı. Veritabanı SQLCipher ile (AES-256) şifrelidir; kayıtlar ayrıca uygulama kilidi ve
 * kasa açılırken yeniden doğrulama arkasındadır. Silinmez, arşivlenir (arşivlenen kayıt yedekte kalır).
 */
@Entity(tableName = "vault_items", indices = [Index(value = ["isArchived"])])
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val username: String = "",
    val secret: String,
    val url: String = "",
    val note: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

/** Liste görünümü için: şifre alanı YOK (gizli veri gereksiz yere belleğe/arayüze taşınmaz). */
data class VaultSummaryRow(val id: Long, val title: String, val username: String, val url: String)

/**
 * Bir kasa kaydının ESKİ şifresi. Şifre her değiştiğinde önceki değer buraya alınır; kayıt başına en çok
 * `VaultHistoryPolicy.MAX_PER_ITEM` kayıt tutulur (fazlası silinir). Veritabanı SQLCipher ile şifrelidir.
 */
@Entity(
    tableName = "vault_history",
    foreignKeys = [
        ForeignKey(
            entity = VaultItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class VaultHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val secret: String,
    /** Bu şifrenin DEĞİŞTİRİLDİĞİ an (epoch ms): yerini yenisine bıraktığı zaman. */
    val changedAt: Long
) {
    // Şifre yanlışlıkla log'a düşmesin.
    override fun toString(): String = "VaultHistoryEntity(id=$id, itemId=$itemId, secret=***)"
}
