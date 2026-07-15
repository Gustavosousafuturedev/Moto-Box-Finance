package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {
    private val deliveryDao = database.deliveryDao()
    private val establishmentDao = database.establishmentDao()
    private val fuelLogDao = database.fuelLogDao()
    private val expenseDao = database.expenseDao()
    private val maintenanceDao = database.maintenanceDao()
    private val goalDao = database.goalDao()

    // Deliveries
    val allDeliveries: Flow<List<Delivery>> = deliveryDao.getAllDeliveries()
    val distinctEstablishments: Flow<List<String>> = deliveryDao.getDistinctEstablishments()
    
    fun getDeliveriesByEstablishment(estId: Int): Flow<List<Delivery>> {
        return deliveryDao.getDeliveriesByEstablishment(estId)
    }

    suspend fun insertDelivery(delivery: Delivery): Long {
        return deliveryDao.insertDelivery(delivery)
    }

    suspend fun deleteDelivery(delivery: Delivery) {
        deliveryDao.deleteDelivery(delivery)
    }

    suspend fun deleteDeliveryById(id: Int) {
        deliveryDao.deleteDeliveryById(id)
    }

    // Establishments
    val allEstablishments: Flow<List<Establishment>> = establishmentDao.getAllEstablishments()

    suspend fun insertEstablishment(establishment: Establishment): Long {
        return establishmentDao.insertEstablishment(establishment)
    }

    suspend fun deleteEstablishment(establishment: Establishment) {
        establishmentDao.deleteEstablishment(establishment)
    }

    suspend fun deleteEstablishmentById(id: Int) {
        establishmentDao.deleteEstablishmentById(id)
    }

    // Fuel Logs
    val allFuelLogs: Flow<List<FuelLog>> = fuelLogDao.getAllFuelLogs()

    suspend fun insertFuelLog(fuelLog: FuelLog): Long {
        return fuelLogDao.insertFuelLog(fuelLog)
    }

    suspend fun deleteFuelLog(fuelLog: FuelLog) {
        fuelLogDao.deleteFuelLog(fuelLog)
    }

    // Expenses
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    // Maintenances
    val allMaintenances: Flow<List<Maintenance>> = maintenanceDao.getAllMaintenances()

    suspend fun insertMaintenance(maintenance: Maintenance): Long {
        return maintenanceDao.insertMaintenance(maintenance)
    }

    suspend fun deleteMaintenance(maintenance: Maintenance) {
        maintenanceDao.deleteMaintenance(maintenance)
    }

    // Goals
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

    fun getGoalByType(type: String): Flow<Goal?> {
        return goalDao.getGoalByType(type)
    }

    suspend fun insertGoal(goal: Goal): Long {
        return goalDao.insertGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }
}
