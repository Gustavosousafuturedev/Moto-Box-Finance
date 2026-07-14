package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

// Style Color Palettes (Dark Octane / Carbon Vibe)
val CarbonBg = Color(0xFF121214)
val CarbonSurface = Color(0xFF1C1C1F)
val HighOctaneAmber = Color(0xFFFF9F0A)
val DeepGold = Color(0xFFFFD60A)
val SteelGray = Color(0xFF8E8E93)
val MediumGray = Color(0xFFAEAEB2)
val BackgroundDark = Color(0xFF0F0F11)

val GreenPIX = Color(0xFF34C759)
val BlueCash = Color(0xFF30B0C7)
val AlertRed = Color(0xFFFF453A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MainApplication
        val factory = MainViewModelFactory(app, app.repository)
        setContent {
            val viewModel: MainViewModel = viewModel(factory = factory)

            MotoGestorApp(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoGestorApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("panel") } // "panel", "deliveries", "expenses", "reports", "settings"

    // Collect states
    val userProfile by viewModel.userProfile.collectAsState()
    val allDeliveries by viewModel.allDeliveries.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val activeMoto by viewModel.activeMotorcycle.collectAsState()
    val refuels by viewModel.allRefuels.collectAsState()
    val maintenances by viewModel.allMaintenances.collectAsState()

    // Observe notifications
    LaunchedEffect(key1 = true) {
        viewModel.message.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundDark
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = CarbonSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Painel") },
                        label = { Text("Painel") },
                        selected = currentTab == "panel",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HighOctaneAmber,
                            selectedTextColor = HighOctaneAmber,
                            unselectedIconColor = SteelGray,
                            unselectedTextColor = SteelGray,
                            indicatorColor = Color(0x33FF9F0A)
                        ),
                        onClick = { currentTab = "panel" },
                        modifier = Modifier.testTag("tab_panel")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DirectionsBike, contentDescription = "Entregas") },
                        label = { Text("Entregas") },
                        selected = currentTab == "deliveries",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HighOctaneAmber,
                            selectedTextColor = HighOctaneAmber,
                            unselectedIconColor = SteelGray,
                            unselectedTextColor = SteelGray,
                            indicatorColor = Color(0x33FF9F0A)
                        ),
                        onClick = { currentTab = "deliveries" },
                        modifier = Modifier.testTag("tab_deliveries")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AttachMoney, contentDescription = "Despesas") },
                        label = { Text("Despesas") },
                        selected = currentTab == "expenses",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HighOctaneAmber,
                            selectedTextColor = HighOctaneAmber,
                            unselectedIconColor = SteelGray,
                            unselectedTextColor = SteelGray,
                            indicatorColor = Color(0x33FF9F0A)
                        ),
                        onClick = { currentTab = "expenses" },
                        modifier = Modifier.testTag("tab_expenses")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "Relatórios") },
                        label = { Text("Relatórios") },
                        selected = currentTab == "reports",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HighOctaneAmber,
                            selectedTextColor = HighOctaneAmber,
                            unselectedIconColor = SteelGray,
                            unselectedTextColor = SteelGray,
                            indicatorColor = Color(0x33FF9F0A)
                        ),
                        onClick = { currentTab = "reports" },
                        modifier = Modifier.testTag("tab_reports")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                        label = { Text("Ajustes") },
                        selected = currentTab == "settings",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HighOctaneAmber,
                            selectedTextColor = HighOctaneAmber,
                            unselectedIconColor = SteelGray,
                            unselectedTextColor = SteelGray,
                            indicatorColor = Color(0x33FF9F0A)
                        ),
                        onClick = { currentTab = "settings" },
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    "panel" -> CourierPanelScreen(viewModel)
                    "deliveries" -> DeliveriesScreen(viewModel)
                    "expenses" -> ExpensesScreen(viewModel)
                    "reports" -> ReportsScreen(viewModel)
                    "settings" -> SettingsScreen(viewModel)
                }
            }
        }
    }
}

// --- AVATAR CONSTANTS ---
val AvatarOptions = listOf(
    "🚴‍♂️" to "Entrega Hero",
    "🏍️" to "Rider Veloz",
    "⚡" to "Raio Urbano",
    "😎" to "Courier Master",
    "🎒" to "Parceiro Corre",
    "🏁" to "Piloto Pro"
)

@Composable
fun AvatarView(photoUri: String, size: dp = 48.dp) {
    val emoji = AvatarOptions.firstOrNull { it.first == photoUri }?.first ?: "🛵"
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2C2C2E)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value * 0.55).sp)
    }
}

