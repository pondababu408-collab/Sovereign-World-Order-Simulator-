package com.example.data

import android.content.Context
import androidx.room.*
import com.example.model.GameState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Entity(tableName = "save_slots")
data class SaveSlotEntity(
    @PrimaryKey val slotId: Int, // 1..5
    val campaignName: String,
    val countryName: String,
    val dateFormatted: String,
    val playerLevel: Int,
    val netWorthUsd: Double,
    val savedAtMillis: Long,
    val stateJson: String
)

data class SaveSlotSummary(
    val slotId: Int,
    val isEmpty: Boolean,
    val campaignName: String = "",
    val countryName: String = "",
    val dateFormatted: String = "",
    val playerLevel: Int = 1,
    val netWorthUsd: Double = 0.0,
    val savedAtMillis: Long = 0L
)

@Dao
interface SaveSlotDao {
    @Query("SELECT * FROM save_slots ORDER BY slotId ASC")
    fun observeAllSlots(): Flow<List<SaveSlotEntity>>

    @Query("SELECT * FROM save_slots WHERE slotId = :slotId LIMIT 1")
    suspend fun getSlot(slotId: Int): SaveSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlot(slot: SaveSlotEntity)

    @Query("DELETE FROM save_slots WHERE slotId = :slotId")
    suspend fun deleteSlot(slotId: Int)
}

@Database(entities = [SaveSlotEntity::class], version = 1, exportSchema = false)
abstract class WorldOrderDatabase : RoomDatabase() {
    abstract fun saveSlotDao(): SaveSlotDao

    companion object {
        @Volatile
        private var INSTANCE: WorldOrderDatabase? = null

        fun getInstance(context: Context): WorldOrderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorldOrderDatabase::class.java,
                    "world_order_offline.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class SaveGameRepository(private val dao: SaveSlotDao) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(GameState::class.java)

    val allSlotsFlow: Flow<List<SaveSlotEntity>> = dao.observeAllSlots()

    suspend fun saveToSlot(slotId: Int, state: GameState) = withContext(Dispatchers.IO) {
        val country = state.countries.firstOrNull { it.iso == state.player.countryIso }
        val portfolioVal = state.stocks.sumOf { s -> s.playerShares * s.currentPriceUsd }
        val bizVal = state.companies.filter { it.ownerId == state.player.id }.sumOf { it.valuationUsd }
        val btcVal = state.player.bitcoinBalance * (state.stocks.firstOrNull { it.ticker == "BTC" }?.currentPriceUsd ?: 94200.0)
        val totalNetWorth = state.player.cashUsd + portfolioVal + bizVal + btcVal
        val json = adapter.toJson(state)
        val entity = SaveSlotEntity(
            slotId = slotId.coerceIn(1, 5),
            campaignName = "${state.player.name} (${state.startingMode.title})",
            countryName = country?.name ?: state.player.countryIso,
            dateFormatted = state.date.formatted(),
            playerLevel = state.player.level,
            netWorthUsd = totalNetWorth,
            savedAtMillis = System.currentTimeMillis(),
            stateJson = json
        )
        dao.upsertSlot(entity)
    }

    suspend fun loadFromSlot(slotId: Int): GameState? = withContext(Dispatchers.IO) {
        val entity = dao.getSlot(slotId) ?: return@withContext null
        runCatching { adapter.fromJson(entity.stateJson) }.getOrNull()
    }

    suspend fun deleteSlot(slotId: Int) = withContext(Dispatchers.IO) {
        dao.deleteSlot(slotId)
    }
}
