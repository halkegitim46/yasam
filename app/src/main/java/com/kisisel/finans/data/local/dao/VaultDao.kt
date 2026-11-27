package com.kisisel.finans.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kisisel.finans.data.local.entity.VaultHistoryEntity
import com.kisisel.finans.data.local.entity.VaultItemEntity
import com.kisisel.finans.data.local.entity.VaultSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Insert
    suspend fun insert(item: VaultItemEntity): Long

    @Update
    suspend fun update(item: VaultItemEntity)

    /** Arşivlenmiş kayıt dönmez. */
    @Query("SELECT * FROM vault_items WHERE id = :id AND isArchived = 0")
    suspend fun getById(id: Long): VaultItemEntity?

    /** Tek kayıt akışı (düzenleme/arşivleme sonrası ayrıntı ekranı güncellenir); arşivlenince null yayar. */
    @Query("SELECT * FROM vault_items WHERE id = :id AND isArchived = 0")
    fun observeById(id: Long): Flow<VaultItemEntity?>

    /** Şifre alanı olmadan özet liste. */
    @Query("SELECT id, title, username, url FROM vault_items WHERE isArchived = 0 ORDER BY title COLLATE NOCASE, id")
    fun observeSummaries(): Flow<List<VaultSummaryRow>>

    @Insert
    suspend fun insertHistory(entry: VaultHistoryEntity): Long

    /** Eski şifreler, en yeni değişiklik önce. */
    @Query("SELECT * FROM vault_history WHERE itemId = :itemId ORDER BY changedAt DESC, id DESC")
    fun observeHistory(itemId: Long): Flow<List<VaultHistoryEntity>>

    @Query("SELECT * FROM vault_history WHERE itemId = :itemId ORDER BY changedAt DESC, id DESC")
    suspend fun getHistory(itemId: Long): List<VaultHistoryEntity>

    /** Kayıt başına yalnızca en yeni [keep] eski şifre kalır. */
    @Query(
        "DELETE FROM vault_history WHERE itemId = :itemId AND id NOT IN " +
            "(SELECT id FROM vault_history WHERE itemId = :itemId ORDER BY changedAt DESC, id DESC LIMIT :keep)"
    )
    suspend fun pruneHistory(itemId: Long, keep: Int)

    @Query("UPDATE vault_items SET isArchived = 1, updatedAt = :now WHERE id = :id AND isArchived = 0")
    suspend fun archive(id: Long, now: Long): Int
}
