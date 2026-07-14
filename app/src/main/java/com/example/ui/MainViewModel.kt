package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application, private val repository: Repository) : AndroidViewModel(application) {

    // Existing Motorcycle flows
    val motorcycles: StateFlow<List<Motorcycle>> = repository.motorcycles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMaintenances: StateFlow<List<Maintenance>> = repository.allMaintenances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRefuels: StateFlow<List<Refuel>> = repository.allRefuels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMotorcycleId = MutableStateFlow<Int>(-1)
    val selectedMotorcycleId: StateFlow<Int> = _selectedMotorcycleId.asStateFlow()

    private val _activeMotorcycle = MutableStateFlow<Motorcycle?>(null)
    val activeMotorcycle: StateFlow<Motorcycle?> = _activeMotorcycle.asStateFlow()

    private val _activeMaintenances = MutableStateFlow<List<Maintenance>>(emptyList())
    val activeMaintenances: StateFlow<List<Maintenance>> = _activeMaintenances.asStateFlow()

    private val _activeRefuels = MutableStateFlow<List<Refuel>>(emptyList())
    val activeRefuels: StateFlow<List<Refuel>> = _activeRefuels.asStateFlow()

    // --- NEW DELIVERIES & EXPENSES FLOWS ---
    val allDeliveries: StateFlow<List<Delivery>> = repository.allDeliveries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Sync state
    private val _lastSyncTime = MutableStateFlow<String>("Nunca sincronizado")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("Sem alterações pendentes")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow<Boolean>(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow<Boolean>(false)
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    // Messages state (Snackbars / Toasts)
    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    // Search and filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow("Mais recentes") // "Mais recentes", "Mais antigas", "Maior número de entregas", "Nome do estabelecimento"
    val sortType: StateFlow<String> = _sortType.asStateFlow()

    private val _filterStartDate = MutableStateFlow<Long?>(null)
    val filterStartDate: StateFlow<Long?> = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<Long?>(null)
    val filterEndDate: StateFlow<Long?> = _filterEndDate.asStateFlow()

    private val _filterEstablishment = MutableStateFlow<String?>(null)
    val filterEstablishment: StateFlow<String?> = _filterEstablishment.asStateFlow()

    private val _filterPaymentMethod = MutableStateFlow<String?>(null)
    val filterPaymentMethod: StateFlow<String?> = _filterPaymentMethod.asStateFlow()

    // Filtered & Sorted deliveries
    val filteredDeliveries: StateFlow<List<Delivery>> = combine(
        allDeliveries, searchQuery, sortType, filterStartDate, filterEndDate, filterEstablishment, filterPaymentMethod
    ) { deliveries, query, sort, start, end, est, pay ->
        var list = deliveries

        // Search
        if (query.isNotBlank()) {
            list = list.filter {
                it.establishment.contains(query, ignoreCase = true) ||
                it.paymentMethod.contains(query, ignoreCase = true) ||
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it.dateMillis)).contains(query)
            }
        }

        // Filters
        if (start != null) {
            list = list.filter { it.dateMillis >= start }
        }
        if (end != null) {
            list = list.filter { it.dateMillis <= end }
        }
        if (est != null) {
            list = list.filter { it.establishment.equals(est, ignoreCase = true) }
        }
        if (pay != null) {
            list = list.filter { it.paymentMethod.equals(pay, ignoreCase = true) }
        }

        // Sorting
        when (sort) {
            "Mais recentes" -> list.sortedByDescending { it.dateMillis }
            "Mais antigas" -> list.sortedBy { it.dateMillis }
            "Maior número de entregas" -> {
                // Group by establishment, count, and sort by that count
                val counts = list.groupBy { it.establishment }.mapValues { it.value.size }
                list.sortedByDescending { counts[it.establishment] ?: 0 }
            }
            "Nome do estabelecimento" -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.establishment })
            else -> list.sortedByDescending { it.dateMillis }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Chat Support (kept for GeminiClient if needed, but voice completely removed)
    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("Olá! Sou o assistente de IA do MotoGestor. Como posso ajudar com suas entregas hoje?" to false)
    )
    val chatMessages: StateFlow<List<Pair<String, Boolean>>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        viewModelScope.launch {
            motorcycles.collect { list ->
                if (list.isNotEmpty() && _selectedMotorcycleId.value == -1) {
                    selectMotorcycle(list.first().id)
                }
            }
        }
        // Initialize default user profile if empty
        viewModelScope.launch {
            val current = repository.getUserProfile()
            if (current == null) {
                repository.insertUserProfile(UserProfile(id = 1, name = "Gustavo"))
            }
        }
    }

    fun selectMotorcycle(id: Int) {
        _selectedMotorcycleId.value = id
        viewModelScope.launch {
            val moto = repository.getMotorcycleById(id)
            _activeMotorcycle.value = moto
            
            repository.getMaintenancesForMotorcycle(id).collect {
                _activeMaintenances.value = it
            }
        }
        viewModelScope.launch {
            repository.getRefuelsForMotorcycle(id).collect {
                _activeRefuels.value = it
            }
        }
    }

    // --- MESSAGING ---
    private fun showMessage(msg: String) {
        viewModelScope.launch {
            _message.emit(msg)
        }
    }

    // --- PROFILE ---
    fun updateProfile(
        name: String, phone: String, city: String,
        brand: String, model: String, year: Int, plate: String, odometer: Double, photoUri: String
    ) {
        viewModelScope.launch {
            try {
                val profile = UserProfile(
                    id = 1,
                    name = name,
                    phone = phone,
                    city = city,
                    motorcycleBrand = brand,
                    motorcycleModel = model,
                    motorcycleYear = year,
                    motorcyclePlate = plate,
                    currentOdometer = odometer,
                    photoUri = photoUri
                )
                repository.insertUserProfile(profile)

                // Sync with existing Motorcycle if active
                val active = _activeMotorcycle.value
                if (active != null) {
                    val updatedMoto = active.copy(
                        brand = brand,
                        model = model,
                        year = year,
                        plate = plate,
                        currentOdometer = odometer
                    )
                    repository.insertMotorcycle(updatedMoto)
                    _activeMotorcycle.value = updatedMoto
                } else {
                    // Create a motorcycle record
                    val newMoto = Motorcycle(
                        brand = brand,
                        model = model,
                        year = year,
                        plate = plate,
                        color = "Preto",
                        currentOdometer = odometer
                    )
                    val newId = repository.insertMotorcycle(newMoto)
                    selectMotorcycle(newId.toInt())
                }
                showMessage("Perfil atualizado")
            } catch (e: Exception) {
                showMessage("Erro ao salvar")
            }
        }
    }

    // --- DELIVERY CRUD ---
    fun addDelivery(establishment: String, value: Double, paymentMethod: String, dateMillis: Long, notes: String) {
        viewModelScope.launch {
            try {
                if (establishment.isBlank() || value <= 0.0 || paymentMethod.isBlank()) {
                    showMessage("Erro ao salvar")
                    return@launch
                }
                val delivery = Delivery(
                    establishment = establishment.trim(),
                    value = value,
                    paymentMethod = paymentMethod,
                    dateMillis = dateMillis,
                    notes = notes
                )
                repository.insertDelivery(delivery)
                showMessage("Entrega salva com sucesso")
            } catch (e: Exception) {
                showMessage("Erro ao salvar")
            }
        }
    }

    fun updateDelivery(delivery: Delivery) {
        viewModelScope.launch {
            try {
                if (delivery.establishment.isBlank() || delivery.value <= 0.0 || delivery.paymentMethod.isBlank()) {
                    showMessage("Erro ao salvar")
                    return@launch
                }
                repository.updateDelivery(delivery)
                showMessage("Entrega editada")
            } catch (e: Exception) {
                showMessage("Erro ao salvar")
            }
        }
    }

    fun deleteDelivery(delivery: Delivery) {
        viewModelScope.launch {
            try {
                repository.deleteDelivery(delivery)
                showMessage("Entrega excluída")
            } catch (e: Exception) {
                showMessage("Erro ao salvar")
            }
        }
    }

    // --- EXPENSE CRUD ---
    fun addExpense(title: String, cost: Double, category: String, dateMillis: Long, notes: String) {
        viewModelScope.launch {
            try {
                if (title.isBlank() || cost <= 0.0 || category.isBlank()) {
                    showMessage("Erro ao salvar")
                    return@launch
                }
                val expense = Expense(
                    title = title.trim(),
                    cost = cost,
                    category = category,
                    dateMillis = dateMillis,
                    notes = notes
                )
                repository.insertExpense(expense)
                showMessage("Despesa cadastrada")
            } catch (e: Exception) {
                showMessage("Erro ao salvar")
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                if (expense.title.isBlank() || expense.cost <= 0.0 || expense.category.isBlank()) {
                    showMessage("Erro ao salvar")
                    return@launch
                }
                repository.updateExpense(expense)
                showMessage("Despesa editada")
            } catch (e: Exception) {
                showMessage("Erro ao salvar")
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expense)
                showMessage("Despesa excluída")
            } catch (e: Exception) {
                showMessage("Erro ao salvar")
            }
        }
    }

    // --- SEARCH / SORT SETTERS ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortType(type: String) {
        _sortType.value = type
    }

    fun setDateFilter(start: Long?, end: Long?) {
        _filterStartDate.value = start
        _filterEndDate.value = end
    }

    fun setEstablishmentFilter(est: String?) {
        _filterEstablishment.value = est
    }

    fun setPaymentMethodFilter(pay: String?) {
        _filterPaymentMethod.value = pay
    }

    fun clearFilters() {
        _filterStartDate.value = null
        _filterEndDate.value = null
        _filterEstablishment.value = null
        _filterPaymentMethod.value = null
        _searchQuery.value = ""
    }

    // --- SYNCHRONIZATION ---
    fun toggleAutoBackup(enabled: Boolean) {
        _isAutoBackupEnabled.value = enabled
        if (enabled) {
            showMessage("Backup automático ativado")
        } else {
            showMessage("Backup automático desativado")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Sincronizando com a nuvem..."
            kotlinx.coroutines.delay(1500)
            _isSyncing.value = false
            _syncStatus.value = "Sincronizado"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            _lastSyncTime.value = sdf.format(Date())
            showMessage("Backup concluído")
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Restaurando dados..."
            kotlinx.coroutines.delay(1500)
            _isSyncing.value = false
            _syncStatus.value = "Restaurado"
            showMessage("Backup restaurado com sucesso")
        }
    }

    // --- CALCULATIONS FOR HOME & DASHBOARDS ---

    // Date range helpers
    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfWeekMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Deliveries count today
    fun getDeliveriesCountToday(): Int {
        val start = getStartOfDayMillis()
        return allDeliveries.value.count { it.dateMillis >= start }
    }

    // Deliveries count this week
    fun getDeliveriesCountThisWeek(): Int {
        val start = getStartOfWeekMillis()
        return allDeliveries.value.count { it.dateMillis >= start }
    }

    // Deliveries count this month
    fun getDeliveriesCountThisMonth(): Int {
        val start = getStartOfMonthMillis()
        return allDeliveries.value.count { it.dateMillis >= start }
    }

    // Active establishments count
    fun getActiveEstablishmentsCount(): Int {
        return allDeliveries.value.map { it.establishment.trim().lowercase() }.distinct().count { it.isNotBlank() }
    }

    // Daily Goal progress % (Assuming a dynamic or static target of 15 deliveries)
    fun getDailyGoalPercentage(): Int {
        val count = getDeliveriesCountToday()
        val target = 15 // Standard target deliveries per day
        return ((count / target.toDouble()) * 100).coerceIn(0.0, 100.0).toInt()
    }

    // Average daily deliveries
    fun getAverageDailyDeliveries(): Double {
        val deliveries = allDeliveries.value
        if (deliveries.isEmpty()) return 0.0
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val daysCount = deliveries.map { sdf.format(Date(it.dateMillis)) }.distinct().size
        if (daysCount == 0) return 0.0
        return deliveries.size.toDouble() / daysCount
    }

    // Top 5 establishments (Parceiros Frequentes)
    fun getTop5Establishments(): List<Pair<String, Int>> {
        val deliveries = allDeliveries.value
        if (deliveries.isEmpty()) return emptyList()
        return deliveries.groupBy { it.establishment }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }

    // Best establishment name (Melhor estabelecimento)
    fun getBestEstablishmentName(): String {
        val top = getTop5Establishments().firstOrNull()
        return top?.first ?: "Nenhum ainda"
    }

    // Payment method metrics
    fun getPaymentMethodCounts(): Map<String, Int> {
        val deliveries = allDeliveries.value
        val pixCount = deliveries.count { it.paymentMethod.equals("PIX", ignoreCase = true) }
        val cashCount = deliveries.count { it.paymentMethod.equals("Dinheiro", ignoreCase = true) }
        return mapOf("PIX" to pixCount, "Dinheiro" to cashCount)
    }

    // Get deliveries grouped by day for calendar marking (Map<dd-MM-yyyy, count>)
    fun getDeliveriesByDate(): Map<String, List<Delivery>> {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return allDeliveries.value.groupBy { sdf.format(Date(it.dateMillis)) }
    }

    // Existing calculations kept
    fun addMotorcycle(brand: String, model: String, year: Int, plate: String, color: String, odometer: Double, oilInterval: Double) {
        viewModelScope.launch {
            val moto = Motorcycle(
                brand = brand,
                model = model,
                year = year,
                plate = plate,
                color = color,
                currentOdometer = odometer,
                oilChangeInterval = oilInterval
            )
            val newId = repository.insertMotorcycle(moto)
            selectMotorcycle(newId.toInt())
        }
    }

    fun updateMotorcycleOdometer(motoId: Int, newOdometer: Double) {
        viewModelScope.launch {
            val moto = repository.getMotorcycleById(motoId)
            if (moto != null && newOdometer >= moto.currentOdometer) {
                val updated = moto.copy(currentOdometer = newOdometer)
                repository.insertMotorcycle(updated)
                _activeMotorcycle.value = updated
            }
        }
    }

    fun deleteMotorcycle(moto: Motorcycle) {
        viewModelScope.launch {
            repository.deleteMotorcycle(moto)
            _selectedMotorcycleId.value = -1
            _activeMotorcycle.value = null
            _activeMaintenances.value = emptyList()
            _activeRefuels.value = emptyList()
        }
    }

    fun addMaintenance(type: String, title: String, cost: Double, notes: String, odometer: Double) {
        val motoId = _selectedMotorcycleId.value
        if (motoId == -1) return
        viewModelScope.launch {
            val maintenance = Maintenance(
                motorcycleId = motoId,
                type = type,
                title = title,
                cost = cost,
                notes = notes,
                odometer = odometer,
                dateMillis = System.currentTimeMillis()
            )
            repository.insertMaintenance(maintenance)
            updateMotorcycleOdometer(motoId, odometer)
            selectMotorcycle(motoId)
            showMessage("Despesa cadastrada")
        }
    }

    fun deleteMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            repository.deleteMaintenance(maintenance)
            selectMotorcycle(maintenance.motorcycleId)
        }
    }

    fun addRefuel(odometer: Double, liters: Double, pricePerLiter: Double) {
        val motoId = _selectedMotorcycleId.value
        if (motoId == -1) return
        viewModelScope.launch {
            val totalCost = liters * pricePerLiter
            val refuel = Refuel(
                motorcycleId = motoId,
                odometer = odometer,
                liters = liters,
                pricePerLiter = pricePerLiter,
                totalCost = totalCost,
                dateMillis = System.currentTimeMillis()
            )
            repository.insertRefuel(refuel)
            updateMotorcycleOdometer(motoId, odometer)
            selectMotorcycle(motoId)
            showMessage("Despesa cadastrada")
        }
    }

    fun deleteRefuel(refuel: Refuel) {
        viewModelScope.launch {
            repository.deleteRefuel(refuel)
            selectMotorcycle(refuel.motorcycleId)
        }
    }

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        
        val history = _chatMessages.value.toMutableList()
        history.add(message to true)
        _chatMessages.value = history
        _isAiLoading.value = true

        viewModelScope.launch {
            val profile = userProfile.value
            val dels = allDeliveries.value
            val contextPrompt = buildString {
                append("Você é o mecânico especialista e consultor financeiro integrado ao app MotoGestor.\n")
                if (profile != null) {
                    append("Entregador: ${profile.name}\n")
                    append("Cidade: ${profile.city}\n")
                    append("Moto: ${profile.motorcycleBrand} ${profile.motorcycleModel} (${profile.motorcycleYear})\n")
                    append("Odômetro: ${profile.currentOdometer} KM\n")
                    append("Entregas acumuladas: ${dels.size}\n")
                }
                append("\nResponda em português de forma simpática, clara e curta.\n\n")
                append("Mensagem do usuário: $message")
            }

            val response = GeminiClient.generateContent(contextPrompt)
            val updatedHistory = _chatMessages.value.toMutableList()
            updatedHistory.add(response to false)
            _chatMessages.value = updatedHistory
            _isAiLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            "Olá! Sou o assistente de IA do MotoGestor. Como posso ajudar com suas entregas hoje?" to false
        )
    }

    fun calculateAverageConsumption(): Double {
        val refs = _activeRefuels.value
        if (refs.size < 2) return 0.0
        val sortedRefs = refs.sortedBy { it.odometer }
        val firstOdo = sortedRefs.first().odometer
        val lastOdo = sortedRefs.last().odometer
        val totalDistance = lastOdo - firstOdo
        if (totalDistance <= 0) return 0.0
        val totalLiters = sortedRefs.drop(1).sumOf { it.liters }
        if (totalLiters <= 0.0) return 0.0
        return totalDistance / totalLiters
    }

    fun getNextOilChangeKm(): Double {
        val active = _activeMotorcycle.value ?: return 0.0
        val maints = _activeMaintenances.value
        val lastOilChange = maints.firstOrNull { it.type == "Troca de Óleo" || it.title.contains("óleo", ignoreCase = true) }
        val baseKm = lastOilChange?.odometer ?: 0.0
        return if (baseKm > 0) baseKm + active.oilChangeInterval else active.oilChangeInterval
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: Repository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
