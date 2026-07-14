package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val database: AppDatabase) {
    val motorcycles: Flow<List<Motorcycle>> = database.motorcycleDao().getAllMotorcycles()
    val allMaintenances: Flow<List<Maintenance>> = database.maintenanceDao().getAllMaintenances()
    val allRefuels: Flow<List<Refuel>> = database.refuelDao().getAllRefuels()
    val allDeliveries: Flow<List<Delivery>> = database.deliveryDao().getAllDeliveries()
    val allExpenses: Flow<List<Expense>> = database.expenseDao().getAllExpenses()

    // Deliveries
    suspend fun insertDelivery(delivery: Delivery): Long = database.deliveryDao().insertDelivery(delivery)
    suspend fun updateDelivery(delivery: Delivery) = database.deliveryDao().updateDelivery(delivery)
    suspend fun deleteDelivery(delivery: Delivery) = database.deliveryDao().deleteDelivery(delivery)

    // Expenses
    suspend fun insertExpense(expense: Expense): Long = database.expenseDao().insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = database.expenseDao().updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = database.expenseDao().deleteExpense(expense)

    // UserProfile
    fun getUserProfileFlow(): Flow<UserProfile?> = database.userProfileDao().getUserProfileFlow()
    suspend fun getUserProfile(): UserProfile? = database.userProfileDao().getUserProfile()
    suspend fun insertUserProfile(profile: UserProfile): Long = database.userProfileDao().insertUserProfile(profile)

    suspend fun getMotorcycleById(id: Int): Motorcycle? {
        return database.motorcycleDao().getMotorcycleById(id)
    }

    suspend fun insertMotorcycle(motorcycle: Motorcycle): Long {
        return database.motorcycleDao().insertMotorcycle(motorcycle)
    }

    suspend fun updateMotorcycle(motorcycle: Motorcycle) {
        database.motorcycleDao().updateMotorcycle(motorcycle)
    }

    suspend fun deleteMotorcycle(motorcycle: Motorcycle) {
        database.motorcycleDao().deleteMotorcycle(motorcycle)
    }

    fun getMaintenancesForMotorcycle(motorcycleId: Int): Flow<List<Maintenance>> {
        return database.maintenanceDao().getMaintenancesForMotorcycle(motorcycleId)
    }

    suspend fun insertMaintenance(maintenance: Maintenance): Long {
        return database.maintenanceDao().insertMaintenance(maintenance)
    }

    suspend fun deleteMaintenance(maintenance: Maintenance) {
        database.maintenanceDao().deleteMaintenance(maintenance)
    }

    fun getRefuelsForMotorcycle(motorcycleId: Int): Flow<List<Refuel>> {
        return database.refuelDao().getRefuelsForMotorcycle(motorcycleId)
    }

    suspend fun insertRefuel(refuel: Refuel): Long {
        return database.refuelDao().insertRefuel(refuel)
    }

    suspend fun deleteRefuel(refuel: Refuel) {
        database.refuelDao().deleteRefuel(refuel)
    }
}
