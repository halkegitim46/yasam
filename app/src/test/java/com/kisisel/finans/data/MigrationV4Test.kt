package com.kisisel.finans.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kisisel.finans.data.local.KisiselFinansDatabase
import com.kisisel.finans.data.local.Migrations
import com.kisisel.finans.data.local.entity.VaultHistoryEntity
import com.kisisel.finans.data.local.entity.VaultItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sürüm 3 → 4 (kasa şifre geçmişi) yükseltmesinin gerçek sınaması: sürüm 3 görünümü, güncel Room dosyasından `vault_history`
 * silinip `user_version = 3` yapılarak elde edilir; yükseltmeden sonra Room tüm şemayı entity tanımlarına göre doğrular.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MigrationV4Test {
    private lateinit var context: Context
    private val name = "migration-v4-test.db"

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(name)
    }

    @After fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun createVersion3File(): Long {
        val db = Room.databaseBuilder(context, KisiselFinansDatabase::class.java, name).allowMainThreadQueries().build()
        val itemId = runBlocking {
            db.vaultDao().insert(VaultItemEntity(title = "Banka", secret = "guncel", createdAt = 1, updatedAt = 1))
        }
        db.close()
        val raw = SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE)
        raw.execSQL("DROP TABLE vault_history")
        raw.execSQL("PRAGMA user_version = 3")
        raw.close()
        return itemId
    }

    private fun upgraded(): KisiselFinansDatabase =
        Room.databaseBuilder(context, KisiselFinansDatabase::class.java, name)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()

    @Test fun upgradeCreatesHistoryTableMatchingEntityAndKeepsVaultData() {
        val itemId = createVersion3File()
        val db = upgraded()
        try {
            assertEquals("guncel", runBlocking { db.vaultDao().getById(itemId)?.secret })
            val historyId = runBlocking { db.vaultDao().insertHistory(VaultHistoryEntity(itemId = itemId, secret = "eski", changedAt = 5)) }
            val rows = runBlocking { db.vaultDao().getHistory(itemId) }
            assertEquals(listOf(historyId), rows.map { it.id })
            assertEquals("eski", rows.single().secret)
        } finally {
            db.close()
        }
    }

    @Test fun foreignKeyAndIndexCreatedByMigrationWork() {
        createVersion3File()
        val db = upgraded()
        try {
            val failed = runCatching {
                runBlocking { db.vaultDao().insertHistory(VaultHistoryEntity(itemId = 9_999, secret = "x", changedAt = 1)) }
            }.isFailure
            assertTrue("Olmayan kayda bağlı geçmiş yabancı anahtarla reddedilmeli", failed)

            val names = mutableListOf<String>()
            db.openHelper.readableDatabase.query("PRAGMA index_list(vault_history)").use { c ->
                val col = c.getColumnIndexOrThrow("name")
                while (c.moveToNext()) names += c.getString(col)
            }
            assertTrue(names.toString(), names.contains("index_vault_history_itemId"))
        } finally {
            db.close()
        }
    }

    @Test fun missingMigrationNeverDestroysData() {
        createVersion3File()
        val bad = Room.databaseBuilder(context, KisiselFinansDatabase::class.java, name).allowMainThreadQueries().build()
        val failed = runCatching { bad.openHelper.writableDatabase }.isFailure
        runCatching { bad.close() }
        assertTrue("Migration yoksa yıkıcı geçiş yapılmamalı", failed)

        val ok = upgraded()
        try {
            assertEquals(1, runBlocking { ok.snapshotDao().vaultItems().size })
        } finally {
            ok.close()
        }
    }
}
