package com.kisisel.finans.domain.repository

import com.kisisel.finans.domain.model.NewVaultItem
import com.kisisel.finans.domain.model.VaultItem
import com.kisisel.finans.domain.model.VaultSecretHistory
import com.kisisel.finans.domain.model.VaultSummary
import com.kisisel.finans.domain.model.VaultUpdate
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    suspend fun create(cmd: NewVaultItem): Long
    /** Şifre değiştiyse önceki şifre geçmişe alınır (kayıt başına en çok 3); tek işlemde, atomik. */
    suspend fun update(cmd: VaultUpdate)

    /** Silme yok: arşivlenir, listelerden kalkar (kayıt yedekte korunur). */
    suspend fun archive(id: Long)

    /** Tam kayıt (şifre dahil). Arşivlenmişse null. */
    suspend fun getById(id: Long): VaultItem?

    /** Tek kaydın akışı (şifre dahil); arşivlenince veya yoksa null yayar. */
    fun observeById(id: Long): Flow<VaultItem?>

    /** Eski şifreler, en yeni değişiklik önce (en çok 3). */
    fun observeHistory(itemId: Long): Flow<List<VaultSecretHistory>>

    suspend fun getHistory(itemId: Long): List<VaultSecretHistory>

    /** Başlığa göre sıralı özetler; şifre içermez. */
    fun observeSummaries(): Flow<List<VaultSummary>>
}
