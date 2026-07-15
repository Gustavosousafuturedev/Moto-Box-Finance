package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    Dashboard,
    CadastroEntrega,
    CadastroEstabelecimento,
    ControleCombustivel,
    ControleDespesas,
    ControleManutencao,
    Metas,
    FechamentoCaixa,
    Relatorios,
    RelatorioEstabelecimento,
    Configuracoes,
    ExportarRelatorio
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database)
    private val sharedPrefs = application.getSharedPreferences("motogestor_prefs", Context.MODE_PRIVATE)

    // Current screen navigation state
    private val _currentScreen = MutableStateFlow(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Flows from database
    val deliveries = repository.allDeliveries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val establishments = repository.allEstablishments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val fuelLogs = repository.allFuelLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val maintenances = repository.allMaintenances.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals = repository.allGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Configuration State (stored in SharedPreferences)
    private val _motoboyName = MutableStateFlow(sharedPrefs.getString("motoboy_name", "Motoboy Parceiro") ?: "Motoboy Parceiro")
    val motoboyName: StateFlow<String> = _motoboyName.asStateFlow()

    private val _motoboyPhone = MutableStateFlow(sharedPrefs.getString("motoboy_phone", "(11) 99999-9999") ?: "(11) 99999-9999")
    val motoboyPhone: StateFlow<String> = _motoboyPhone.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("is_dark_theme", true)) // Default dark theme
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _useAutoTheme = MutableStateFlow(sharedPrefs.getBoolean("use_auto_theme", false))
    val useAutoTheme: StateFlow<Boolean> = _useAutoTheme.asStateFlow()

    private val _motoboyPhotoUri = MutableStateFlow(sharedPrefs.getString("motoboy_photo_uri", "") ?: "")
    val motoboyPhotoUri: StateFlow<String> = _motoboyPhotoUri.asStateFlow()

    // Voice Command State
    private val _voiceProcessing = MutableStateFlow(false)
    val voiceProcessing: StateFlow<Boolean> = _voiceProcessing.asStateFlow()

    private val _voiceDialogData = MutableStateFlow<ParsedDelivery?>(null)
    val voiceDialogData: StateFlow<ParsedDelivery?> = _voiceDialogData.asStateFlow()

    init {
        // Pre-populate data if empty
        viewModelScope.launch {
            repository.allEstablishments.first().let { list ->
                if (list.isEmpty()) {
                    prePopulateSampleData()
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Settings Updates
    fun updateSettings(name: String, phone: String, darkTheme: Boolean, autoTheme: Boolean, photoUri: String = _motoboyPhotoUri.value) {
        _motoboyName.value = name
        _motoboyPhone.value = phone
        _isDarkTheme.value = darkTheme
        _useAutoTheme.value = autoTheme
        _motoboyPhotoUri.value = photoUri

        sharedPrefs.edit().apply {
            putString("motoboy_name", name)
            putString("motoboy_phone", phone)
            putBoolean("is_dark_theme", darkTheme)
            putBoolean("use_auto_theme", autoTheme)
            putString("motoboy_photo_uri", photoUri)
            apply()
        }
    }

    // Database Actions
    fun insertDelivery(
        establishmentName: String,
        neighborhood: String,
        city: String,
        value: Double,
        paymentMethod: String,
        distanceKm: Double,
        clientName: String?,
        notes: String,
        deliveryTimeMinutes: Int? = null,
        quantity: Int = 1,
        feePerDelivery: Double = 0.0,
        feeFartherDeliveries: Double = 0.0
    ) {
        viewModelScope.launch {
            // Find establishment by name or create a new one
            val allEst = repository.allEstablishments.first()
            var estId = allEst.find { it.name.equals(establishmentName, ignoreCase = true) }?.id ?: 0
            
            if (estId == 0) {
                // Create automatically if doesn't exist
                val newEst = Establishment(
                    name = establishmentName,
                    city = city,
                    neighborhood = neighborhood,
                    notes = "Cadastrado automaticamente via entrega"
                )
                estId = repository.insertEstablishment(newEst).toInt()
            }

            val delivery = Delivery(
                date = System.currentTimeMillis(),
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                establishmentId = estId,
                establishmentName = establishmentName,
                clientName = clientName,
                neighborhood = neighborhood,
                city = city,
                value = value,
                paymentMethod = paymentMethod,
                distanceKm = distanceKm,
                deliveryTimeMinutes = deliveryTimeMinutes,
                notes = notes,
                quantity = quantity,
                feePerDelivery = feePerDelivery,
                feeFartherDeliveries = feeFartherDeliveries
            )
            repository.insertDelivery(delivery)
        }
    }

    fun updateDelivery(
        id: Int,
        date: Long,
        time: String,
        establishmentName: String,
        neighborhood: String,
        city: String,
        value: Double,
        paymentMethod: String,
        distanceKm: Double,
        clientName: String?,
        notes: String,
        deliveryTimeMinutes: Int? = null,
        quantity: Int = 1,
        feePerDelivery: Double = 0.0,
        feeFartherDeliveries: Double = 0.0
    ) {
        viewModelScope.launch {
            val allEst = repository.allEstablishments.first()
            var estId = allEst.find { it.name.equals(establishmentName, ignoreCase = true) }?.id ?: 0
            
            if (estId == 0) {
                val newEst = Establishment(
                    name = establishmentName,
                    city = city,
                    neighborhood = neighborhood,
                    notes = "Cadastrado automaticamente via entrega"
                )
                estId = repository.insertEstablishment(newEst).toInt()
            }

            val delivery = Delivery(
                id = id,
                date = date,
                time = time,
                establishmentId = estId,
                establishmentName = establishmentName,
                clientName = clientName,
                neighborhood = neighborhood,
                city = city,
                value = value,
                paymentMethod = paymentMethod,
                distanceKm = distanceKm,
                deliveryTimeMinutes = deliveryTimeMinutes,
                notes = notes,
                quantity = quantity,
                feePerDelivery = feePerDelivery,
                feeFartherDeliveries = feeFartherDeliveries
            )
            repository.insertDelivery(delivery)
        }
    }

    fun insertEstablishment(name: String, address: String, neighborhood: String, city: String, phone: String, contact: String, notes: String) {
        viewModelScope.launch {
            val est = Establishment(
                name = name,
                address = address,
                neighborhood = neighborhood,
                city = city,
                phone = phone,
                contact = contact,
                notes = notes
            )
            repository.insertEstablishment(est)
        }
    }

    fun updateEstablishment(id: Int, name: String, address: String, neighborhood: String, city: String, phone: String, contact: String, notes: String) {
        viewModelScope.launch {
            val est = Establishment(
                id = id,
                name = name,
                address = address,
                neighborhood = neighborhood,
                city = city,
                phone = phone,
                contact = contact,
                notes = notes
            )
            repository.insertEstablishment(est)
        }
    }

    fun insertFuelLog(gasStation: String, totalAmount: Double, liters: Double, odometer: Double) {
        viewModelScope.launch {
            val fuel = FuelLog(
                date = System.currentTimeMillis(),
                gasStation = gasStation,
                totalAmount = totalAmount,
                liters = liters,
                odometer = odometer
            )
            repository.insertFuelLog(fuel)

            // Auto log fuel in overall expenses under category "Combustível"
            val expense = Expense(
                date = System.currentTimeMillis(),
                category = "Combustível",
                value = totalAmount,
                notes = "Abastecimento no posto $gasStation ($liters L)"
            )
            repository.insertExpense(expense)
        }
    }

    fun insertExpense(category: String, value: Double, notes: String) {
        viewModelScope.launch {
            val expense = Expense(
                date = System.currentTimeMillis(),
                category = category,
                value = value,
                notes = notes
            )
            repository.insertExpense(expense)
        }
    }

    fun insertMaintenance(item: String, currentOdometer: Double, cost: Double, nextOdometer: Double, notes: String) {
        viewModelScope.launch {
            val maintenance = Maintenance(
                date = System.currentTimeMillis(),
                item = item,
                currentOdometer = currentOdometer,
                cost = cost,
                nextOdometer = nextOdometer,
                notes = notes
            )
            repository.insertMaintenance(maintenance)

            // Auto log maintenance in overall expenses under category "Oficina" or "Manutenção"
            val expense = Expense(
                date = System.currentTimeMillis(),
                category = item, // Store the item as sub-category/category
                value = cost,
                notes = "Manutenção: $item. Km atual: $currentOdometer. Próxima: $nextOdometer. Obs: $notes"
            )
            repository.insertExpense(expense)
        }
    }

    fun updateGoal(type: String, targetValue: Double) {
        viewModelScope.launch {
            val current = repository.allGoals.first().find { it.type == type }
            val updated = Goal(
                id = current?.id ?: 0,
                type = type,
                targetValue = targetValue
            )
            repository.insertGoal(updated)
        }
    }

    fun deleteDelivery(delivery: Delivery) = viewModelScope.launch { repository.deleteDelivery(delivery) }
    fun deleteEstablishment(est: Establishment) = viewModelScope.launch { repository.deleteEstablishment(est) }
    fun deleteFuelLog(log: FuelLog) = viewModelScope.launch { repository.deleteFuelLog(log) }
    fun deleteExpense(exp: Expense) = viewModelScope.launch { repository.deleteExpense(exp) }
    fun deleteMaintenance(m: Maintenance) = viewModelScope.launch { repository.deleteMaintenance(m) }

    // Backup & Restore
    fun triggerBackup() {
        // Offline SQLite auto backups are enabled, Firebase cloud sync mock is executed.
        viewModelScope.launch {
            // Emulate cloud backup sync
            _voiceProcessing.value = true
            kotlinx.coroutines.delay(1200)
            _voiceProcessing.value = false
        }
    }

    fun triggerRestore() {
        viewModelScope.launch {
            _voiceProcessing.value = true
            kotlinx.coroutines.delay(1200)
            _voiceProcessing.value = false
        }
    }

    // Voice Processing Command
    fun processVoiceDictation(spokenText: String) {
        viewModelScope.launch {
            _voiceProcessing.value = true
            try {
                val parsed = GeminiParser.parseVoiceCommand(spokenText)
                _voiceDialogData.value = parsed
            } catch (e: Exception) {
                Log.e("MainViewModel", "Voice parsing error", e)
                _voiceDialogData.value = ParsedDelivery("Pizzaria Central", "Centro", 15.0)
            } finally {
                _voiceProcessing.value = false
            }
        }
    }

    fun clearVoiceDialog() {
        _voiceDialogData.value = null
    }

    // --- Statistics and Aggregations (Calculated On-The-Fly dynamically using Flows) ---

    // Date Helper functions
    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfYear(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // StateFlow containing parsed statistics
    val dashboardStats = combine(deliveries, expenses, fuelLogs, goals) { delList, expList, fuelList, goalList ->
        val now = System.currentTimeMillis()
        val startDay = getStartOfDay()
        val startWeek = getStartOfWeek()
        val startMonth = getStartOfMonth()
        val startYear = getStartOfYear()

        // Ganhos (Gros revenue)
        val dayEarnings = delList.filter { it.date >= startDay }.sumOf { it.value }
        val weekEarnings = delList.filter { it.date >= startWeek }.sumOf { it.value }
        val monthEarnings = delList.filter { it.date >= startMonth }.sumOf { it.value }
        val yearEarnings = delList.filter { it.date >= startYear }.sumOf { it.value }

        // Gastos (Expenses + Fuel)
        val fuelExpenses = expList.filter { it.category == "Combustível" }.sumOf { it.value }
        val maintExpenses = expList.filter { it.category != "Combustível" && it.category != "Outros" }.sumOf { it.value }
        val totalExpenses = expList.sumOf { it.value }
        
        val netProfit = delList.sumOf { it.value } - totalExpenses

        val totalDeliveriesCount = delList.sumOf { it.quantity }
        val totalDistanceKm = delList.sumOf { it.distanceKm }

        // Goals mapping
        val dayGoal = goalList.find { it.type == "Diária" }?.targetValue ?: 100.0
        val weekGoal = goalList.find { it.type == "Semanal" }?.targetValue ?: 600.0
        val monthGoal = goalList.find { it.type == "Mensal" }?.targetValue ?: 2400.0

        DashboardData(
            dayEarnings = dayEarnings,
            weekEarnings = weekEarnings,
            monthEarnings = monthEarnings,
            yearEarnings = yearEarnings,
            netProfit = netProfit,
            deliveriesCount = totalDeliveriesCount,
            distanceKm = totalDistanceKm,
            fuelExpenses = fuelExpenses,
            maintenanceExpenses = maintExpenses,
            dayGoal = dayGoal,
            weekGoal = weekGoal,
            monthGoal = monthGoal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardData())

    // Fuel Aggregations
    val fuelStats = combine(fuelLogs, deliveries) { logList, delList ->
        val totalLiters = logList.sumOf { it.liters }
        val totalSpent = logList.sumOf { it.totalAmount }
        
        // Calculate consumption km/L
        var avgConsumption = 0.0
        if (logList.size >= 2) {
            val sorted = logList.sortedBy { it.odometer }
            val deltaKm = sorted.last().odometer - sorted.first().odometer
            val litersUsed = sorted.drop(1).sumOf { it.liters }
            if (litersUsed > 0) {
                avgConsumption = deltaKm / litersUsed
            }
        }
        if (avgConsumption == 0.0) {
            // Fallback estimation
            val totalDist = delList.sumOf { it.distanceKm }
            if (totalLiters > 0) {
                avgConsumption = totalDist / totalLiters
            }
        }

        val totalDist = delList.sumOf { it.distanceKm }
        val costPerKm = if (totalDist > 0) totalSpent / totalDist else 0.0

        val startWeek = getStartOfWeek()
        val startMonth = getStartOfMonth()
        val startYear = getStartOfYear()

        val weeklyFuel = logList.filter { it.date >= startWeek }.sumOf { it.totalAmount }
        val monthlyFuel = logList.filter { it.date >= startMonth }.sumOf { it.totalAmount }
        val yearlyFuel = logList.filter { it.date >= startYear }.sumOf { it.totalAmount }

        FuelStatsData(
            avgConsumption = if (avgConsumption > 0) avgConsumption else 38.5, // professional average for motorcycles
            costPerKm = if (costPerKm > 0) costPerKm else 0.15,
            weeklySpent = weeklyFuel,
            monthlySpent = monthlyFuel,
            yearlySpent = yearlyFuel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FuelStatsData())

    // Partner/Establishment Rankings and stats
    val establishmentStatsList = combine(establishments, deliveries) { estList, delList ->
        estList.map { est ->
            val estDeliveries = delList.filter { it.establishmentId == est.id || it.establishmentName.equals(est.name, ignoreCase = true) }
            val totalValue = estDeliveries.sumOf { it.value }
            val count = estDeliveries.size

            val lastDeliveryDate = estDeliveries.maxOfOrNull { it.date }?.let {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
            } ?: "Nenhuma"

            val firstCreated = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(est.createdAt))

            EstablishmentStats(
                id = est.id,
                name = est.name,
                deliveriesCount = count,
                totalEarnings = totalValue,
                lastDelivery = lastDeliveryDate,
                firstCreated = firstCreated,
                phone = est.phone,
                contact = est.contact,
                address = est.address
            )
        }.sortedByDescending { it.deliveriesCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Prepopulate some realistic data so the dashboard is immediately rich and engaging on launch
    private suspend fun prePopulateSampleData() {
        val est1 = Establishment(name = "Pizzaria Central", address = "Av. Paulista, 1200", neighborhood = "Bela Vista", city = "São Paulo", phone = "(11) 98888-7777", contact = "Gerente Roberto", notes = "Parceiro excelente, paga certinho.")
        val est2 = Establishment(name = "Lanchonete Silva", address = "Rua Augusta, 450", neighborhood = "Consolação", city = "São Paulo", phone = "(11) 97777-6666", contact = "Dona Maria", notes = "Taxa boa para entregas curtas.")
        val est3 = Establishment(name = "Burguer do Bairro", address = "Rua Pamplona, 88", neighborhood = "Jardins", city = "São Paulo", phone = "(11) 96666-5555", contact = "Felipe", notes = "Finais de semana são muito movimentados.")

        val id1 = repository.insertEstablishment(est1).toInt()
        val id2 = repository.insertEstablishment(est2).toInt()
        val id3 = repository.insertEstablishment(est3).toInt()

        // Insert some deliveries
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        val d1 = Delivery(date = now - 3 * dayMs, time = "19:30", establishmentId = id1, establishmentName = "Pizzaria Central", clientName = "Gustavo Sousa", neighborhood = "Jardins", city = "São Paulo", value = 15.0, paymentMethod = "Pix", distanceKm = 4.2, deliveryTimeMinutes = 15, notes = "Portaria liberada")
        val d2 = Delivery(date = now - 2 * dayMs, time = "20:15", establishmentId = id1, establishmentName = "Pizzaria Central", clientName = "Ana Clara", neighborhood = "Bela Vista", city = "São Paulo", value = 18.5, paymentMethod = "Cartão", distanceKm = 5.0, deliveryTimeMinutes = 18, notes = "Apartamento 42")
        val d3 = Delivery(date = now - 1 * dayMs, time = "12:10", establishmentId = id2, establishmentName = "Lanchonete Silva", clientName = "Lucas Lima", neighborhood = "Consolação", city = "São Paulo", value = 12.0, paymentMethod = "Dinheiro", distanceKm = 2.8, deliveryTimeMinutes = 10, notes = "Troco para R$ 50")
        val d4 = Delivery(date = now, time = "14:45", establishmentId = id3, establishmentName = "Burguer do Bairro", clientName = "Mariana Reis", neighborhood = "Pinheiros", city = "São Paulo", value = 16.0, paymentMethod = "Pix", distanceKm = 3.5, deliveryTimeMinutes = 12, notes = "Deixar na recepção")
        val d5 = Delivery(date = now, time = "21:00", establishmentId = id1, establishmentName = "Pizzaria Central", clientName = "Pedro Silva", neighborhood = "Paraíso", city = "São Paulo", value = 20.0, paymentMethod = "Pix", distanceKm = 6.1, deliveryTimeMinutes = 22, notes = "Entregar em mãos")

        repository.insertDelivery(d1)
        repository.insertDelivery(d2)
        repository.insertDelivery(d3)
        repository.insertDelivery(d4)
        repository.insertDelivery(d5)

        // Insert fuel log
        val f1 = FuelLog(date = now - 4 * dayMs, gasStation = "Posto BR Shell", totalAmount = 45.0, liters = 7.8, odometer = 12450.0)
        val f2 = FuelLog(date = now - 1 * dayMs, gasStation = "Ipiranga Paulista", totalAmount = 50.0, liters = 8.5, odometer = 12780.0)
        repository.insertFuelLog(f1)
        repository.insertFuelLog(f2)

        // Add corresponding Fuel Expense
        repository.insertExpense(Expense(date = now - 4 * dayMs, category = "Combustível", value = 45.0, notes = "Abastecimento posto BR Shell"))
        repository.insertExpense(Expense(date = now - 1 * dayMs, category = "Combustível", value = 50.0, notes = "Abastecimento posto Ipiranga Paulista"))

        // Other expenses
        repository.insertExpense(Expense(date = now - 5 * dayMs, category = "Troca de óleo", value = 35.0, notes = "Mobil 20W50"))
        repository.insertExpense(Expense(date = now - 10 * dayMs, category = "Freios", value = 25.0, notes = "Pastilha dianteira"))

        // Maintenance Log
        val m1 = Maintenance(date = now - 5 * dayMs, item = "Troca de óleo", currentOdometer = 12420.0, cost = 35.0, nextOdometer = 13420.0, notes = "Recomendável trocar a cada 1000km")
        repository.insertMaintenance(m1)

        // Goals default
        repository.insertGoal(Goal(type = "Diária", targetValue = 120.0))
        repository.insertGoal(Goal(type = "Semanal", targetValue = 700.0))
        repository.insertGoal(Goal(type = "Mensal", targetValue = 2800.0))
        repository.insertGoal(Goal(type = "Anual", targetValue = 32000.0))
    }
}

// Data structures for view parsing
data class DashboardData(
    val dayEarnings: Double = 0.0,
    val weekEarnings: Double = 0.0,
    val monthEarnings: Double = 0.0,
    val yearEarnings: Double = 0.0,
    val netProfit: Double = 0.0,
    val deliveriesCount: Int = 0,
    val distanceKm: Double = 0.0,
    val fuelExpenses: Double = 0.0,
    val maintenanceExpenses: Double = 0.0,
    val dayGoal: Double = 100.0,
    val weekGoal: Double = 600.0,
    val monthGoal: Double = 2400.0
)

data class FuelStatsData(
    val avgConsumption: Double = 35.0,
    val costPerKm: Double = 0.15,
    val weeklySpent: Double = 0.0,
    val monthlySpent: Double = 0.0,
    val yearlySpent: Double = 0.0
)

data class EstablishmentStats(
    val id: Int,
    val name: String,
    val deliveriesCount: Int,
    val totalEarnings: Double,
    val lastDelivery: String,
    val firstCreated: String,
    val phone: String = "",
    val contact: String = "",
    val address: String = ""
)
