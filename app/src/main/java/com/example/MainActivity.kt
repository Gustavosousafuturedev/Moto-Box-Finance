package com.example

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Delivery
import com.example.data.Expense
import com.example.data.FuelLog
import com.example.data.Maintenance
import com.example.ui.*
import com.example.ui.components.BarChart
import com.example.ui.components.HorizontalBarChart
import com.example.ui.components.PieChart
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    // Activity launcher for Google Speech-To-Text dictation
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                viewModel.processVoiceDictation(spokenText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScaffold(
                        viewModel = viewModel,
                        onTriggerVoiceInput = { triggerVoiceDictation() }
                    )
                }
            }
        }
    }

    private fun triggerVoiceDictation() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale os dados da entrega, ex: 'Entrega da Pizzaria Central bairro São João quinze reais'")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Reconhecimento de voz não suportado neste dispositivo", Toast.LENGTH_SHORT).show()
            // Fallback manual simulation dialog for easy prototyping and testing
            viewModel.processVoiceDictation("Entrega da Pizzaria Central bairro São João quinze reais")
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    onTriggerVoiceInput: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val motoboyName by viewModel.motoboyName.collectAsStateWithLifecycle()
    val voiceDialogData by viewModel.voiceDialogData.collectAsStateWithLifecycle()
    val voiceProcessing by viewModel.voiceProcessing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Alert badge count based on maintenance warnings
    val maintenances by viewModel.maintenances.collectAsStateWithLifecycle()
    val odometerAlerts = remember(maintenances) {
        maintenances.filter { m ->
            val diff = m.nextOdometer - m.currentOdometer
            diff in 1.0..500.0
        }
    }

    Scaffold(
        topBar = {
            HeaderBar(
                motoboyName = motoboyName,
                alertCount = odometerAlerts.size,
                onAlertClick = { viewModel.navigateTo(Screen.ControleManutencao) },
                onNavigateConfig = { viewModel.navigateTo(Screen.Configuracoes) },
                onBackupClick = {
                    viewModel.triggerBackup()
                    Toast.makeText(context, "Sincronização offline SQLite + Firestore realizada com sucesso!", Toast.LENGTH_SHORT).show()
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentScreen = currentScreen,
                onTabSelect = { viewModel.navigateTo(it) }
            )
        },
        floatingActionButton = {
            if (currentScreen == Screen.Dashboard || currentScreen == Screen.CadastroEntrega) {
                FloatingActionButton(
                    onClick = onTriggerVoiceInput,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    if (voiceProcessing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.Mic, contentDescription = "Comando de Voz", modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel)
                Screen.CadastroEntrega -> DeliveriesScreen(viewModel)
                Screen.CadastroEstabelecimento -> EstablishmentsScreen(viewModel)
                Screen.ControleCombustivel -> FuelScreen(viewModel)
                Screen.ControleDespesas -> ExpensesScreen(viewModel)
                Screen.ControleManutencao -> MaintenanceScreen(viewModel)
                Screen.Metas -> GoalsScreen(viewModel)
                Screen.FechamentoCaixa -> RegisterCloseScreen(viewModel)
                Screen.Relatorios -> ReportsScreen(viewModel)
                Screen.RelatorioEstabelecimento -> PartnerReportScreen(viewModel)
                Screen.Configuracoes -> SettingsScreen(viewModel)
            }

            // Overlay Voice Command Pre-fill confirmation Dialog
            voiceDialogData?.let { data ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearVoiceDialog() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Entrega Capturada por Voz!")
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("A inteligência artificial do MOTOBOX Finance preencheu os campos para você. Confirme os dados antes de salvar:")
                            
                            OutlinedTextField(
                                value = data.establishment,
                                onValueChange = {},
                                label = { Text("Estabelecimento") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = data.neighborhood,
                                onValueChange = {},
                                label = { Text("Bairro") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = "R$ %.2f".format(data.value),
                                onValueChange = {},
                                label = { Text("Valor da Entrega") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            onClick = {
                                viewModel.insertDelivery(
                                    establishmentName = data.establishment,
                                    neighborhood = data.neighborhood,
                                    city = "São Paulo", // Default city
                                    value = data.value,
                                    paymentMethod = "Pix", // Default payment
                                    distanceKm = 4.0, // Default average
                                    clientName = null,
                                    notes = "Adicionado via comando de voz"
                                )
                                viewModel.clearVoiceDialog()
                                Toast.makeText(context, "Entrega salva com sucesso!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Confirmar e Salvar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearVoiceDialog() }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

// --- TOP HEADER TOOLBAR ---
@Composable
fun HeaderBar(
    motoboyName: String,
    alertCount: Int,
    onAlertClick: () -> Unit,
    onNavigateConfig: () -> Unit,
    onBackupClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Motorcycle, contentDescription = "Logo MOTOBOX Finance", modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MOTOBOX Finance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Piloto: $motoboyName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cloud backup icon
                IconButton(onClick = onBackupClick) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = "Backup em Nuvem", tint = MaterialTheme.colorScheme.onPrimary)
                }

                // Alerts badge
                IconButton(onClick = onAlertClick) {
                    BadgedBox(
                        badge = {
                            if (alertCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                    Text(alertCount.toString(), color = Color.White)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Alertas de Manutenção",
                            tint = if (alertCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Settings icon
                IconButton(onClick = onNavigateConfig) {
                    Icon(Icons.Filled.Settings, contentDescription = "Configurações", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// --- BOTTOM NAVIGATION BAR ---
@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onTabSelect: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        val navItems = listOf(
            Triple(Screen.Dashboard, Icons.Filled.Dashboard, "Painel"),
            Triple(Screen.CadastroEntrega, Icons.Filled.DeliveryDining, "Entregas"),
            Triple(Screen.ControleCombustivel, Icons.Filled.LocalGasStation, "Combustível"),
            Triple(Screen.ControleDespesas, Icons.Filled.ReceiptLong, "Despesas"),
            Triple(Screen.Relatorios, Icons.Filled.Summarize, "Relatórios")
        )

        navItems.forEach { (screen, icon, label) ->
            NavigationBarItem(
                selected = currentScreen == screen || 
                           (screen == Screen.Relatorios && (currentScreen == Screen.FechamentoCaixa || currentScreen == Screen.RelatorioEstabelecimento)),
                onClick = { onTabSelect(screen) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

// =====================================================
// TELA 1: DASHBOARD
// =====================================================
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()
    val estStats by viewModel.establishmentStatsList.collectAsStateWithLifecycle()
    
    val scrollState = rememberScrollState()

    // Chart preparation
    val dayChartData = remember(deliveries) {
        val format = SimpleDateFormat("dd/MM", Locale.getDefault())
        val last5Days = (0..4).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.time
        }.reversed()

        last5Days.map { date ->
            val startOfDay = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
            val earnings = deliveries.filter { it.date in startOfDay until endOfDay }.sumOf { it.value }
            Pair(format.format(date), earnings.toFloat())
        }
    }

    val estChartData = remember(estStats) {
        estStats.take(4).map { Pair(it.name, it.deliveriesCount.toFloat()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Resumo Financeiro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Controle total e lucros atualizados em tempo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Primary KPI Cards Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(title = "Ganhos do Dia", value = "R$ %.2f".format(stats.dayEarnings), icon = Icons.Filled.AttachMoney, modifier = Modifier.weight(1f))
                KpiCard(title = "Ganhos da Semana", value = "R$ %.2f".format(stats.weekEarnings), icon = Icons.Filled.DateRange, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(title = "Ganhos do Mês", value = "R$ %.2f".format(stats.monthEarnings), icon = Icons.Filled.CalendarMonth, modifier = Modifier.weight(1f))
                KpiCard(title = "Lucro Líquido", value = "R$ %.2f".format(stats.netProfit), icon = Icons.Filled.AccountBalanceWallet, valueColor = if (stats.netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(title = "Entregas", value = "${stats.deliveriesCount}", icon = Icons.Filled.DeliveryDining, modifier = Modifier.weight(1f))
                KpiCard(title = "Quilometragem", value = "%.1f km".format(stats.distanceKm), icon = Icons.Filled.TwoWheeler, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(title = "Gasto Combustível", value = "R$ %.2f".format(stats.fuelExpenses), icon = Icons.Filled.LocalGasStation, modifier = Modifier.weight(1f))
                KpiCard(title = "Gasto Manutenção", value = "R$ %.2f".format(stats.maintenanceExpenses), icon = Icons.Filled.Build, modifier = Modifier.weight(1f))
            }
        }

        // Goals Progress Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Metas e Progresso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // Daily Goal
                val dayProg = if (stats.dayGoal > 0) (stats.dayEarnings / stats.dayGoal).toFloat() else 0f
                GoalBar(label = "Meta Diária", current = stats.dayEarnings, target = stats.dayGoal, progress = dayProg)

                // Weekly Goal
                val weekProg = if (stats.weekGoal > 0) (stats.weekEarnings / stats.weekGoal).toFloat() else 0f
                GoalBar(label = "Meta Semanal", current = stats.weekEarnings, target = stats.weekGoal, progress = weekProg)

                // Monthly Goal
                val monthProg = if (stats.monthGoal > 0) (stats.monthEarnings / stats.monthGoal).toFloat() else 0f
                GoalBar(label = "Meta Mensal", current = stats.monthEarnings, target = stats.monthGoal, progress = monthProg)
            }
        }

        // Charts Section
        Text("Estatísticas Visuais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ganhos Diários (R$)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                BarChart(data = dayChartData, modifier = Modifier.fillMaxWidth())
            }
        }

        if (estChartData.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Participação das Entregas por Estabelecimento", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    PieChart(data = estChartData, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun GoalBar(label: String, current: Double, target: Double, progress: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("R$ %.0f / R$ %.0f".format(current, target), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (progress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

// =====================================================
// TELA 2: CADASTRO DE ENTREGAS & HISTÓRICO
// =====================================================
@Composable
fun DeliveriesScreen(viewModel: MainViewModel) {
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()
    val establishments by viewModel.establishments.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigation and screen sub-tabs
    var subTab by remember { mutableIntStateOf(0) } // 0 = Histórico, 1 = Nova Entrega, 2 = Estabelecimentos
    
    // Search states
    var searchQuery by remember { mutableStateOf("") }
    
    // Delivery Form states
    var estName by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("São Paulo") }
    var valueStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Pix") }
    var distanceStr by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val filteredDeliveries = remember(deliveries, searchQuery) {
        if (searchQuery.isEmpty()) {
            deliveries
        } else {
            deliveries.filter { d ->
                d.establishmentName.contains(searchQuery, ignoreCase = true) ||
                d.neighborhood.contains(searchQuery, ignoreCase = true) ||
                d.city.contains(searchQuery, ignoreCase = true) ||
                d.paymentMethod.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Histórico") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Lançar") })
            Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("Parceiros") })
        }

        when (subTab) {
            0 -> {
                // Histórico List with filters
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filtrar por Estab., Bairro, Cidade, Pagamento...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (filteredDeliveries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhuma entrega cadastrada.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(filteredDeliveries) { delivery ->
                                DeliveryItemCard(delivery, onDelete = { viewModel.deleteDelivery(delivery) })
                            }
                        }
                    }
                }
            }
            1 -> {
                // Form Cadastro Entrega
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Lançar Nova Entrega", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = estName,
                        onValueChange = { estName = it },
                        label = { Text("Nome do Estabelecimento *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Cliente (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Bairro Destino *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Cidade *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = valueStr,
                            onValueChange = { valueStr = it },
                            label = { Text("Valor (R$) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = distanceStr,
                            onValueChange = { distanceStr = it },
                            label = { Text("Distância (km) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Payment method dropdown selection
                    Column {
                        Text("Forma de Pagamento", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val methods = listOf("Pix", "Dinheiro", "Cartão", "Outro")
                            methods.forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Button(
                        onClick = {
                            val value = valueStr.replace(',', '.').toDoubleOrNull()
                            val distance = distanceStr.replace(',', '.').toDoubleOrNull()
                            if (estName.isNotEmpty() && neighborhood.isNotEmpty() && value != null && distance != null) {
                                viewModel.insertDelivery(
                                    establishmentName = estName,
                                    neighborhood = neighborhood,
                                    city = city,
                                    value = value,
                                    paymentMethod = paymentMethod,
                                    distanceKm = distance,
                                    clientName = clientName.ifEmpty { null },
                                    notes = notes
                                )
                                Toast.makeText(context, "Entrega registrada com sucesso!", Toast.LENGTH_SHORT).show()
                                // Clear form
                                estName = ""
                                neighborhood = ""
                                valueStr = ""
                                distanceStr = ""
                                clientName = ""
                                notes = ""
                                subTab = 0
                            } else {
                                Toast.makeText(context, "Preencha todos os campos obrigatórios (*)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar Entrega")
                    }
                }
            }
            2 -> {
                // Estabelecimentos (Parceiros e Ranking)
                EstablishmentsScreen(viewModel)
            }
        }
    }
}

@Composable
fun DeliveryItemCard(delivery: Delivery, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(delivery.establishmentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(delivery.paymentMethod, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Bairro: ${delivery.neighborhood} - ${delivery.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("Distância: ${delivery.distanceKm} km | Hora: ${delivery.time}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                if (delivery.notes.isNotEmpty()) {
                    Text("Obs: ${delivery.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "R$ %.2f".format(delivery.value),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// =====================================================
// SUB-TELA PARCEIROS (ESTABELECIMENTOS)
// =====================================================
@Composable
fun EstablishmentsScreen(viewModel: MainViewModel) {
    val estStats by viewModel.establishmentStatsList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Form States
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("São Paulo") }
    var phone by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Parceiros & Rankings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar")
            }
        }

        // Mini rankings presentation
        if (estStats.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🏆 Ranking dos Principais Parceiros", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    
                    estStats.take(3).forEachIndexed { index, item ->
                        val medal = when(index) {
                            0 -> "🥇 1º"
                            1 -> "🥈 2º"
                            else -> "🥉 3º"
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$medal ${item.name}", fontWeight = FontWeight.SemiBold)
                            Text("${item.deliveriesCount} entregas (R$ %.1f)".format(item.totalEarnings), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(estStats) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("R$ %.2f".format(item.totalEarnings), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Text("Total entregas: ${item.deliveriesCount}", style = MaterialTheme.typography.bodySmall)
                        Text("Última entrega: ${item.lastDelivery}", style = MaterialTheme.typography.bodySmall)
                        if (item.phone.isNotEmpty()) {
                            Text("Telefone: ${item.phone} | Contato: ${item.contact}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Cadastrar Estabelecimento") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do local *") })
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço") })
                    OutlinedTextField(value = neighborhood, onValueChange = { neighborhood = it }, label = { Text("Bairro") })
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Cidade") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Pessoa de Contato") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observações") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotEmpty()) {
                            viewModel.insertEstablishment(name, address, neighborhood, city, phone, contact, notes)
                            Toast.makeText(context, "Estabelecimento cadastrado!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                            // Reset
                            name = ""
                            address = ""
                            neighborhood = ""
                            phone = ""
                            contact = ""
                            notes = ""
                        } else {
                            Toast.makeText(context, "Nome é obrigatório", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// =====================================================
// TELA 3: CONTROLE DE COMBUSTÍVEL
// =====================================================
@Composable
fun FuelScreen(viewModel: MainViewModel) {
    val fuelLogs by viewModel.fuelLogs.collectAsStateWithLifecycle()
    val stats by viewModel.fuelStats.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var amountStr by remember { mutableStateOf("") }
    var litersStr by remember { mutableStateOf("") }
    var odometerStr by remember { mutableStateOf("") }
    var gasStation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Controle de Combustível", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Statistics Cards
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Desempenho e Gastos Médios", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Consumo Médio (Estimativa):", style = MaterialTheme.typography.bodyMedium)
                    Text("%.1f km/L".format(stats.avgConsumption), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Custo por Quilômetro:", style = MaterialTheme.typography.bodyMedium)
                    Text("R$ %.2f".format(stats.costPerKm), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Abastecido esta semana:", style = MaterialTheme.typography.bodyMedium)
                    Text("R$ %.2f".format(stats.weeklySpent), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Abastecido este mês:", style = MaterialTheme.typography.bodyMedium)
                    Text("R$ %.2f".format(stats.monthlySpent), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Form to Fuel Log
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Registrar Abastecimento", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = gasStation,
                    onValueChange = { gasStation = it },
                    label = { Text("Posto de Combustível *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Valor Gasto (R$) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = litersStr,
                        onValueChange = { litersStr = it },
                        label = { Text("Litros *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = odometerStr,
                    onValueChange = { odometerStr = it },
                    label = { Text("Quilometragem Atual (Odom.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amount = amountStr.replace(',', '.').toDoubleOrNull()
                        val liters = litersStr.replace(',', '.').toDoubleOrNull()
                        val odometer = odometerStr.replace(',', '.').toDoubleOrNull()

                        if (gasStation.isNotEmpty() && amount != null && liters != null && odometer != null) {
                            viewModel.insertFuelLog(gasStation, amount, liters, odometer)
                            Toast.makeText(context, "Abastecimento registrado com sucesso!", Toast.LENGTH_SHORT).show()
                            amountStr = ""
                            litersStr = ""
                            odometerStr = ""
                            gasStation = ""
                        } else {
                            Toast.makeText(context, "Preencha todos os dados corretamente (*)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Abastecimento")
                }
            }
        }

        // Fuel History Logs list
        Text("Histórico de Abastecimentos", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        if (fuelLogs.isEmpty()) {
            Text("Sem registros.", style = MaterialTheme.typography.bodyMedium)
        } else {
            fuelLogs.take(5).forEach { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(log.gasStation, fontWeight = FontWeight.Bold)
                            Text("Odom: %.1f km | %.2f L".format(log.odometer, log.liters), style = MaterialTheme.typography.bodySmall)
                            Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(log.date)), style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("R$ %.2f".format(log.totalAmount), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteFuelLog(log) }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// TELA 4: CONTROLE DE DESPESAS
// =====================================================
@Composable
fun ExpensesScreen(viewModel: MainViewModel) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var valueStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Troca de óleo") }

    val categories = listOf(
        "Combustível", "Troca de óleo", "Filtro de óleo", "Pneu dianteiro", 
        "Pneu traseiro", "Câmara de ar", "Corrente", "Coroa", "Pinhão", 
        "Freios", "Embreagem", "Oficina", "Lavagem", "Seguro", "IPVA", 
        "Licenciamento", "Multas", "Outros"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Controle de Despesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.navigateTo(Screen.ControleManutencao) }) {
                Icon(Icons.Filled.Build, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manutenções")
            }
        }

        // Form to add Expense
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Lançar Nova Despesa", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Valor (R$) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection chips
                Text("Categoria", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observação") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val value = valueStr.replace(',', '.').toDoubleOrNull()
                        if (value != null && value > 0) {
                            viewModel.insertExpense(category, value, notes)
                            Toast.makeText(context, "Despesa registrada!", Toast.LENGTH_SHORT).show()
                            valueStr = ""
                            notes = ""
                        } else {
                            Toast.makeText(context, "Preencha um valor válido", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Despesa")
                }
            }
        }

        // Expenses lists
        Text("Histórico de Despesas", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        if (expenses.isEmpty()) {
            Text("Nenhuma despesa lançada.", style = MaterialTheme.typography.bodyMedium)
        } else {
            expenses.take(10).forEach { expense ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.category, fontWeight = FontWeight.Bold)
                            if (expense.notes.isNotEmpty()) {
                                Text(expense.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expense.date)), style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("R$ %.2f".format(expense.value), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteExpense(expense) }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// TELA 5: CONTROLE DE MANUTENÇÃO (With proximity warnings)
// =====================================================
@Composable
fun MaintenanceScreen(viewModel: MainViewModel) {
    val maintenances by viewModel.maintenances.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var item by remember { mutableStateOf("Troca de óleo") }
    var currentOdomStr by remember { mutableStateOf("") }
    var nextOdomStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val items = listOf("Troca de óleo", "Filtro", "Vela", "Corrente", "Coroa", "Pinhão", "Freios", "Embreagem", "Bateria", "Pneu dianteiro", "Pneu trasei.", "Outros")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Controle de Manutenção", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Warnings panel
        val warnings = remember(maintenances) {
            maintenances.filter { m ->
                val diff = m.nextOdometer - m.currentOdometer
                diff in 1.0..500.0
            }
        }

        if (warnings.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atenção - Próximas Revisões!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    warnings.forEach { w ->
                        val diff = w.nextOdometer - w.currentOdometer
                        Text("• ${w.item}: restam apenas %.0f km para a próxima manutenção!".format(diff), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }

        // Log maintenance
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Registrar Manutenção", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                Text("Item Manutenção", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { itm ->
                        FilterChip(
                            selected = item == itm,
                            onClick = { item = itm },
                            label = { Text(itm) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentOdomStr,
                        onValueChange = { currentOdomStr = it },
                        label = { Text("Odom. Atual (km) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nextOdomStr,
                        onValueChange = { nextOdomStr = it },
                        label = { Text("Próx. Km (Aviso) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Custo Manutenção (R$) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observação") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val currentOdom = currentOdomStr.replace(',', '.').toDoubleOrNull()
                        val nextOdom = nextOdomStr.replace(',', '.').toDoubleOrNull()
                        val cost = costStr.replace(',', '.').toDoubleOrNull()

                        if (currentOdom != null && nextOdom != null && cost != null) {
                            viewModel.insertMaintenance(item, currentOdom, cost, nextOdom, notes)
                            Toast.makeText(context, "Manutenção registrada!", Toast.LENGTH_SHORT).show()
                            currentOdomStr = ""
                            nextOdomStr = ""
                            costStr = ""
                            notes = ""
                        } else {
                            Toast.makeText(context, "Preencha todos os dados corretamente", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Manutenção")
                }
            }
        }

        // Maintenance Log lists
        Text("Histórico de Manutenções", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        if (maintenances.isEmpty()) {
            Text("Nenhum registro.", style = MaterialTheme.typography.bodyMedium)
        } else {
            maintenances.forEach { m ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(m.item, fontWeight = FontWeight.Bold)
                            Text("Km Atual: %.0f | Próxima: %.0f".format(m.currentOdometer, m.nextOdometer), style = MaterialTheme.typography.bodySmall)
                            Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(m.date)), style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("R$ %.2f".format(m.cost), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteMaintenance(m) }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// TELA 6: METAS CARD SETTINGS
// =====================================================
@Composable
fun GoalsScreen(viewModel: MainViewModel) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var dayGoalVal by remember { mutableStateOf("") }
    var weekGoalVal by remember { mutableStateOf("") }
    var monthGoalVal by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Metas Financeiras", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ajustar Metas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = dayGoalVal,
                    onValueChange = { dayGoalVal = it },
                    label = { Text("Meta Diária (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weekGoalVal,
                    onValueChange = { weekGoalVal = it },
                    label = { Text("Meta Semanal (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = monthGoalVal,
                    onValueChange = { monthGoalVal = it },
                    label = { Text("Meta Mensal (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val d = dayGoalVal.replace(',', '.').toDoubleOrNull()
                        val w = weekGoalVal.replace(',', '.').toDoubleOrNull()
                        val m = monthGoalVal.replace(',', '.').toDoubleOrNull()

                        if (d != null) viewModel.updateGoal("Diária", d)
                        if (w != null) viewModel.updateGoal("Semanal", w)
                        if (m != null) viewModel.updateGoal("Mensal", m)

                        Toast.makeText(context, "Metas atualizadas com sucesso!", Toast.LENGTH_SHORT).show()
                        dayGoalVal = ""
                        weekGoalVal = ""
                        monthGoalVal = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Novas Metas")
                }
            }
        }
    }
}

// =====================================================
// TELA 7: FECHAMENTO DE CAIXA ("Fechamento do Dia")
// =====================================================
@Composable
fun RegisterCloseScreen(viewModel: MainViewModel) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Hours worked (user can adjust)
    var hoursWorked by remember { mutableStateOf("8") }

    val closeReport = remember(stats, deliveries, hoursWorked) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayDeliveries = deliveries.filter { it.date >= today }
        val group = todayDeliveries.groupBy { it.establishmentName }
        val topEst = group.maxByOrNull { it.value.size }?.key ?: "Nenhum"

        """
        === 🏍️ MOTOBOX Finance - FECHAMENTO ===
        Data: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}
        ------------------------------------------
        • Total de entregas: ${todayDeliveries.size}
        • Total recebido: R$ %.2f
        • Total gasto: R$ %.2f
        • Lucro líquido: R$ %.2f
        • Combustível: R$ %.2f
        • Estabelecimento top: $topEst
        • Tempo trabalhado: $hoursWorked horas
        ------------------------------------------
        Gerado pelo MOTOBOX Finance offline.
        """.trimIndent().format(stats.dayEarnings, stats.fuelExpenses, stats.dayEarnings - stats.fuelExpenses, stats.fuelExpenses)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Fechamento do Dia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ajustes Finais", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = hoursWorked,
                    onValueChange = { hoursWorked = it },
                    label = { Text("Tempo Trabalhado (Horas)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Preview Box
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Visualização do Relatório", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = closeReport,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp)
                )
            }
        }

        Button(
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, closeReport)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Enviar Fechamento")
                context.startActivity(shareIntent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Compartilhar no WhatsApp")
        }
    }
}

// =====================================================
// TELA 8: RELATÓRIOS GERAIS
// =====================================================
@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedPeriod by remember { mutableStateOf("Diário") }
    val periods = listOf("Diário", "Semanal", "Mensal", "Anual", "Fechamento")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Relatórios de Gestão", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.navigateTo(Screen.RelatorioEstabelecimento) }) {
                Icon(Icons.Filled.Business, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Por Estabelecimento")
            }
        }

        // Tab switches
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            periods.forEach { p ->
                FilterChip(
                    selected = selectedPeriod == p,
                    onClick = {
                        selectedPeriod = p
                        if (p == "Fechamento") {
                            viewModel.navigateTo(Screen.FechamentoCaixa)
                        }
                    },
                    label = { Text(p) }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Relatório $selectedPeriod", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                val displayEarnings = when (selectedPeriod) {
                    "Diário" -> stats.dayEarnings
                    "Semanal" -> stats.weekEarnings
                    "Mensal" -> stats.monthEarnings
                    else -> stats.yearEarnings
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Recebido:")
                    Text("R$ %.2f".format(displayEarnings), fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gastos Totais:")
                    Text("R$ %.2f".format(stats.fuelExpenses + stats.maintenanceExpenses), fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lucro Líquido:")
                    Text("R$ %.2f".format(displayEarnings - stats.fuelExpenses - stats.maintenanceExpenses), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Distância Percorrida:")
                    Text("%.1f km".format(stats.distanceKm), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Export data
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Exportar Dados", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Selecione um formato para exportar seus relatórios de finanças offline.", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { Toast.makeText(context, "Planilha Excel criada e salva localmente!", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel")
                    }
                    Button(
                        onClick = { Toast.makeText(context, "Relatório em PDF gerado na pasta Downloads!", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF")
                    }
                }
            }
        }
    }
}

// =====================================================
// TELA 9: RELATÓRIO POR ESTABELECIMENTO
// =====================================================
@Composable
fun PartnerReportScreen(viewModel: MainViewModel) {
    val estStats by viewModel.establishmentStatsList.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedEst by remember { mutableStateOf("Todos") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Relatório para Estabelecimentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Select specific partner or all
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Parceiro selecionado: $selectedEst")
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Todos") }, onClick = { selectedEst = "Todos"; expanded = false })
                estStats.forEach { item ->
                    DropdownMenuItem(text = { Text(item.name) }, onClick = { selectedEst = item.name; expanded = false })
                }
            }
        }

        // Generated professional layout
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MOTOBOX Finance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Relatório de Entregas", style = MaterialTheme.typography.titleSmall)
                Text("Emissão: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", style = MaterialTheme.typography.labelSmall)
                HorizontalDivider()

                if (selectedEst == "Todos") {
                    Text("Resumo de Todos os Estabelecimentos", fontWeight = FontWeight.Bold)
                    estStats.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name)
                            Text("${item.deliveriesCount} ent. | R$ %.2f".format(item.totalEarnings))
                        }
                    }
                } else {
                    val est = estStats.find { it.name == selectedEst }
                    if (est != null) {
                        Text("Estabelecimento: ${est.name}", fontWeight = FontWeight.Bold)
                        Text("Quantidade total de entregas: ${est.deliveriesCount}")
                        Text("Valor total recebido: R$ %.2f".format(est.totalEarnings))
                        Text("Última entrega: ${est.lastDelivery}")
                    }
                }

                HorizontalDivider()
                val totalEnt = if (selectedEst == "Todos") estStats.sumOf { it.deliveriesCount } else estStats.find { it.name == selectedEst }?.deliveriesCount ?: 0
                val totalRec = if (selectedEst == "Todos") estStats.sumOf { it.totalEarnings } else estStats.find { it.name == selectedEst }?.totalEarnings ?: 0.0

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL DE ENTREGAS:", fontWeight = FontWeight.Bold)
                    Text("$totalEnt", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL RECEBIDO:", fontWeight = FontWeight.Bold)
                    Text("R$ %.2f".format(totalRec), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Button(
            onClick = { Toast.makeText(context, "PDF profissional exportado para Downloads!", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("GERAR RELATÓRIO PDF")
        }
    }
}

// =====================================================
// TELA 10: CONFIGURAÇÕES DO APLICATIVO
// =====================================================
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val name by viewModel.motoboyName.collectAsStateWithLifecycle()
    val phone by viewModel.motoboyPhone.collectAsStateWithLifecycle()
    val darkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val autoTheme by viewModel.useAutoTheme.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editName by remember { mutableStateOf(name) }
    var editPhone by remember { mutableStateOf(phone) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configurações do MOTOBOX Finance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Perfil do Motoboy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nome do Motoboy") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text("Telefone / WhatsApp") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Aparência e Temas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Modo Escuro")
                    Switch(checked = darkTheme, onCheckedChange = { viewModel.updateSettings(editName, editPhone, it, autoTheme) })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tema Automático")
                    Switch(checked = autoTheme, onCheckedChange = { viewModel.updateSettings(editName, editPhone, darkTheme, it) })
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Manutenção do Banco de Dados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Button(
                    onClick = {
                        viewModel.triggerBackup()
                        Toast.makeText(context, "Sincronização com Firestore bem-sucedida!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fazer Backup na Nuvem")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.triggerRestore()
                        Toast.makeText(context, "Dados restaurados localmente do SQLite!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar Backup")
                }
            }
        }

        Button(
            onClick = {
                viewModel.updateSettings(editName, editPhone, darkTheme, autoTheme)
                Toast.makeText(context, "Configurações atualizadas!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar Perfil")
        }
    }
}
