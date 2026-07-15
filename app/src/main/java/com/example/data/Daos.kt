package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries ORDER BY date DESC, id DESC")
    fun getAllDeliveries(): Flow<List<Delivery>>

    @Query("SELECT * FROM deliveries WHERE establishmentId = :estId ORDER BY date DESC")
    fun getDeliveriesByEstablishment(estId: Int): Flow<List<Delivery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: Delivery): Long

    @Delete
    suspend fun deleteDelivery(delivery: Delivery)

    @Query("DELETE FROM deliveries WHERE id = :id")
    suspend fun deleteDeliveryById(id: Int)
}

@Dao
interface EstablishmentDao {
    @Query("SELECT * FROM establishments ORDER BY name ASC")
    fun getAllEstablishments(): Flow<List<Establishment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstablishment(establishment: Establishment): Long

    @Delete
    suspend fun deleteEstablishment(establishment: Establishment)

    @Query("DELETE FROM establishments WHERE id = :id")
    suspend fun deleteEstablishmentById(id: Int)
}

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs ORDER BY date DESC")
    fun getAllFuelLogs(): Flow<List<FuelLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(fuelLog: FuelLog): Long

    @Delete
    suspend fun deleteFuelLog(fuelLog: FuelLog)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenances ORDER BY date DESC")
    fun getAllMaintenances(): Flow<List<Maintenance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(maintenance: Maintenance): Long

    @Delete
    suspend fun deleteMaintenance(maintenance: Maintenance)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE type = :type LIMIT 1")
    fun getGoalByType(type: String): Flow<Goal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Delete
    suspend fun deleteGoal(goal: Goal)
}
