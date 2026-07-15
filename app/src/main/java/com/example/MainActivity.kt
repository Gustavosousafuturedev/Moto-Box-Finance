package com.example

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.launch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.PopupProperties
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
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import android.graphics.Color as AndroidColor

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

        // Request Notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Schedule daily notification reminders at 00:00
        scheduleDailyNotification(this)

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
    val motoboyPhotoUri by viewModel.motoboyPhotoUri.collectAsStateWithLifecycle()
    val voiceDialogData by viewModel.voiceDialogData.collectAsStateWithLifecycle()
    val voiceProcessing by viewModel.voiceProcessing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Dirty form tracking states
    var isFormDirty by remember { mutableStateOf(false) }
    var pendingNavigationTarget by remember { mutableStateOf<Screen?>(null) }
    var showDiscardConfirmationDialog by remember { mutableStateOf(false) }

    val safeNavigateTo: (Screen) -> Unit = { targetScreen ->
        if (targetScreen == currentScreen) {
            // Already there
        } else if (isFormDirty) {
            pendingNavigationTarget = targetScreen
            showDiscardConfirmationDialog = true
        } else {
            viewModel.navigateTo(targetScreen)
        }
    }

    // Intercept system back button if any form is dirty
    androidx.activity.compose.BackHandler(enabled = isFormDirty) {
        pendingNavigationTarget = Screen.Dashboard
        showDiscardConfirmationDialog = true
    }

    // Dialog state variables
    var showAddDeliveryDialog by remember { mutableStateOf(false) }
    var editingDelivery by remember { mutableStateOf<Delivery?>(null) }
    var showPartnersDialog by remember { mutableStateOf(false) }
    val establishments by viewModel.establishments.collectAsStateWithLifecycle()
    val distinctEstablishments by viewModel.distinctEstablishments.collectAsStateWithLifecycle()

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
                motoboyPhotoUri = motoboyPhotoUri,
                alertCount = odometerAlerts.size,
                onAlertClick = { safeNavigateTo(Screen.ControleManutencao) },
                onNavigateConfig = { safeNavigateTo(Screen.Configuracoes) },
                onBackupClick = {
                    viewModel.triggerBackup(context) { success, msg ->
                        if (success) {
                            Toast.makeText(context, "Backup offline salvo com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Erro ao realizar backup: $msg", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentScreen = currentScreen,
                onTabSelect = { safeNavigateTo(it) }
            )
        },
        floatingActionButton = {
            if (currentScreen == Screen.Dashboard || currentScreen == Screen.CadastroEntrega) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Botão de Parceiros (#bf41ce)
                    FloatingActionButton(
                        onClick = { showPartnersDialog = true },
                        containerColor = Color(0xFFBF41CE),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = "Parceiros", modifier = Modifier.size(24.dp))
                    }

                    // Botão de Lançar Entrega (#42ff00)
                    FloatingActionButton(
                        onClick = { showAddDeliveryDialog = true },
                        containerColor = Color(0xFF42FF00),
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Lançar Entrega", modifier = Modifier.size(28.dp))
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
                Screen.CadastroEntrega -> DeliveriesScreen(viewModel, onEditDelivery = { editingDelivery = it })
                Screen.CadastroEstabelecimento -> EstablishmentsScreen(viewModel)
                Screen.ControleCombustivel -> FuelScreen(viewModel, onFormDirtyChange = { isFormDirty = it })
                Screen.ControleDespesas -> ExpensesScreen(viewModel, onFormDirtyChange = { isFormDirty = it })
                Screen.ControleManutencao -> MaintenanceScreen(viewModel, onFormDirtyChange = { isFormDirty = it })
                Screen.Metas -> GoalsScreen(viewModel)
                Screen.FechamentoCaixa -> RegisterCloseScreen(viewModel)
                Screen.Relatorios -> ReportsScreen(viewModel)
                Screen.RelatorioEstabelecimento -> PartnerReportScreen(viewModel)
                Screen.Configuracoes -> SettingsScreen(viewModel, onFormDirtyChange = { isFormDirty = it })
                Screen.ExportarRelatorio -> ExportarRelatorioScreen(viewModel)
            }

            // Discard warning confirmation Dialog
            if (showDiscardConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardConfirmationDialog = false },
                    title = { Text("Descartar alterações?") },
                    text = { Text("Você preencheu algumas informações neste formulário. Se sair agora, as alterações não salvas serão perdidas.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDiscardConfirmationDialog = false
                                isFormDirty = false
                                val target = pendingNavigationTarget
                                if (target != null) {
                                    viewModel.navigateTo(target)
                                } else {
                                    viewModel.navigateTo(Screen.Dashboard)
                                }
                            }
                        ) {
                            Text("Descartar")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDiscardConfirmationDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
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
                            Text("A inteligência artificial do NuCorre preencheu os campos para você. Confirme os dados antes de salvar:")
                            
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

    if (showAddDeliveryDialog) {
        AddEditDeliveryDialog(
            delivery = null,
            establishments = establishments,
            distinctEstablishments = distinctEstablishments,
            onDismiss = { showAddDeliveryDialog = false },
            onSave = { name, neigh, cty, valPrice, payMet, dist, cliName, notesText, qty, fee, feeFarther ->
                viewModel.insertDelivery(
                    establishmentName = name,
                    neighborhood = neigh,
                    city = cty,
                    value = valPrice,
                    paymentMethod = payMet,
                    distanceKm = dist,
                    clientName = cliName,
                    notes = notesText,
                    quantity = qty,
                    feePerDelivery = fee,
                    feeFartherDeliveries = feeFarther
                )
                showAddDeliveryDialog = false
                Toast.makeText(context, "Entrega lançada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingDelivery?.let { delivery ->
        AddEditDeliveryDialog(
            delivery = delivery,
            establishments = establishments,
            distinctEstablishments = distinctEstablishments,
            onDismiss = { editingDelivery = null },
            onSave = { name, neigh, cty, valPrice, payMet, dist, cliName, notesText, qty, fee, feeFarther ->
                viewModel.updateDelivery(
                    id = delivery.id,
                    date = delivery.date,
                    time = delivery.time,
                    establishmentName = name,
                    neighborhood = neigh,
                    city = cty,
                    value = valPrice,
                    paymentMethod = payMet,
                    distanceKm = dist,
                    clientName = cliName,
                    notes = notesText,
                    quantity = qty,
                    feePerDelivery = fee,
                    feeFartherDeliveries = feeFarther
                )
                editingDelivery = null
                Toast.makeText(context, "Entrega atualizada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPartnersDialog) {
        AlertDialog(
            onDismissRequest = { showPartnersDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gerenciar Parceiros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showPartnersDialog = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar")
                    }
                }
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 500.dp)) {
                    EstablishmentsScreen(viewModel = viewModel)
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeliveryDialog(
    delivery: com.example.data.Delivery? = null,
    establishments: List<com.example.data.Establishment>,
    distinctEstablishments: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        establishmentName: String,
        neighborhood: String,
        city: String,
        value: Double,
        paymentMethod: String,
        distanceKm: Double,
        clientName: String?,
        notes: String,
        quantity: Int,
        feePerDelivery: Double,
        feeFartherDeliveries: Double
    ) -> Unit
) {
    var estName by remember { mutableStateOf(delivery?.establishmentName ?: "") }
    val context = LocalContext.current
    var clientName by remember { mutableStateOf(delivery?.clientName ?: "") }
    var neighborhood by remember { mutableStateOf(delivery?.neighborhood ?: "") }
    var city by remember { mutableStateOf(delivery?.city ?: "São Paulo") }
    var quantityStr by remember { mutableStateOf(delivery?.quantity?.toString() ?: "1") }
    
    val initialFee = if (delivery != null) {
        if (delivery.feePerDelivery > 0.0) delivery.feePerDelivery else (delivery.value / (delivery.quantity.coerceAtLeast(1)))
    } else {
        0.0
    }
    
    var feePerDeliveryStr by remember { mutableStateOf(if (initialFee > 0.0) initialFee.toString() else "") }
    var feeFartherDeliveriesStr by remember { mutableStateOf(delivery?.feeFartherDeliveries?.toString() ?: "") }
    var distanceStr by remember { mutableStateOf(delivery?.distanceKm?.toString() ?: "") }
    var paymentMethod by remember { mutableStateOf(delivery?.paymentMethod ?: "Pix") }
    var notes by remember { mutableStateOf(delivery?.notes ?: "") }

    var showSuggestions by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(estName, distinctEstablishments) {
        if (estName.isNotBlank()) {
            distinctEstablishments.filter {
                it.contains(estName, ignoreCase = true) && !it.equals(estName, ignoreCase = true)
            }
        } else {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (delivery == null) "Lançar Nova Entrega" else "Editar Entrega",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = estName,
                        onValueChange = { 
                            estName = it
                            showSuggestions = true
                        },
                        label = { Text("Nome do Estabelecimento *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = showSuggestions && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { showSuggestions = false },
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    estName = suggestion
                                    showSuggestions = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Qtd. Entregas *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = feePerDeliveryStr,
                        onValueChange = { feePerDeliveryStr = it },
                        label = { Text("Taxa/Entrega (R$) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = feeFartherDeliveriesStr,
                        onValueChange = { feeFartherDeliveriesStr = it },
                        label = { Text("Taxa Distante (R$) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = distanceStr,
                        onValueChange = { distanceStr = it },
                        label = { Text("Distância (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val q = quantityStr.toIntOrNull() ?: 1
                    val f = feePerDeliveryStr.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val fd = feeFartherDeliveriesStr.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val totalVal = (q * f) + fd
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Valor Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("R$ %.2f".format(totalVal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qClean = quantityStr.trim()
                    val fClean = feePerDeliveryStr.trim()
                    val fdClean = feeFartherDeliveriesStr.trim()
                    
                    val quantity = qClean.toIntOrNull()
                    val fee = fClean.replace(',', '.').toDoubleOrNull()
                    val feeFarther = fdClean.replace(',', '.').toDoubleOrNull()
                    val distance = if (distanceStr.isBlank()) 0.0 else (distanceStr.replace(',', '.').toDoubleOrNull() ?: 0.0)

                    if (estName.trim().isEmpty()) {
                        Toast.makeText(context, "Nome do estabelecimento é obrigatório", Toast.LENGTH_SHORT).show()
                    } else if (quantity == null) {
                        Toast.makeText(context, "Quantidade de entrega é obrigatória", Toast.LENGTH_SHORT).show()
                    } else if (fee == null) {
                        Toast.makeText(context, "Taxa de entrega é obrigatória", Toast.LENGTH_SHORT).show()
                    } else if (feeFarther == null) {
                        Toast.makeText(context, "Taxa distante/de distância é obrigatória", Toast.LENGTH_SHORT).show()
                    } else {
                        val value = (quantity * fee) + feeFarther
                        onSave(
                            estName.trim(),
                            neighborhood,
                            city,
                            value,
                            paymentMethod,
                            distance,
                            clientName.ifEmpty { null },
                            notes,
                            quantity,
                            fee,
                            feeFarther
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// --- TOP HEADER TOOLBAR ---
@Composable
fun HeaderBar(
    motoboyName: String,
    motoboyPhotoUri: String = "",
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
                if (motoboyPhotoUri.isNotEmpty()) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(model = motoboyPhotoUri),
                        contentDescription = "Foto do Motoboy",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_nucorre_logo_1784120333345),
                        contentDescription = "Logo NuCorre",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NuCorre",
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
fun DeliveriesScreen(
    viewModel: MainViewModel,
    onEditDelivery: (Delivery) -> Unit
) {
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val filteredDeliveries = remember(deliveries, searchQuery) {
        if (searchQuery.isEmpty()) {
            deliveries
        } else {
            deliveries.filter { d ->
                d.establishmentName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Histórico List with filters
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filtrar por estabelecimento") },
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
                        DeliveryItemCard(
                            delivery = delivery,
                            onEdit = { onEditDelivery(delivery) },
                            onDelete = { viewModel.deleteDelivery(delivery) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryItemCard(delivery: Delivery, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = delivery.establishmentName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .wrapContentWidth()
                    ) {
                        Text(
                            text = delivery.paymentMethod,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text("Distância: ${delivery.distanceKm} km | Hora: ${delivery.time}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                if (delivery.notes.isNotEmpty()) {
                    Text(
                        text = "Obs: ${delivery.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
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
    val establishments by viewModel.establishments.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingEstId by remember { mutableIntStateOf(0) }
    
    // Form States
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Bom Jesus da Lapa") }
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.wrapContentWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Adicionar",
                    maxLines = 1,
                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                    overflow = TextOverflow.Ellipsis
                )
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
                            Text("${item.deliveriesCount} entregas", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(estStats) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                val originalEst = establishments.find { it.id == item.id }
                                if (originalEst != null) {
                                    IconButton(
                                        onClick = {
                                            editingEstId = originalEst.id
                                            name = originalEst.name
                                            address = originalEst.address
                                            neighborhood = originalEst.neighborhood
                                            city = originalEst.city
                                            phone = originalEst.phone
                                            contact = originalEst.contact
                                            notes = originalEst.notes
                                            showEditDialog = true
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = "Editar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteEstablishment(originalEst)
                                            Toast.makeText(context, "Estabelecimento excluído!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Excluir",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
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
                    },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = "Adicionar",
                        maxLines = 1,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = "Cancelar",
                        maxLines = 1,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Estabelecimento") },
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
                            viewModel.updateEstablishment(editingEstId, name, address, neighborhood, city, phone, contact, notes)
                            Toast.makeText(context, "Estabelecimento atualizado!", Toast.LENGTH_SHORT).show()
                            showEditDialog = false
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
                    },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = "Salvar",
                        maxLines = 1,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showEditDialog = false 
                        // Reset
                        name = ""
                        address = ""
                        neighborhood = ""
                        phone = ""
                        contact = ""
                        notes = ""
                    },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = "Cancelar",
                        maxLines = 1,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }
}

// =====================================================
// TELA 3: CONTROLE DE COMBUSTÍVEL
// =====================================================
@Composable
fun FuelScreen(viewModel: MainViewModel, onFormDirtyChange: (Boolean) -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        FuelScreenContent(viewModel, onFormDirtyChange)
    }
}

@Composable
fun FuelScreenContent(viewModel: MainViewModel, onFormDirtyChange: (Boolean) -> Unit = {}) {
    val fuelLogs by viewModel.fuelLogs.collectAsStateWithLifecycle()
    val stats by viewModel.fuelStats.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var amountStr by remember { mutableStateOf("") }
    var litersStr by remember { mutableStateOf("") }
    var odometerStr by remember { mutableStateOf("") }
    var gasStation by remember { mutableStateOf("") }

    // Track dirty state of the form
    LaunchedEffect(gasStation, amountStr, litersStr, odometerStr) {
        val dirty = gasStation.isNotEmpty() || amountStr.isNotEmpty() || litersStr.isNotEmpty() || odometerStr.isNotEmpty()
        onFormDirtyChange(dirty)
    }

    val isAmountValid = amountStr.replace(',', '.').toDoubleOrNull()?.let { it > 0 } ?: false
    val isLitersValid = litersStr.replace(',', '.').toDoubleOrNull()?.let { it > 0 } ?: false
    val isOdometerValid = odometerStr.replace(',', '.').toDoubleOrNull()?.let { it > 0 } ?: false
    val isFuelFormValid = gasStation.isNotBlank() && isAmountValid && isLitersValid && isOdometerValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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
                    isError = gasStation.isNotEmpty() && gasStation.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Valor Gasto (R$) *") },
                        isError = amountStr.isNotEmpty() && !isAmountValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = litersStr,
                        onValueChange = { litersStr = it },
                        label = { Text("Litros *") },
                        isError = litersStr.isNotEmpty() && !isLitersValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = odometerStr,
                    onValueChange = { odometerStr = it },
                    label = { Text("Quilometragem Atual (Odom.) *") },
                    isError = odometerStr.isNotEmpty() && !isOdometerValid,
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
                    enabled = isFuelFormValid,
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
// TELA 4: CONTROLE DE DESPESAS (Com aba Combustível)
// =====================================================
@Composable
fun ExpensesScreen(viewModel: MainViewModel, onFormDirtyChange: (Boolean) -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Geral") },
                icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Combustível") },
                icon = { Icon(Icons.Filled.LocalGasStation, contentDescription = null) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> GeneralExpensesScreenContent(viewModel, onFormDirtyChange)
                1 -> FuelScreenContent(viewModel, onFormDirtyChange)
            }
        }
    }
}

@Composable
fun GeneralExpensesScreenContent(viewModel: MainViewModel, onFormDirtyChange: (Boolean) -> Unit = {}) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var valueStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Troca de óleo") }

    val categories = listOf(
        "Combustível", "Troca de óleo", "Filtro de óleo", "Pneu dianteiro", 
        "Pneu traseiro", "Câmara de ar", "Corrente", "Coroa", "Pinhão", 
        "Freios", "Embreagem", "Oficina", "Lavagem", "Alimentação", "Seguro", "IPVA", 
        "Licenciamento", "Multas", "Outros"
    )

    // Track dirty state of the form
    LaunchedEffect(valueStr, notes) {
        val dirty = valueStr.isNotEmpty() || notes.isNotEmpty()
        onFormDirtyChange(dirty)
    }

    val isValueValid = valueStr.replace(',', '.').toDoubleOrNull()?.let { it > 0 } ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Controle de Despesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Form to add Expense
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Lançar Nova Despesa", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Valor (R$) *") },
                    isError = valueStr.isNotEmpty() && !isValueValid,
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
                    enabled = isValueValid,
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
fun MaintenanceScreen(viewModel: MainViewModel, onFormDirtyChange: (Boolean) -> Unit = {}) {
    val maintenances by viewModel.maintenances.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var item by remember { mutableStateOf("Troca de óleo") }
    var currentOdomStr by remember { mutableStateOf("") }
    var nextOdomStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val items = listOf("Troca de óleo", "Filtro", "Vela", "Corrente", "Coroa", "Pinhão", "Freios", "Embreagem", "Bateria", "Pneu dianteiro", "Pneu trasei.", "Outros")

    // Track dirty state of the form
    LaunchedEffect(currentOdomStr, nextOdomStr, costStr, notes) {
        val dirty = currentOdomStr.isNotEmpty() || nextOdomStr.isNotEmpty() || costStr.isNotEmpty() || notes.isNotEmpty()
        onFormDirtyChange(dirty)
    }

    val isCurrentOdomValid = currentOdomStr.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } ?: false
    val isNextOdomValid = nextOdomStr.replace(',', '.').toDoubleOrNull()?.let { it > 0 } ?: false
    val isCostValid = costStr.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } ?: false
    val isMaintenanceFormValid = isCurrentOdomValid && isNextOdomValid && isCostValid

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
                        isError = currentOdomStr.isNotEmpty() && !isCurrentOdomValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nextOdomStr,
                        onValueChange = { nextOdomStr = it },
                        label = { Text("Próx. Km (Aviso) *") },
                        isError = nextOdomStr.isNotEmpty() && !isNextOdomValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Custo Manutenção (R$) *") },
                    isError = costStr.isNotEmpty() && !isCostValid,
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
                    enabled = isMaintenanceFormValid,
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
        === 🏍️ NuCorre - FECHAMENTO ===
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
        Gerado pelo NuCorre offline.
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
                Text("Exportar Relatório Profissional", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Gere relatórios customizados em formato PDF e Excel (.xlsx) com tabelas de entregas, faturamento bruto e resumos por estabelecimento.", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { viewModel.navigateTo(Screen.ExportarRelatorio) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Relatório PDF / Excel")
                }
            }
        }

        // Copyable text report panel
        val reportText = remember(selectedPeriod, stats, deliveries) {
            val count = when (selectedPeriod) {
                "Diário" -> deliveries.filter { android.text.format.DateUtils.isToday(it.date) }.sumOf { it.quantity }
                else -> deliveries.sumOf { it.quantity } // fallback
            }
            val periodDeliveries = when (selectedPeriod) {
                "Diário" -> deliveries.filter { android.text.format.DateUtils.isToday(it.date) }
                else -> deliveries
            }
            val estsList = periodDeliveries.map { it.establishmentName }.distinct().sorted()
            val estsText = if (estsList.isEmpty()) {
                "Nenhum estabelecimento"
            } else {
                estsList.joinToString("\n") { "• $it" }
            }
            
            """
            *NuCorre - Relatório*
            Período: $selectedPeriod
            ------------------------------
            Total de Entregas: $count
            ------------------------------
            Estabelecimentos:
            $estsText
            ------------------------------
            Gerado em: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
            """.trimIndent()
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Relatório em Texto (Copiável)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = reportText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Relatório NuCorre", reportText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Relatório copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar Relatório")
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
                Text("NuCorre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
            onClick = { exportEstablishmentReportToPdf(context, selectedEst, estStats) },
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
fun SettingsScreen(viewModel: MainViewModel, onFormDirtyChange: (Boolean) -> Unit = {}) {
    val name by viewModel.motoboyName.collectAsStateWithLifecycle()
    val phone by viewModel.motoboyPhone.collectAsStateWithLifecycle()
    val darkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val autoTheme by viewModel.useAutoTheme.collectAsStateWithLifecycle()
    val photoUri by viewModel.motoboyPhotoUri.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editName by remember { mutableStateOf(name) }
    var editPhone by remember { mutableStateOf(phone) }

    var editDayGoal by remember { mutableStateOf("") }
    var editWeekGoal by remember { mutableStateOf("") }
    var editMonthGoal by remember { mutableStateOf("") }

    LaunchedEffect(goals) {
        val dg = goals.find { it.type == "Diária" }?.targetValue ?: 100.0
        val wg = goals.find { it.type == "Semanal" }?.targetValue ?: 600.0
        val mg = goals.find { it.type == "Mensal" }?.targetValue ?: 2400.0

        editDayGoal = dg.toInt().toString()
        editWeekGoal = wg.toInt().toString()
        editMonthGoal = mg.toInt().toString()
    }

    // Synchronize local edit fields when state updates from ViewModel (e.g. database loading or backup restore)
    LaunchedEffect(name, phone) {
        editName = name
        editPhone = phone
    }

    // Track dirty state of the form
    LaunchedEffect(editName, editPhone, name, phone) {
        val dirty = (editName != name && editName.isNotEmpty()) || (editPhone != phone && editPhone.isNotEmpty())
        onFormDirtyChange(dirty)
    }

    val isProfileValid = editName.isNotBlank() && editPhone.isNotBlank()
    val hasChanges = editName != name || editPhone != phone

    val photoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Ignore if permission not persistable
            }
            viewModel.updateSettings(editName, editPhone, darkTheme, autoTheme, it.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configurações do NuCorre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Perfil do Motoboy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = androidx.compose.foundation.shape.CircleShape)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri.isNotEmpty()) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(model = photoUri),
                                contentDescription = "Foto do Entregador",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Sem foto",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Column {
                        Text("Foto do Entregador", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (photoUri.isNotEmpty()) "Toque na foto para alterar" else "Toque para adicionar uma foto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nome do Motoboy *") },
                    isError = editName.isNotEmpty() && editName.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text("Telefone / WhatsApp *") },
                    isError = editPhone.isNotEmpty() && editPhone.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (isProfileValid) {
                            viewModel.updateSettings(editName, editPhone, darkTheme, autoTheme, photoUri)
                            Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isProfileValid && hasChanges,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Perfil")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Minhas Metas de Faturamento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = editDayGoal,
                    onValueChange = { editDayGoal = it },
                    label = { Text("Meta Diária (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editWeekGoal,
                    onValueChange = { editWeekGoal = it },
                    label = { Text("Meta Semanal (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editMonthGoal,
                    onValueChange = { editMonthGoal = it },
                    label = { Text("Meta Mensal (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                val isGoalsValid = editDayGoal.replace(',', '.').toDoubleOrNull() != null &&
                                   editWeekGoal.replace(',', '.').toDoubleOrNull() != null &&
                                   editMonthGoal.replace(',', '.').toDoubleOrNull() != null

                val currentDg = goals.find { it.type == "Diária" }?.targetValue ?: 100.0
                val currentWg = goals.find { it.type == "Semanal" }?.targetValue ?: 600.0
                val currentMg = goals.find { it.type == "Mensal" }?.targetValue ?: 2400.0

                val goalsChanged = editDayGoal.replace(',', '.').toDoubleOrNull() != currentDg ||
                                   editWeekGoal.replace(',', '.').toDoubleOrNull() != currentWg ||
                                   editMonthGoal.replace(',', '.').toDoubleOrNull() != currentMg

                Button(
                    onClick = {
                        val d = editDayGoal.replace(',', '.').toDoubleOrNull()
                        val w = editWeekGoal.replace(',', '.').toDoubleOrNull()
                        val m = editMonthGoal.replace(',', '.').toDoubleOrNull()

                        if (d != null && w != null && m != null) {
                            viewModel.updateGoal("Diária", d)
                            viewModel.updateGoal("Semanal", w)
                            viewModel.updateGoal("Mensal", m)
                            Toast.makeText(context, "Metas de faturamento atualizadas!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isGoalsValid && goalsChanged,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Metas")
                }
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
                        viewModel.triggerBackup(context) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "Backup offline realizado com sucesso! Arquivo: $msg", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Erro ao realizar backup: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fazer Backup Offline (JSON)")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.triggerRestore(context) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "Backup restaurado com sucesso!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Erro ao restaurar backup: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar Backup Offline (JSON)")
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

// =====================================================
// EXPORT HELPERS (PDF / CSV)
// =====================================================
fun saveFileToDownloads(
    context: android.content.Context,
    fileName: String,
    mimeType: String,
    writeBlock: (OutputStream) -> Unit
): Uri? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    writeBlock(outputStream)
                }
            }
            uri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                writeBlock(outputStream)
            }
            Uri.fromFile(file)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun exportDeliveriesToCsv(context: android.content.Context, deliveries: List<Delivery>) {
    val fileName = "nucorre_entregas_${System.currentTimeMillis()}.csv"
    val resultUri = saveFileToDownloads(context, fileName, "text/csv") { outputStream ->
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val writer = outputStream.bufferedWriter(Charsets.UTF_8)
        writer.write("ID;Data;Hora;Estabelecimento;Cliente;Bairro;Cidade;Valor;Forma Pagamento;Distancia (km);Tempo (min);Notas;Quantidade;Taxa\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        deliveries.forEach { d ->
            val dateStr = sdf.format(Date(d.date))
            val client = d.clientName ?: ""
            val time = d.time
            val est = d.establishmentName
            val neighborhood = d.neighborhood
            val city = d.city
            val value = "%.2f".format(d.value).replace(".", ",")
            val pay = d.paymentMethod
            val dist = "%.1f".format(d.distanceKm).replace(".", ",")
            val duration = d.deliveryTimeMinutes?.toString() ?: ""
            val notes = d.notes.replace("\n", " ").replace(";", ",")
            val qty = d.quantity
            val fee = "%.2f".format(d.feePerDelivery).replace(".", ",")
            writer.write("${d.id};$dateStr;$time;$est;$client;$neighborhood;$city;$value;$pay;$dist;$duration;$notes;$qty;$fee\n")
        }
        writer.flush()
    }
    if (resultUri != null) {
        Toast.makeText(context, "Planilha Excel criada e salva nos Downloads!", Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(context, "Falha ao exportar planilha Excel.", Toast.LENGTH_SHORT).show()
    }
}

fun exportDeliveriesToPdf(context: android.content.Context, deliveries: List<Delivery>) {
    val fileName = "nucorre_relatorio_geral_${System.currentTimeMillis()}.pdf"
    val resultUri = saveFileToDownloads(context, fileName, "application/pdf") { outputStream ->
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val textPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        
        val titlePaint = Paint().apply {
            color = AndroidColor.parseColor("#aa00fa")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val headerPaint = Paint().apply {
            color = AndroidColor.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val valuePaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = AndroidColor.LTGRAY
            strokeWidth = 1f
        }
        
        var y = 50f
        canvas.drawText("NUCORRE", 40f, y, titlePaint)
        y += 25f
        
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Relatório Geral de Entregas - Gerado em ${sdf.format(Date())}", 40f, y, textPaint.apply { textSize = 10f })
        y += 30f
        
        val totalDeliveries = deliveries.size
        val totalEarnings = deliveries.sumOf { it.value }
        val totalDistance = deliveries.sumOf { it.distanceKm }
        
        canvas.drawRect(40f, y, 555f, y + 50f, Paint().apply { color = AndroidColor.parseColor("#F3E5F5") })
        canvas.drawText("Total de Entregas: $totalDeliveries", 50f, y + 20f, textPaint.apply { typeface = Typeface.DEFAULT_BOLD; textSize = 11f; color = AndroidColor.parseColor("#aa00fa") })
        canvas.drawText("Total Recebido: R$ %.2f".format(totalEarnings), 220f, y + 20f, textPaint)
        canvas.drawText("Distância: %.1f km".format(totalDistance), 420f, y + 20f, textPaint)
        y += 70f
        
        canvas.drawText("Data", 40f, y, headerPaint)
        canvas.drawText("Estabelecimento", 110f, y, headerPaint)
        canvas.drawText("Bairro", 260f, y, headerPaint)
        canvas.drawText("Forma Pag.", 380f, y, headerPaint)
        canvas.drawText("Dist (km)", 460f, y, headerPaint)
        canvas.drawText("Valor", 510f, y, headerPaint)
        
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 15f
        
        val rowSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        deliveries.forEachIndexed { index, d ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                y = 50f
                
                canvas.drawText("Data", 40f, y, headerPaint)
                canvas.drawText("Estabelecimento", 110f, y, headerPaint)
                canvas.drawText("Bairro", 260f, y, headerPaint)
                canvas.drawText("Forma Pag.", 380f, y, headerPaint)
                canvas.drawText("Dist (km)", 460f, y, headerPaint)
                canvas.drawText("Valor", 510f, y, headerPaint)
                y += 10f
                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 15f
            }
            
            val dateStr = rowSdf.format(Date(d.date))
            canvas.drawText(dateStr, 40f, y, valuePaint)
            
            val estTrunc = if (d.establishmentName.length > 22) d.establishmentName.take(20) + ".." else d.establishmentName
            canvas.drawText(estTrunc, 110f, y, valuePaint)
            
            val neighborhoodTrunc = if (d.neighborhood.length > 18) d.neighborhood.take(16) + ".." else d.neighborhood
            canvas.drawText(neighborhoodTrunc, 260f, y, valuePaint)
            
            canvas.drawText(d.paymentMethod, 380f, y, valuePaint)
            canvas.drawText("%.1f".format(d.distanceKm), 460f, y, valuePaint)
            canvas.drawText("R$ %.2f".format(d.value), 510f, y, valuePaint)
            
            y += 8f
            canvas.drawLine(40f, y, 555f, y, Paint().apply { color = AndroidColor.parseColor("#EEEEEE"); strokeWidth = 0.5f })
            y += 12f
        }
        
        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
    if (resultUri != null) {
        Toast.makeText(context, "Relatório em PDF gerado nos Downloads!", Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(context, "Falha ao exportar relatório PDF.", Toast.LENGTH_SHORT).show()
    }
}

fun exportEstablishmentReportToPdf(
    context: android.content.Context,
    selectedEst: String,
    estStats: List<com.example.ui.EstablishmentStats>
) {
    val fileName = "nucorre_relatorio_parceiro_${System.currentTimeMillis()}.pdf"
    val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    if (!documentsDir.exists()) {
        documentsDir.mkdirs()
    }
    val file = File(documentsDir, fileName)

    try {
        FileOutputStream(file).use { outputStream ->
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val titlePaint = Paint().apply {
                color = AndroidColor.parseColor("#aa00fa")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = AndroidColor.DKGRAY
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 10f
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = AndroidColor.LTGRAY
                strokeWidth = 1f
            }
            
            var y = 50f
            canvas.drawText("NUCORRE", 40f, y, titlePaint)
            y += 25f
            
            canvas.drawText("Relatório de Entregas por Estabelecimento", 40f, y, textPaint.apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD })
            y += 15f
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            canvas.drawText("Emissão: ${sdf.format(Date())}", 40f, y, textPaint.apply { textSize = 9f; typeface = Typeface.DEFAULT })
            y += 30f
            
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f
            
            if (selectedEst == "Todos") {
                canvas.drawText("Resumo de Todos os Estabelecimentos", 40f, y, headerPaint)
                y += 25f
                
                estStats.forEach { item ->
                    canvas.drawText(item.name, 40f, y, textPaint)
                    canvas.drawText("${item.deliveriesCount} entregas | R$ %.2f".format(item.totalEarnings), 350f, y, textPaint)
                    y += 20f
                    canvas.drawLine(40f, y, 555f, y, Paint().apply { color = AndroidColor.parseColor("#EEEEEE") })
                    y += 5f
                }
            } else {
                val est = estStats.find { it.name == selectedEst }
                if (est != null) {
                    canvas.drawText("Estabelecimento: ${est.name}", 40f, y, headerPaint)
                    y += 25f
                    canvas.drawText("Quantidade total de entregas: ${est.deliveriesCount}", 40f, y, textPaint)
                    y += 20f
                    canvas.drawText("Valor total recebido: R$ %.2f".format(est.totalEarnings), 40f, y, textPaint)
                    y += 20f
                    canvas.drawText("Última entrega cadastrada: ${est.lastDelivery}", 40f, y, textPaint)
                    y += 25f
                }
            }
            
            y += 10f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f
            
            val totalEnt = if (selectedEst == "Todos") estStats.sumOf { it.deliveriesCount } else estStats.find { it.name == selectedEst }?.deliveriesCount ?: 0
            val totalRec = if (selectedEst == "Todos") estStats.sumOf { it.totalEarnings } else estStats.find { it.name == selectedEst }?.totalEarnings ?: 0.0
            
            canvas.drawText("TOTAL DE ENTREGAS: $totalEnt", 40f, y, headerPaint)
            y += 20f
            canvas.drawText("TOTAL RECEBIDO: R$ %.2f".format(totalRec), 40f, y, headerPaint.apply { color = AndroidColor.parseColor("#aa00fa") })
            
            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        Toast.makeText(context, "PDF gerado com sucesso!", Toast.LENGTH_LONG).show()
        
        ExportHelpers.openFile(context, uri, "application/pdf")
    } catch (e: Exception) {
        Log.e("MainActivity", "Erro ao gerar PDF do estabelecimento", e)
        Toast.makeText(context, "Falha ao gerar PDF do parceiro.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarRelatorioScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()
    val establishments by viewModel.establishments.collectAsStateWithLifecycle()
    val motoboyName by viewModel.motoboyName.collectAsStateWithLifecycle()
    val motoboyPhone by viewModel.motoboyPhone.collectAsStateWithLifecycle()
    val motoboyCity by viewModel.motoboyCity.collectAsStateWithLifecycle()

    var period by remember { mutableStateOf("Hoje") }
    val periods = listOf("Hoje", "Ontem", "Esta semana", "Este mês", "Personalizado")

    var customStart by remember { mutableStateOf<Long?>(null) }
    var customEnd by remember { mutableStateOf<Long?>(null) }

    var filterByEst by remember { mutableStateOf(false) } // false = Todos, true = Apenas um
    var selectedEstName by remember { mutableStateOf("") }
    var estDropdownExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var exportResultUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var exportResultType by remember { mutableStateOf("") } // "pdf" or "xlsx"
    var exportResultFileName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Date pickers helpers
    val calendar = Calendar.getInstance()
    val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val startDatePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth, 0, 0, 0)
            customStart = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val endDatePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth, 23, 59, 59)
            customEnd = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Set default establishment when list is available
    LaunchedEffect(establishments) {
        if (establishments.isNotEmpty() && selectedEstName.isEmpty()) {
            selectedEstName = establishments.first().name
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Relatorios) }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = "Exportar Relatório",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Subtitle Info
        Text(
            text = "Gere relatórios profissionais detalhados das suas entregas diretamente para a memória do seu aparelho em formato PDF ou planilha do Excel.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Card 1: Filtros de Período e Estabelecimentos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "1. Configurar Filtros",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Period Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Período do Relatório",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        periods.forEach { p ->
                            FilterChip(
                                selected = period == p,
                                onClick = { 
                                    period = p 
                                    exportResultUri = null // Reset previous generation when filter changes
                                },
                                label = { Text(p) }
                            )
                        }
                    }
                }

                // Custom Date Range Pickers (only shown if period is Personalizado)
                if (period == "Personalizado") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { startDatePickerDialog.show() },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = customStart?.let { "De: " + sdfDate.format(Date(it)) } ?: "Data Inicial",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = { endDatePickerDialog.show() },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = customEnd?.let { "Até: " + sdfDate.format(Date(it)) } ?: "Data Final",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                // Establishment Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filtrar por Estabelecimentos",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                filterByEst = false 
                                exportResultUri = null
                            }
                        ) {
                            RadioButton(
                                selected = !filterByEst,
                                onClick = { 
                                    filterByEst = false 
                                    exportResultUri = null
                                }
                            )
                            Text("Todos")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                filterByEst = true 
                                exportResultUri = null
                            }
                        ) {
                            RadioButton(
                                selected = filterByEst,
                                onClick = { 
                                    filterByEst = true 
                                    exportResultUri = null
                                }
                            )
                            Text("Apenas um")
                        }
                    }

                    // Dropdown for establishments (only shown if filterByEst is true)
                    if (filterByEst) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { estDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp, 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedEstName.isNotEmpty()) selectedEstName else "Selecione o Estabelecimento",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            DropdownMenu(
                                expanded = estDropdownExpanded,
                                onDismissRequest = { estDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                if (establishments.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Nenhum estabelecimento cadastrado") },
                                        onClick = { estDropdownExpanded = false }
                                    )
                                } else {
                                    establishments.forEach { est ->
                                        DropdownMenuItem(
                                            text = { Text(est.name) },
                                            onClick = {
                                                selectedEstName = est.name
                                                estDropdownExpanded = false
                                                exportResultUri = null
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

        // Action Buttons: Generate PDF or Excel
        if (!isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        isLoading = true
                        exportResultUri = null
                        coroutineScope.launch {
                            try {
                                val estFilter = if (filterByEst) selectedEstName else "Todos os estabelecimentos"
                                val filtered = ExportHelpers.filterDeliveries(
                                    deliveries,
                                    period,
                                    customStart,
                                    customEnd,
                                    estFilter
                                )
                                val uri = ExportHelpers.exportToPdf(
                                    context = context,
                                    deliveries = filtered,
                                    period = period,
                                    customStart = customStart,
                                    customEnd = customEnd,
                                    selectedEstName = estFilter,
                                    motoboyName = motoboyName,
                                    motoboyPhone = motoboyPhone,
                                    motoboyCity = motoboyCity
                                )
                                if (uri != null) {
                                    exportResultUri = uri
                                    exportResultType = "pdf"
                                    
                                    val sdfDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val sdfMonthStr = SimpleDateFormat("MMMM_yyyy", Locale( "pt", "BR"))
                                    exportResultFileName = when (period) {
                                        "Hoje" -> "Entregas_${sdfDateStr.format(Date())}.pdf"
                                        "Este mês" -> "Entregas_${sdfMonthStr.format(Date()).replaceFirstChar { it.uppercase() }}.pdf"
                                        "Personalizado" -> {
                                            val startStr = customStart?.let { sdfDateStr.format(Date(it)) } ?: "inicio"
                                            val endStr = customEnd?.let { sdfDateStr.format(Date(it)) } ?: "fim"
                                            "Entregas_${startStr}_a_${endStr}.pdf"
                                        }
                                        else -> "Entregas_${period.replace(" ", "_")}_${sdfDateStr.format(Date())}.pdf"
                                    }

                                    Toast.makeText(context, "Relatório salvo com sucesso.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Erro ao salvar relatório em PDF.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("NuCorre", "Erro ao exportar PDF: ${e.message}", e)
                                Toast.makeText(context, "Erro inesperado ao gerar PDF.", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gerar PDF")
                }

                Button(
                    onClick = {
                        isLoading = true
                        exportResultUri = null
                        coroutineScope.launch {
                            try {
                                val estFilter = if (filterByEst) selectedEstName else "Todos os estabelecimentos"
                                val filtered = ExportHelpers.filterDeliveries(
                                    deliveries,
                                    period,
                                    customStart,
                                    customEnd,
                                    estFilter
                                )
                                val uri = ExportHelpers.exportToExcel(
                                    context = context,
                                    deliveries = filtered
                                )
                                if (uri != null) {
                                    exportResultUri = uri
                                    exportResultType = "xlsx"
                                    val sdfMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                                    exportResultFileName = "Entregas_${sdfMonthStr.format(Date())}.xlsx"
                                    Toast.makeText(context, "Relatório salvo com sucesso.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Erro ao salvar planilha do Excel.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("NuCorre", "Erro ao exportar Excel: ${e.message}", e)
                                Toast.makeText(context, "Erro inesperado ao gerar Excel.", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Filled.GridOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gerar Excel")
                }
            }
        }

        // Loading view
        if (isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Gerando relatório, por favor aguarde...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Result Panel: Saved Successfully and Actions
        exportResultUri?.let { uri ->
            val mimeType = if (exportResultType == "pdf") "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Relatório salvo com sucesso.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Text(
                            text = "Arquivo: $exportResultFileName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Local: Documentos/NuCorre/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                    // Buttons of Results Panel
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { ExportHelpers.openFile(context, uri, mimeType) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir " + exportResultType.uppercase())
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { ExportHelpers.shareFile(context, uri, mimeType) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compartilhar", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = { ExportHelpers.shareToWhatsApp(context, uri, mimeType) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Button(
                            onClick = { ExportHelpers.shareToEmail(context, uri) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar por E-mail")
                        }
                    }
                }
            }
        }
    }
}

