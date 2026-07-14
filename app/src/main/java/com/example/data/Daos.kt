package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MotorcycleDao {
    @Query("SELECT * FROM motorcycles ORDER BY id DESC")
    fun getAllMotorcycles(): Flow<List<Motorcycle>>

    @Query("SELECT * FROM motorcycles WHERE id = :id LIMIT 1")
    suspend fun getMotorcycleById(id: Int): Motorcycle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotorcycle(motorcycle: Motorcycle): Long

    @Update
    suspend fun updateMotorcycle(motorcycle: Motorcycle)

    @Delete
    suspend fun deleteMotorcycle(motorcycle: Motorcycle)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenances WHERE motorcycleId = :motorcycleId ORDER BY dateMillis DESC")
    fun getMaintenancesForMotorcycle(motorcycleId: Int): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenances ORDER BY dateMillis DESC")
    fun getAllMaintenances(): Flow<List<Maintenance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(maintenance: Maintenance): Long

    @Delete
    suspend fun deleteMaintenance(maintenance: Maintenance)
}

@Dao
interface RefuelDao {
    @Query("SELECT * FROM refuels WHERE motorcycleId = :motorcycleId ORDER BY odometer DESC")
    fun getRefuelsForMotorcycle(motorcycleId: Int): Flow<List<Refuel>>

    @Query("SELECT * FROM refuels ORDER BY odometer DESC")
    fun getAllRefuels(): Flow<List<Refuel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefuel(refuel: Refuel): Long

    @Delete
    suspend fun deleteRefuel(refuel: Refuel)
}

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries ORDER BY dateMillis DESC")
    fun getAllDeliveries(): Flow<List<Delivery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: Delivery): Long

    @Update
    suspend fun updateDelivery(delivery: Delivery)

    @Delete
    suspend fun deleteDelivery(delivery: Delivery)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile): Long
}