// ==========================================
// 1 & 24 & 15. COURIER DASHBOARD PANEL SCREEN (TELA INICIAL / PAINEL DO MOTOBOY)
// ==========================================
@Composable
fun CourierPanelScreen(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val allDeliveries by viewModel.allDeliveries.collectAsState()
    val allRefuels by viewModel.allRefuels.collectAsState()
    
    val deliveriesToday = viewModel.getDeliveriesCountToday()
    val goalPercent = viewModel.getDailyGoalPercentage()
    val deliveriesWeek = viewModel.getDeliveriesCountThisWeek()
    val deliveriesMonth = viewModel.getDeliveriesCountThisMonth()
    val activeEsts = viewModel.getActiveEstablishmentsCount()
    val avgDaily = viewModel.getAverageDailyDeliveries()
    val topEstName = viewModel.getBestEstablishmentName()

    // Last refuel info
    val lastRefuel = allRefuels.firstOrNull()
    // Predicted next oil change
    val nextOilChange = viewModel.getNextOilChangeKm()
    
    // Greeting depending on current hour
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour in 5..11 -> "Bom dia"
        hour in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    val driverName = userProfile?.name?.trim() ?: ""
    val welcomeText = if (driverName.isNotEmpty()) "$greeting, $driverName 👋" else "Olá, Entregador 👋"

    // Calendar logic state
    var selectedCalendarDate by remember { mutableStateOf("") }
    val sdfDay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val deliveriesByDate = viewModel.getDeliveriesByDate()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Profile Greeting Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarbonSurface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(photoUri = userProfile?.photoUri ?: "", size = 52.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = welcomeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Piloto MotoGestor • ${userProfile?.motorcycleModel.ifEmpty { "Sem Moto" }}",
                        color = SteelGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            // Meta do dia Progress Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Meta Diária (Objetivo: 15 Entregas)",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hoje você realizou: $deliveriesToday entregas",
                            color = MediumGray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "$goalPercent%",
                            color = HighOctaneAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = (goalPercent / 100.0).toFloat().coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = HighOctaneAmber,
                        trackColor = Color(0xFF2C2C2E)
                    )
                }
            }
        }

        item {
            // KPI Summary Grid (Dashboard Melhorado)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Hoje",
                        value = "$deliveriesToday",
                        icon = Icons.Default.DirectionsBike,
                        tint = HighOctaneAmber
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Esta Semana",
                        value = "$deliveriesWeek",
                        icon = Icons.Default.CalendarMonth,
                        tint = DeepGold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Este Mês",
                        value = "$deliveriesMonth",
                        icon = Icons.Default.CalendarMonth,
                        tint = BlueCash
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Parceiros Ativos",
                        value = "$activeEsts",
                        icon = Icons.Default.Store,
                        tint = GreenPIX
                    )
                }
            }
        }

        item {
            // Extra KPI stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resumo Operacional",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Média Diária de Entregas", color = MediumGray, fontSize = 13.sp)
                        Text(String.format(Locale.getDefault(), "%.1f", avgDaily), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Melhor Estabelecimento", color = MediumGray, fontSize = 13.sp)
                        Text(topEstName, color = HighOctaneAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            // Payment Summary Section (No money values shown!)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resumo por Forma de Pagamento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val paymentCounts = viewModel.getPaymentMethodCounts()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GreenPIX))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PIX (Entregas)", color = MediumGray, fontSize = 13.sp)
                        }
                        Text("${paymentCounts["PIX"] ?: 0} corridas", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BlueCash))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dinheiro (Entregas)", color = MediumGray, fontSize = 13.sp)
                        }
                        Text("${paymentCounts["Dinheiro"] ?: 0} corridas", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Calendar and Days marking
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calendário de Corridas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Toque em um dia para listar as entregas correspondentes",
                        color = SteelGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Simple July 2026 Grid Calendário
                    // July 2026 starts on Wednesday (3), 31 days
                    val year = 2026
                    val month = 6 // July (0-indexed Calendar)
                    val days = (1..31).toList()
                    val startOffset = 3 // Wednesday

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                            Text(
                                text = it,
                                color = SteelGray,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Draw calendar grid
                    val totalSlots = startOffset + 31
                    val rows = (totalSlots + 6) / 7

                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (c in 0..6) {
                                val slotIndex = r * 7 + c
                                val dayNum = slotIndex - startOffset + 1
                                if (dayNum in 1..31) {
                                    val formattedDate = String.format(Locale.getDefault(), "%02d/07/%d", dayNum, year)
                                    val hasRuns = deliveriesByDate.containsKey(formattedDate)
                                    val isSelected = selectedCalendarDate == formattedDate

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) HighOctaneAmber.copy(alpha = 0.3f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedCalendarDate = if (isSelected) "" else formattedDate
                                            }
                                            .padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            color = if (isSelected) HighOctaneAmber else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (hasRuns) HighOctaneAmber else Color.Transparent)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Selected Calendar Date Runs
        if (selectedCalendarDate.isNotEmpty()) {
            val dateDeliveries = deliveriesByDate[selectedCalendarDate] ?: emptyList()
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Entregas em $selectedCalendarDate",
                                fontWeight = FontWeight.Bold,
                                color = HighOctaneAmber,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { selectedCalendarDate = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = SteelGray, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (dateDeliveries.isEmpty()) {
                            Text("Nenhuma entrega realizada nesta data.", color = SteelGray, fontSize = 13.sp)
                        } else {
                            dateDeliveries.forEach { del ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(del.establishment, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = if (del.paymentMethod == "PIX") "🟢 PIX" else "💵 Dinheiro",
                                            color = if (del.paymentMethod == "PIX") GreenPIX else BlueCash,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.getDefault(), "R$ %.2f", del.value),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Divider(color = Color(0xFF2C2C2E))
                            }
                        }
                    }
                }
            }
        }

        item {
            // Partners section (Parceiros Frequentes)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Parceiros Frequentes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Estabelecimentos com maior volume de entregas",
                        color = SteelGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val topEsts = viewModel.getTop5Establishments()
                    if (topEsts.isEmpty()) {
                        Text(
                            text = "Nenhum estabelecimento frequente ainda.",
                            color = SteelGray,
                            fontSize = 13.sp
                        )
                    } else {
                        topEsts.forEachIndexed { idx, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${idx + 1}.",
                                        color = HighOctaneAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = pair.first,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2C2C2E))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${pair.second} entregas",
                                        color = HighOctaneAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (idx < topEsts.size - 1) {
                                Divider(color = Color(0xFF2C2C2E))
                            }
                        }
                    }
                }
            }
        }

        item {
            // Vehicle & Maintenance overview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monitoramento do Veículo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Último Abastecimento", color = SteelGray, fontSize = 11.sp)
                            if (lastRefuel != null) {
                                Text(
                                    text = "${lastRefuel.liters}L em ${String.format(Locale.getDefault(), "%,.0f", lastRefuel.odometer)} KM",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "Custo: R$ %.2f", lastRefuel.totalCost),
                                    color = DeepGold,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text("Nenhum registrado", color = Color.White, fontSize = 13.sp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Previsão Troca de Óleo", color = SteelGray, fontSize = 11.sp)
                            Text(
                                text = if (nextOilChange > 0) String.format(Locale.getDefault(), "%,.0f KM", nextOilChange) else "--- KM",
                                color = HighOctaneAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            val remaining = nextOilChange - (userProfile?.currentOdometer ?: 0.0)
                            Text(
                                text = if (remaining > 0) String.format(Locale.getDefault(), "Faltam %,.0f KM", remaining) else "Trocar agora!",
                                color = if (remaining > 0) SteelGray else AlertRed,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun KpiCard(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
            Column {
                Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(text = title, fontSize = 11.sp, color = SteelGray)
            }
        }
    }
}

// ==========================================
// 4 & 5 & 12 & 13 & 17. DELIVERIES TAB (TELA DE ENTREGAS)
// ==========================================
@Composable
fun DeliveriesScreen(viewModel: MainViewModel) {
    val filteredDeliveries by viewModel.filteredDeliveries.collectAsState()
    val allDeliveries by viewModel.allDeliveries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var deliveryToEdit by remember { mutableStateOf<Delivery?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Unique establishments list for filter
    val establishments = allDeliveries.map { it.establishment.trim() }.distinct().filter { it.isNotBlank() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = HighOctaneAmber,
                shape = CircleShape,
                modifier = Modifier.testTag("add_delivery_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Entrega", tint = Color.Black)
            }
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Minhas Entregas",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search Bar & Filters Trigger Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Buscar estabelecimento, forma, data...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = SteelGray) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("search_deliveries_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighOctaneAmber,
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        textColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showFilterSheet = !showFilterSheet },
                    modifier = Modifier
                        .size(54.dp)
                        .background(if (showFilterSheet) HighOctaneAmber.copy(alpha = 0.2f) else CarbonSurface, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros", tint = if (showFilterSheet) HighOctaneAmber else Color.White)
                }
            }

            // Advanced Filters Panel (Filtros: Data, Estabelecimento, Forma de Pagamento)
            AnimatedVisibility(
                visible = showFilterSheet,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Filtros Avançados", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = { viewModel.clearFilters() }) {
                                Text("Limpar", color = HighOctaneAmber, fontSize = 12.sp)
                            }
                        }

                        // Payment Method Filter
                        Column {
                            Text("Forma de Pagamento", color = SteelGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("PIX", "Dinheiro").forEach { method ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2C2C2E))
                                            .border(1.dp, HighOctaneAmber, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setPaymentMethodFilter(method) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(method, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Quick date filter buttons
                        Column {
                            Text("Período", color = SteelGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
                                        viewModel.setDateFilter(start, System.currentTimeMillis())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Hoje", color = Color.White, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
                                        viewModel.setDateFilter(start, System.currentTimeMillis())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Últimos 7 dias", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sorting bar (Mais recentes, Mais antigas, etc.)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ordenar por:", color = SteelGray, fontSize = 12.sp)
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSortMenu = true }
                            .background(CarbonSurface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sortType, color = HighOctaneAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("▼", color = SteelGray, fontSize = 8.sp)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(CarbonSurface)
                    ) {
                        listOf("Mais recentes", "Mais antigas", "Maior número de entregas", "Nome do estabelecimento").forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort, color = Color.White) },
                                onClick = {
                                    viewModel.updateSortType(sort)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Deliveries List
            if (filteredDeliveries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = "Sem entregas", tint = SteelGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Nenhuma entrega encontrada", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Altere os filtros ou registre novas corridas clicando no +", color = SteelGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredDeliveries) { delivery ->
                        // Redesigned Delivery Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deliveryToEdit = delivery },
                            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = delivery.establishment,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Data", tint = SteelGray, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(delivery.dateMillis))
                                        Text(dateStr, color = SteelGray, fontSize = 11.sp)
                                    }
                                    if (delivery.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = delivery.notes,
                                            color = MediumGray,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Custom Badges for PIX or Dinheiro
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (delivery.paymentMethod == "PIX") GreenPIX.copy(alpha = 0.15f)
                                                else BlueCash.copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (delivery.paymentMethod == "PIX") "🟢 PIX" else "💵 Dinheiro",
                                                color = if (delivery.paymentMethod == "PIX") GreenPIX else BlueCash,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "R$ %.2f", delivery.value),
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Delivery Dialog (Form inputs: Estabelecimento, Valor, Forma de pagamento, Data/Hora, Observações)
    if (showAddDialog) {
        DeliveryFormDialog(
            title = "Adicionar Entrega",
            onDismiss = { showAddDialog = false },
            onSave = { est, valStr, pay, date, notes ->
                val value = valStr.toDoubleOrNull() ?: 0.0
                viewModel.addDelivery(est, value, pay, date, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Delivery Dialog (Simple screen, Removed fields: Cliente, Cidade, Bairro, Cartão, Outro)
    if (deliveryToEdit != null) {
        val del = deliveryToEdit!!
        DeliveryFormDialog(
            title = "Editar Entrega",
            delivery = del,
            onDismiss = { deliveryToEdit = null },
            onSave = { est, valStr, pay, date, notes ->
                val value = valStr.toDoubleOrNull() ?: 0.0
                viewModel.updateDelivery(del.copy(establishment = est, value = value, paymentMethod = pay, dateMillis = date, notes = notes))
                deliveryToEdit = null
            },
            onDelete = {
                viewModel.deleteDelivery(del)
                deliveryToEdit = null
            }
        )
    }
}

@Composable
fun DeliveryFormDialog(
    title: String,
    delivery: Delivery? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var establishment by remember { mutableStateOf(delivery?.establishment ?: "") }
    var valueStr by remember { mutableStateOf(delivery?.let { String.format(Locale.getDefault(), "%.2f", it.value) } ?: "") }
    var paymentMethod by remember { mutableStateOf(delivery?.paymentMethod ?: "PIX") }
    var notes by remember { mutableStateOf(delivery?.notes ?: "") }

    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CarbonSurface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 11. Top Back Button & Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                Divider(color = Color(0xFF2C2C2E))

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = AlertRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 23. Validations: cannot save without establishment, value, paymentMethod
                OutlinedTextField(
                    value = establishment,
                    onValueChange = { establishment = it },
                    label = { Text("Estabelecimento") },
                    placeholder = { Text("Ex: Pizzaria Central") },
                    modifier = Modifier.fillMaxWidth().testTag("delivery_est_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighOctaneAmber,
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        textColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Valor da Corrida (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Ex: 12.50") },
                    modifier = Modifier.fillMaxWidth().testTag("delivery_val_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighOctaneAmber,
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        textColor = Color.White
                    )
                )

                Text("Forma de Pagamento", color = SteelGray, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("PIX", "Dinheiro").forEach { method ->
                        val selected = paymentMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) HighOctaneAmber else Color(0xFF2C2C2E))
                                .clickable { paymentMethod = method }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method,
                                color = if (selected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighOctaneAmber,
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        textColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (delivery != null && onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = AlertRed)
                        ) {
                            Text("Excluir", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = SteelGray)
                        }
                        Button(
                            onClick = {
                                val valParsed = valueStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (establishment.isBlank()) {
                                    errorMsg = "Insira o nome do estabelecimento."
                                } else if (valParsed <= 0.0) {
                                    errorMsg = "Insira um valor maior que R$ 0."
                                } else {
                                    onSave(establishment, valueStr, paymentMethod, delivery?.dateMillis ?: System.currentTimeMillis(), notes)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber)
                        ) {
                            Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. EXPENSES TAB (DESPESAS COM ALIMENTAÇÃO, PEDÁGIO, ESTACIONAMENTO, REFUEL, MAINTENANCE)
// ==========================================
@Composable
fun ExpensesScreen(viewModel: MainViewModel) {
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allRefuels by viewModel.allRefuels.collectAsState()
    val allMaintenances by viewModel.allMaintenances.collectAsState()

    var activeExpenseTab by remember { mutableStateOf("Geral") } // "Geral", "Abastecimentos", "Manutenções"
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddRefuelDialog by remember { mutableStateOf(false) }
    var showAddMaintDialog by remember { mutableStateOf(false) }

    // Aggregate values for a clean custom proportions chart
    val totalGeral = allExpenses.sumOf { it.cost }
    val totalRefuels = allRefuels.sumOf { it.totalCost }
    val totalMaints = allMaintenances.sumOf { it.cost }
    val grandTotal = totalGeral + totalRefuels + totalMaints

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Minhas Despesas",
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom Styled Proportions Chart (Stacked Bar / Pie indicator representation)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Proporção de Despesas", color = SteelGray, fontSize = 12.sp)
                Text(
                    text = String.format(Locale.getDefault(), "Total: R$ %.2f", grandTotal),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Custom segmented color line
                if (grandTotal > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        val pctGeral = (totalGeral / grandTotal).toFloat()
                        val pctRefuels = (totalRefuels / grandTotal).toFloat()
                        val pctMaints = (totalMaints / grandTotal).toFloat()

                        if (pctGeral > 0) Box(modifier = Modifier.weight(pctGeral).fillMaxHeight().background(HighOctaneAmber))
                        if (pctRefuels > 0) Box(modifier = Modifier.weight(pctRefuels).fillMaxHeight().background(DeepGold))
                        if (pctMaints > 0) Box(modifier = Modifier.weight(pctMaints).fillMaxHeight().background(BlueCash))
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF2C2C2E)))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("Geral", HighOctaneAmber, totalGeral)
                    LegendItem("Combustível", DeepGold, totalRefuels)
                    LegendItem("Manutenção", BlueCash, totalMaints)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs Row: Geral, Abastecimentos, Manutenções
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CarbonSurface)
                .padding(4.dp)
        ) {
            listOf("Geral", "Abastecimentos", "Manutenções").forEach { tab ->
                val isSelected = activeExpenseTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) HighOctaneAmber else Color.Transparent)
                        .clickable { activeExpenseTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content List
        Box(modifier = Modifier.weight(1f)) {
            when (activeExpenseTab) {
                "Geral" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Alimentação, Pedágio, Estacionamento...", color = SteelGray, fontSize = 12.sp)
                            Button(
                                onClick = { showAddExpenseDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Nova Despesa", tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adicionar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (allExpenses.isEmpty()) {
                            EmptyLogsPlaceholder("Nenhuma despesa geral registrada.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allExpenses) { exp ->
                                    var showDeleteConfirm by remember { mutableStateOf(false) }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(exp.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2C2C2E)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                        Text(exp.category, color = HighOctaneAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(exp.dateMillis))
                                                    Text(dateStr, color = SteelGray, fontSize = 11.sp)
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(String.format(Locale.getDefault(), "R$ %.2f", exp.cost), color = Color.White, fontWeight = FontWeight.Bold)
                                                IconButton(onClick = { showDeleteConfirm = true }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = AlertRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    if (showDeleteConfirm) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteConfirm = false },
                                            title = { Text("Deletar Despesa?", color = Color.White) },
                                            text = { Text("Deseja realmente excluir esta despesa?", color = MediumGray) },
                                            containerColor = CarbonSurface,
                                            confirmButton = {
                                                Button(onClick = { viewModel.deleteExpense(exp); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = AlertRed)) {
                                                    Text("Excluir")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar", color = SteelGray) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "Abastecimentos" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Acompanhamento de Combustível", color = SteelGray, fontSize = 12.sp)
                            Button(
                                onClick = { showAddRefuelDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Novo Abastecimento", tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Registrar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (allRefuels.isEmpty()) {
                            EmptyLogsPlaceholder("Nenhum abastecimento registrado.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allRefuels) { ref ->
                                    var showConfirm by remember { mutableStateOf(false) }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(String.format(Locale.getDefault(), "Odo: %,.0f KM", ref.odometer), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${ref.liters}L • R$ ${String.format(Locale.getDefault(), "%.2f", ref.pricePerLiter)}/L", color = SteelGray, fontSize = 11.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(String.format(Locale.getDefault(), "R$ %.2f", ref.totalCost), color = DeepGold, fontWeight = FontWeight.Bold)
                                                IconButton(onClick = { showConfirm = true }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = AlertRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    if (showConfirm) {
                                        AlertDialog(
                                            onDismissRequest = { showConfirm = false },
                                            title = { Text("Excluir Abastecimento?", color = Color.White) },
                                            containerColor = CarbonSurface,
                                            confirmButton = {
                                                Button(onClick = { viewModel.deleteRefuel(ref); showConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = AlertRed)) {
                                                    Text("Excluir")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showConfirm = false }) { Text("Cancelar", color = SteelGray) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "Manutenções" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Histórico de Serviços & Óleo", color = SteelGray, fontSize = 12.sp)
                            Button(
                                onClick = { showAddMaintDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Nova Manutenção", tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Registrar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (allMaintenances.isEmpty()) {
                            EmptyLogsPlaceholder("Nenhuma manutenção registrada.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allMaintenances) { maint ->
                                    var showConfirm by remember { mutableStateOf(false) }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(maint.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Tipo: ${maint.type} • KM: ${String.format(Locale.getDefault(), "%,.0f", maint.odometer)}", color = SteelGray, fontSize = 11.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(String.format(Locale.getDefault(), "R$ %.2f", maint.cost), color = Color.White, fontWeight = FontWeight.Bold)
                                                IconButton(onClick = { showConfirm = true }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = AlertRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    if (showConfirm) {
                                        AlertDialog(
                                            onDismissRequest = { showConfirm = false },
                                            title = { Text("Excluir Manutenção?", color = Color.White) },
                                            containerColor = CarbonSurface,
                                            confirmButton = {
                                                Button(onClick = { viewModel.deleteMaintenance(maint); showConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = AlertRed)) {
                                                    Text("Excluir")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showConfirm = false }) { Text("Cancelar", color = SteelGray) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Expense registration dialog (Categories: Alimentação, Pedágio, Estacionamento, Outros)
    if (showAddExpenseDialog) {
        var expenseTitle by remember { mutableStateOf("") }
        var expenseCost by remember { mutableStateOf("") }
        var expenseCategory by remember { mutableStateOf("Alimentação") }
        var expenseNotes by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddExpenseDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = CarbonSurface, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Adicionar Nova Despesa", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Divider(color = Color(0xFF2C2C2E))

                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Título / Descrição") },
                        placeholder = { Text("Ex: Almoço churrascaria") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                    )

                    OutlinedTextField(
                        value = expenseCost,
                        onValueChange = { expenseCost = it },
                        label = { Text("Custo (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                    )

                    Text("Categoria", color = SteelGray, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Alimentação", "Pedágio", "Estacionamento", "Outros").forEach { cat ->
                            val selected = expenseCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) HighOctaneAmber else Color(0xFF2C2C2E))
                                    .clickable { expenseCategory = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, color = if (selected) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = expenseNotes,
                        onValueChange = { expenseNotes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddExpenseDialog = false }) { Text("Cancelar", color = SteelGray) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val costVal = expenseCost.toDoubleOrNull() ?: 0.0
                                if (expenseTitle.isNotBlank() && costVal > 0) {
                                    viewModel.addExpense(expenseTitle, costVal, expenseCategory, System.currentTimeMillis(), expenseNotes)
                                }
                                showAddExpenseDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber)
                        ) {
                            Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Simple dialog for Fuel
    if (showAddRefuelDialog) {
        var refuelOdo by remember { mutableStateOf("") }
        var refuelLiters by remember { mutableStateOf("") }
        var refuelPrice by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddRefuelDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = CarbonSurface, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Registrar Abastecimento", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Divider(color = Color(0xFF2C2C2E))

                    OutlinedTextField(value = refuelOdo, onValueChange = { refuelOdo = it }, label = { Text("Odômetro Atual (KM)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White))
                    OutlinedTextField(value = refuelLiters, onValueChange = { refuelLiters = it }, label = { Text("Litros Abastecidos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White))
                    OutlinedTextField(value = refuelPrice, onValueChange = { refuelPrice = it }, label = { Text("Preço por Litro (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddRefuelDialog = false }) { Text("Cancelar", color = SteelGray) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val odo = refuelOdo.toDoubleOrNull() ?: 0.0
                                val liters = refuelLiters.toDoubleOrNull() ?: 0.0
                                val price = refuelPrice.toDoubleOrNull() ?: 0.0
                                if (liters > 0 && price > 0) {
                                    viewModel.addRefuel(odo, liters, price)
                                }
                                showAddRefuelDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber)
                        ) {
                            Text("Registrar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Simple dialog for Maintenance
    if (showAddMaintDialog) {
        var maintTitle by remember { mutableStateOf("") }
        var maintType by remember { mutableStateOf("Troca de Óleo") }
        var maintOdo by remember { mutableStateOf("") }
        var maintCost by remember { mutableStateOf("") }
        var maintNotes by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddMaintDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = CarbonSurface, modifier = Modifier.padding(16.dp)) {
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Registrar Manutenção", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Divider(color = Color(0xFF2C2C2E), modifier = Modifier.padding(vertical = 6.dp))
                    }
                    item {
                        OutlinedTextField(value = maintTitle, onValueChange = { maintTitle = it }, label = { Text("Título da Manutenção") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White))
                    }
                    item {
                        Text("Tipo de Manutenção", color = SteelGray, fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Troca de Óleo", "Freios", "Outro").forEach { t ->
                                val sel = t == maintType
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) HighOctaneAmber else Color(0xFF2C2C2E))
                                        .clickable { maintType = t }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(t, color = if (sel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(value = maintOdo, onValueChange = { maintOdo = it }, label = { Text("Odômetro (KM)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White))
                    }
                    item {
                        OutlinedTextField(value = maintCost, onValueChange = { maintCost = it }, label = { Text("Custo Total (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White))
                    }
                    item {
                        OutlinedTextField(value = maintNotes, onValueChange = { maintNotes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White))
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddMaintDialog = false }) { Text("Cancelar", color = SteelGray) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val cost = maintCost.toDoubleOrNull() ?: 0.0
                                    val odo = maintOdo.toDoubleOrNull() ?: 0.0
                                    if (cost > 0) {
                                        viewModel.addMaintenance(maintType, maintTitle.ifBlank { maintType }, cost, maintNotes, odo)
                                    }
                                    showAddMaintDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber)
                            ) {
                                Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(title: String, color: Color, amount: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(title, color = SteelGray, fontSize = 11.sp)
            Text(String.format(Locale.getDefault(), "R$ %.1f", amount), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun EmptyLogsPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = SteelGray, fontSize = 13.sp)
    }
}

// ==========================================
// 8 & 9 & 10 & 16 & 18. REPORTS SCREEN (TELA DE RELATÓRIOS)
// ==========================================
@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val allDeliveries by viewModel.allDeliveries.collectAsState()

    var showReportType by remember { mutableStateOf("PDF") } // "PDF", "Text"
    var selectedEstablishmentFilter by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Active establishments with count (excluding zero delivery ones!)
    val activeEstsCounts = allDeliveries.groupBy { it.establishment.trim() }
        .mapValues { it.value.size }
        .filter { it.key.isNotBlank() && it.value > 0 }

    val filteredDeliveriesForReport = if (selectedEstablishmentFilter != null) {
        allDeliveries.filter { it.establishment.trim().equals(selectedEstablishmentFilter, ignoreCase = true) }
    } else {
        allDeliveries
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Relatórios de Atividades",
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = Color.White
        )

        // Switch Tab: PDF Visualizer vs. Text Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CarbonSurface)
                .padding(4.dp)
        ) {
            listOf("PDF", "Texto").forEach { rep ->
                val isSel = showReportType == rep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) HighOctaneAmber else Color.Transparent)
                        .clickable { showReportType = rep }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (rep == "PDF") "📄 Folha PDF Oficial" else "📝 Relatório em Texto",
                        color = if (isSel) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Filter report by establishment dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filtrar por Estabelecimento:", color = SteelGray, fontSize = 12.sp)
            var showEstMenu by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showEstMenu = true }
                        .background(CarbonSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedEstablishmentFilter ?: "Todos", color = HighOctaneAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = SteelGray, fontSize = 8.sp)
                }
                DropdownMenu(
                    expanded = showEstMenu,
                    onDismissRequest = { showEstMenu = false },
                    modifier = Modifier.background(CarbonSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos", color = Color.White) },
                        onClick = {
                            selectedEstablishmentFilter = null
                            showEstMenu = false
                        }
                    )
                    activeEstsCounts.keys.forEach { est ->
                        DropdownMenuItem(
                            text = { Text(est, color = Color.White) },
                            onClick = {
                                selectedEstablishmentFilter = est
                                showEstMenu = false
                            }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (showReportType == "PDF") {
                // 8. Visual representation of ONE-PAGE PDF sheet
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Document Mockup Sheet
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, SteelGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("MOTOGESTOR RELATÓRIOS", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 14.sp)
                                Text("FOLHA ÚNICA", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Divider(color = Color.Black, modifier = Modifier.padding(vertical = 6.dp))

                            // Driver Info
                            Text("Entregador: ${userProfile?.name?.ifEmpty { "GUSTAVO" }}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Telefone: ${userProfile?.phone?.ifEmpty { "(11) 99999-9999" }}", color = Color.Black, fontSize = 10.sp)
                            Text("Cidade: ${userProfile?.city?.ifEmpty { "São Paulo" }}", color = Color.Black, fontSize = 10.sp)
                            Text("Emissão: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", color = Color.Black, fontSize = 10.sp)
                            
                            Divider(color = Color.Black, modifier = Modifier.padding(vertical = 6.dp))

                            // Quantity of Deliveries
                            Text(
                                text = "Quantidade Total de Entregas: ${filteredDeliveriesForReport.size}",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Table Headers (Data, Estabelecimento, Forma de pagamento. VALUES COMPLETELY HIDDEN!)
                            Row(modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(4.dp)) {
                                Text("Data", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                                Text("Estabelecimento", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(2f))
                                Text("Pagamento", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                            }

                            // Table Rows (Take first 6 to fit beautiful single page preview)
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(filteredDeliveriesForReport.take(8)) { del ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp)) {
                                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(del.dateMillis))
                                        Text(dateStr, color = Color.Black, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                                        Text(del.establishment, color = Color.Black, fontSize = 10.sp, modifier = Modifier.weight(2f))
                                        Text(del.paymentMethod, color = Color.Black, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                                    }
                                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                                }
                                if (filteredDeliveriesForReport.size > 8) {
                                    item {
                                        Text("... e mais ${filteredDeliveriesForReport.size - 8} entregas listadas na folha.", color = Color.DarkGray, fontSize = 9.sp, modifier = Modifier.padding(6.dp))
                                    }
                                }
                            }

                            // Footer
                            Divider(color = Color.Black, modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Gerado automaticamente pelo MotoGestor.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Gray,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Report Action Buttons (9. Shrink "Gerar", Add "Compartilhar PDF" and "Exportar")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { Toast.makeText(context, "Relatório PDF gerado e salvo localmente!", Toast.LENGTH_SHORT).show() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                            modifier = Modifier.weight(1f).height(44.dp) // Shrunk button
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Gerar", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gerar PDF", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                // 18. Export: WhatsApp, email, etc. simulated intent sharing
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Relatório de Entregas - MotoGestor")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Relatório de ${userProfile?.name ?: "Entregador"}\nQuantidade total de entregas: ${filteredDeliveriesForReport.size}\nGerado via MotoGestor."
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Relatório"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber),
                            modifier = Modifier.weight(1.2f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartilhar PDF", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // 10. TEXT REPORT SCREEN (Shows active ones only, lists establishment + run count, hides gains)
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val textReportString = buildString {
                        append("=== RELATÓRIO MOTOBOY ===\n")
                        append("Entregador: ${userProfile?.name?.ifEmpty { "GUSTAVO" }}\n")
                        append("Moto: ${userProfile?.motorcycleModel?.ifEmpty { "Fazer 160" }}\n")
                        append("-----------------------\n")
                        if (activeEstsCounts.isEmpty()) {
                            append("Nenhum estabelecimento com entregas registradas.\n")
                        } else {
                            activeEstsCounts.forEach { (est, count) ->
                                append("$est\n")
                                append("$count entregas\n\n")
                            }
                        }
                        append("-----------------------\n")
                        append("Gerado via MotoGestor.")
                    }

                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                            Text(
                                text = textReportString,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(textReportString))
                                Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("Copiar Texto", color = Color.White)
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, textReportString)
                                    type = "text/plain"
                                    setPackage("com.whatsapp")
                                }
                                try {
                                    context.startActivity(sendIntent)
                                } catch (e: Exception) {
                                    // Fallback to standard share chooser if WhatsApp not installed
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, textReportString)
                                    }, "Enviar Relatório"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber),
                            modifier = Modifier.weight(1.2f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2 & 25. SETTINGS / PROFILE SCREEN (SINCRONIZAÇÃO E PERFIL DO ENTREGADOR)
// ==========================================
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isAutoBackup by viewModel.isAutoBackupEnabled.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Profile field inputs
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var cityInput by remember { mutableStateOf("") }
    var brandInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf("") }
    var plateInput by remember { mutableStateOf("") }
    var odoInput by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf("") }

    // Synchronize local UI state when DB profile loads
    LaunchedEffect(userProfile) {
        userProfile?.let {
            nameInput = it.name
            phoneInput = it.phone
            cityInput = it.city
            brandInput = it.motorcycleBrand
            modelInput = it.motorcycleModel
            yearInput = if (it.motorcycleYear > 0) it.motorcycleYear.toString() else "2024"
            plateInput = it.motorcyclePlate
            odoInput = if (it.currentOdometer > 0) String.format(Locale.US, "%.0f", it.currentOdometer) else "0"
            selectedPhotoUri = it.photoUri
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Perfil do Motoboy Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Meu Perfil de Entregador", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Divider(color = Color(0xFF2C2C2E))

                    // Avatar photo picker selection (Interactive Riders avatars!)
                    Text("Escolha sua Foto / Avatar", color = SteelGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AvatarOptions.forEach { pair ->
                            val isSelected = selectedPhotoUri == pair.first
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) HighOctaneAmber else Color(0xFF2C2C2E))
                                    .clickable { selectedPhotoUri = pair.first }
                                    .border(if (isSelected) 2.dp else 0.dp, HighOctaneAmber, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pair.first, fontSize = 22.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nome do Piloto") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Telefone") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                        )
                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            label = { Text("Cidade") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                        )
                    }

                    Text("Dados da Motocicleta", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = brandInput,
                            onValueChange = { brandInput = it },
                            label = { Text("Marca") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                        )
                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = { modelInput = it },
                            label = { Text("Modelo") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it },
                            label = { Text("Ano") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                        )
                        OutlinedTextField(
                            value = plateInput,
                            onValueChange = { plateInput = it },
                            label = { Text("Placa") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                        )
                    }

                    OutlinedTextField(
                        value = odoInput,
                        onValueChange = { odoInput = it },
                        label = { Text("Quilometragem Atual (KM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighOctaneAmber, unfocusedBorderColor = Color(0xFF2C2C2E), textColor = Color.White)
                    )

                    Button(
                        onClick = {
                            val yr = yearInput.toIntOrNull() ?: 2024
                            val odo = odoInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                            viewModel.updateProfile(
                                nameInput, phoneInput, cityInput, brandInput, modelInput, yr, plateInput, odo, selectedPhotoUri
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Salvar Perfil", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Sincronização / Backup Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Backup & Sincronização", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Mantenha seus dados seguros na nuvem", color = SteelGray, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.CloudSync, contentDescription = "Sincronização", tint = HighOctaneAmber, modifier = Modifier.size(28.dp))
                    }

                    Divider(color = Color(0xFF2C2C2E))

                    // Sync Status
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Última sincronização:", color = SteelGray, fontSize = 13.sp)
                        Text(lastSyncTime, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status:", color = SteelGray, fontSize = 13.sp)
                        Text(syncStatus, color = if (syncStatus == "Sincronizado") GreenPIX else HighOctaneAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (isSyncing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = HighOctaneAmber)
                    }

                    // Auto Backup Switch toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Backup Automático", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Sincroniza automaticamente a cada nova corrida", color = SteelGray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isAutoBackup,
                            onCheckedChange = { viewModel.toggleAutoBackup(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = HighOctaneAmber, checkedTrackColor = Color(0xFF2C2C2E))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncNow() },
                            colors = ButtonDefaults.buttonColors(containerColor = HighOctaneAmber),
                            modifier = Modifier.weight(1f).height(42.dp),
                            enabled = !isSyncing
                        ) {
                            Text("Sincronizar Agora", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.restoreBackup() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                            modifier = Modifier.weight(1f).height(42.dp),
                            enabled = !isSyncing
                        ) {
                            Text("Restaurar Backup", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
