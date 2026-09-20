package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partners ORDER BY rating DESC, name ASC")
    fun getAllPartners(): Flow<List<Partner>>

    @Query("SELECT * FROM partners ORDER BY id ASC")
    suspend fun getAllPartnersList(): List<Partner>

    @Query("SELECT * FROM partners WHERE id = :id LIMIT 1")
    suspend fun getPartnerById(id: Int): Partner?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: Partner): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartners(partners: List<Partner>): List<Long>

    @Update
    suspend fun updatePartner(partner: Partner)

    @Delete
    suspend fun deletePartner(partner: Partner)

    @Query("DELETE FROM partners WHERE id = :id")
    suspend fun deletePartnerById(id: Int)
}
