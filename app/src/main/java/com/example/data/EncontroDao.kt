package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EncontroDao {
    @Query("SELECT * FROM encontros ORDER BY date DESC, time DESC")
    fun getAllEncontros(): Flow<List<Encontro>>

    @Query("SELECT * FROM encontros ORDER BY id ASC")
    suspend fun getAllEncontrosList(): List<Encontro>

    @Query("SELECT * FROM encontros WHERE partnerId = :partnerId ORDER BY date DESC, time DESC")
    fun getEncontrosForPartner(partnerId: Int): Flow<List<Encontro>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncontro(encontro: Encontro): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncontros(encontros: List<Encontro>): List<Long>

    @Update
    suspend fun updateEncontro(encontro: Encontro)

    @Delete
    suspend fun deleteEncontro(encontro: Encontro)

    @Query("DELETE FROM encontros WHERE id = :id")
    suspend fun deleteEncontroById(id: Int)

    @Query("DELETE FROM encontros")
    suspend fun clearAllEncontros()

    @Query("DELETE FROM partners")
    suspend fun clearAllPartners()
}
