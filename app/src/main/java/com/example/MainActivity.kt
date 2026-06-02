package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Child
import com.example.data.DoseLog
import com.example.data.Medication
import com.example.data.SharedProfile
import com.example.ui.FamilyDoseViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

val avatarColors = listOf(
    Color(0xFFFFB7B2), // Pastel Red
    Color(0xFFFFDAC1), // Pastel Orange
    Color(0xFFE2F0CB), // Pastel Green
    Color(0xFFB5EAD7), // Pastel Mint
    Color(0xFFC7CEEA), // Pastel Blue
    Color(0xFFF1CBFF), // Pastel Purple
    Color(0xFFFFD1DC)  // Pastel Pink
)

fun getChildAvatarColor(name: String): Color {
    if (name.isBlank()) return AvatarBgDefault
    val index = name.hashCode().absoluteValue % avatarColors.size
    return avatarColors[index]
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Instantiating ViewModel using our Custom Factory
                val application = LocalContext.current.applicationContext as Application
                val viewModel: FamilyDoseViewModel by viewModels {
                    FamilyDoseViewModel.Factory(application)
                }
                
                FamilyDoseApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FamilyDoseApp(viewModel: FamilyDoseViewModel) {
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val logs by viewModel.doseLogs.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    
    val showAddMed by viewModel.showAddMedDialog.collectAsStateWithLifecycle()
    val showAddProfile by viewModel.showAddProfileDialog.collectAsStateWithLifecycle()
    val showAddChild by viewModel.showAddChildDialog.collectAsStateWithLifecycle()
    
    // Scan Preset Holder State
    var prefilledScanMed by remember { mutableStateOf<Medication?>(null) }
    
    // State indicators for Choose Method and OCR Scanning dialog overlays
    var showAddMedChooser by remember { mutableStateOf(false) }
    var selectedRecipientForAdd by remember { mutableStateOf<String?>(null) }
    var showOCRScanDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FamilyDoseBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) {
                            prefilledScanMed = null
                            selectedRecipientForAdd = null
                            showAddMedChooser = true
                        } else {
                            viewModel.setShowAddProfileDialog(true)
                        }
                    },
                    containerColor = PolishPrimaryContainer,
                    contentColor = PolishOnPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp, end = 8.dp)
                        .testTag(if (selectedTab == 0) "fab_add_medication" else "fab_add_profile")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (selectedTab == 0) "Add Medication" else "Invite Caretaker",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PolishBackground)
                .padding(innerPadding)
        ) {
            // Screen Dispatcher based on active tab state
            Crossfade(targetState = selectedTab, label = "tab_fade") { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        medications = medications,
                        children = children,
                        onLogMed = { 
                            viewModel.logMedication(it)
                            scope.launch {
                                snackbarHostState.showSnackbar("Logged ${it.name} for ${it.recipient}")
                            }
                        },
                        onDeleteMed = { viewModel.deleteMedication(it) },
                        onShareCardClick = { viewModel.selectTab(1) }, // Navigate to Family Tab
                        onAddMedicationForChild = { recipient ->
                            prefilledScanMed = null
                            selectedRecipientForAdd = recipient
                            showAddMedChooser = true
                        }
                    )
                    1 -> FamilyScreen(
                        children = children,
                        onDeleteChild = { viewModel.deleteChild(it) },
                        onEditChild = { id, name, age, dob, height, weight ->
                            viewModel.updateChildInfo(id, name, age, dob, height, weight)
                        },
                        onAddChildClick = { viewModel.setShowAddChildDialog(true) },
                        onAddMedicationForChild = { recipient ->
                            prefilledScanMed = null
                            selectedRecipientForAdd = recipient
                            showAddMedChooser = true
                        }
                    )
                    2 -> AccountScreen(
                        logs = logs,
                        profiles = profiles,
                        medications = medications,
                        onDeleteLog = { viewModel.deleteLog(it) },
                        onDeleteProfile = { viewModel.deleteProfile(it) },
                        onShareAccessClick = { viewModel.setShowAddProfileDialog(true) }
                    )
                }
            }
        }
    }

    // Method Chooser Dialog Overlay
    if (showAddMedChooser) {
        AddMedicationMethodChooserDialog(
            recipientName = selectedRecipientForAdd,
            onDismiss = { showAddMedChooser = false },
            onChooseManual = {
                showAddMedChooser = false
                viewModel.setShowAddMedDialog(true)
            },
            onChooseScan = {
                showAddMedChooser = false
                showOCRScanDialog = true
            }
        )
    }

    // OCR simulated Scan dialog overlay
    if (showOCRScanDialog) {
        ScanOCRDialog(
            recipientName = selectedRecipientForAdd,
            onDismiss = { showOCRScanDialog = false },
            onScanComplete = { scannedMed ->
                showOCRScanDialog = false
                prefilledScanMed = scannedMed
                viewModel.setShowAddMedDialog(true)
            }
        )
    }

    // Add Medication Dialog Overlay
    if (showAddMed) {
        AddMedicationDialog(
            prefilled = prefilledScanMed,
            childrenList = children,
            defaultRecipient = selectedRecipientForAdd,
            onDismiss = {
                prefilledScanMed = null
                viewModel.setShowAddMedDialog(false)
            },
            onConfirm = { name, dosage, recipient, time, isCrit, cat ->
                viewModel.addNewMedication(name, dosage, recipient, time, isCrit, cat)
            }
        )
    }

    // Add Family Profile Dialog Overlay
    if (showAddProfile) {
        AddProfileDialog(
            onDismiss = { viewModel.setShowAddProfileDialog(false) },
            onConfirm = { name, role, sharingType, duration, start, end ->
                viewModel.addNewProfile(name, role, sharingType, duration, start, end)
            }
        )
    }

    // Add Child Profile Dialog Overlay
    if (showAddChild) {
        AddChildDialog(
            onDismiss = { viewModel.setShowAddChildDialog(false) },
            onConfirm = { name, age, dob, height, weight ->
                viewModel.addNewChild(name, age, dob, height, weight)
            }
        )
    }
}

// ==========================================
// HOME TAB (DASHBOARD)
// ==========================================
@Composable
fun HomeScreen(
    medications: List<Medication>,
    children: List<Child>,
    onLogMed: (Medication) -> Unit,
    onDeleteMed: (Medication) -> Unit,
    onShareCardClick: () -> Unit,
    onAddMedicationForChild: (String?) -> Unit
) {
    var medicationToDelete by remember { mutableStateOf<Medication?>(null) }
    var selectedChildFilter by remember { mutableStateOf("All") }

    // Separate critical "Due Now" medications from upcoming medications, filtered by recipient child
    val criticalMeds = medications.filter { med ->
        med.isCritical && (selectedChildFilter == "All" || med.recipient.equals(selectedChildFilter, ignoreCase = true))
    }
    val upcomingMeds = medications.filter { med ->
        !med.isCritical && (selectedChildFilter == "All" || med.recipient.equals(selectedChildFilter, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header profile context
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FAMILY DOSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishOnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Good morning, Sarah",
                    style = MaterialTheme.typography.titleLarge,
                    color = PolishOnSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                color = AvatarBgDefault,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "S",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AvatarTextDefault
                        )
                    )
                }
            }
        }

        // Horizontal filter list for children
        Text(
            text = "RECIPIENT DOSES FILTER",
            style = MaterialTheme.typography.labelSmall,
            color = PolishOnSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            letterSpacing = 0.5.sp
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            // "All" chip
            item {
                val isSelected = selectedChildFilter == "All"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedChildFilter = "All" }
                        .padding(4.dp)
                        .testTag("filter_all_children")
                ) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) PolishPrimary else PolishOutlineVariant,
                                shape = CircleShape
                            ),
                        color = if (isSelected) PolishPrimaryContainer else Color(0xFFF3EDF7)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "All",
                                tint = if (isSelected) PolishPrimary else PolishOnSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kids",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) PolishPrimary else PolishOnSurfaceVariant
                    )
                }
            }

            // Database children list
            items(children) { child ->
                val isSelected = selectedChildFilter.equals(child.name, ignoreCase = true)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedChildFilter = child.name }
                        .padding(4.dp)
                        .testTag("filter_child_${child.name.lowercase()}")
                ) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) PolishPrimary else PolishOutlineVariant,
                                shape = CircleShape
                            ),
                        color = if (isSelected) PolishPrimaryContainer else getChildAvatarColor(child.name)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = child.avatarLetter,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PolishPrimary else AvatarTextDefault
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) PolishPrimary else PolishOnSurfaceVariant
                    )
                }
            }

            // Floating helper click to quickly schedule a dose specifically for the selected kid!
            if (selectedChildFilter != "All") {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onAddMedicationForChild(selectedChildFilter) }
                            .padding(4.dp)
                            .testTag("filter_quick_add")
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = PolishPrimary,
                                    shape = CircleShape
                                ),
                            color = PolishSecondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Quick Add Dose",
                                    tint = PolishPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add Dose",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = PolishPrimary
                        )
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Critical "Due Now" Alert Section
            if (criticalMeds.isNotEmpty()) {
                item {
                    Text(
                        text = "CRITICAL DUE NOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishAlertAction,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
                items(criticalMeds, key = { "crit_${it.id}" }) { medication ->
                    CriticalDueCard(
                        medication = medication,
                        onLoggedClick = { onLogMed(medication) },
                        onDeleteClick = { medicationToDelete = medication }
                    )
                }
            }

            // 2. Quick Insights: Active Sharing Banner
            item {
                ActiveSharingBanner(onClick = onShareCardClick)
            }

            // 3. Upcoming Schedule Section
            item {
                Text(
                    text = "UPCOMING DOSES SCHEDULE",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishOnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                    letterSpacing = 1.sp
                )
            }

            if (upcomingMeds.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "No upcoming doses",
                                tint = PolishOnSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All set! No upcoming medications for this recipient.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PolishOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(upcomingMeds, key = { "up_${it.id}" }) { medication ->
                    UpcomingDoseCard(
                        medication = medication,
                        onQuickLog = { onLogMed(medication) },
                        onDeleteClick = { medicationToDelete = medication }
                    )
                }
            }
            
            // Padding at bottom of lazy column so items don't overlap with FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (medicationToDelete != null) {
        AlertDialog(
            onDismissRequest = { medicationToDelete = null },
            title = { Text("Delete Medication?") },
            text = { Text("Are you sure you want to delete ${medicationToDelete?.name}? This will remove it from the schedule.") },
            confirmButton = {
                TextButton(onClick = {
                    medicationToDelete?.let { onDeleteMed(it) }
                    medicationToDelete = null
                }) { Text("Delete", color = PolishAlertAction) }
            },
            dismissButton = {
                TextButton(onClick = { medicationToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CriticalDueCard(medication: Medication, onLoggedClick: () -> Unit, onDeleteClick: () -> Unit) {
    // Pulse animation configuration for warning dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PolishAlertBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PolishAlertBorder, RoundedCornerShape(28.dp))
            .testTag("critical_card_${medication.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Alert Header Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(alpha)
                            .clip(CircleShape)
                            .background(PolishAlertAction)
                    )
                    Text(
                        text = "DUE NOW",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PolishAlertTextBrand,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.25.sp
                        )
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp).testTag("btn_delete_critical_${medication.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete medication",
                        tint = PolishAlertTextBrand.copy(alpha = 0.6f)
                    )
                }
            }

            // Dose detail and complete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${medication.name} (${medication.dosage})",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PolishAlertTextDark
                        )
                    )
                    Text(
                        text = "${medication.recipient} • ${medication.scheduledTime} Dose",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PolishAlertTextBrand,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Button(
                    onClick = onLoggedClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PolishAlertAction),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    modifier = Modifier
                        .testTag("log_btn_${medication.id}")
                ) {
                    Text(
                        text = "Logged",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveSharingBanner(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSecondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("sharing_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Shared vector logo icon card
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Active Sharing Group",
                        tint = PolishOnPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "SHARED ACCESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PolishOnPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Babysitter Chloe • 3h left",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = TextDecoration.Underline,
                            color = PolishOnPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = PolishOnPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun UpcomingDoseCard(medication: Medication, onQuickLog: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PolishOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .testTag("upcoming_card_${medication.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Left graphical display icon matching specific categories
                val isVitamin = medication.category.contains("vitamin", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isVitamin) Color(0xFFEADDFF) else AvatarGreyBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVitamin) Icons.Outlined.WaterDrop else Icons.Outlined.Healing,
                        contentDescription = "Medication Icon",
                        tint = if (isVitamin) Color(0xFF21005D) else PolishOnSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "${medication.name} (${medication.dosage})",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PolishOnSurface
                        )
                    )
                    Text(
                        text = "${medication.recipient} • ${medication.scheduledTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishOnSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp).testTag("btn_delete_upcoming_${medication.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete layout",
                        tint = PolishOnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onQuickLog,
                    modifier = Modifier.size(36.dp).testTag("quick_log_${medication.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Quick Check Log",
                        tint = PolishPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// FAMILY TAB
// ==========================================
@Composable
fun FamilyScreen(
    children: List<Child>,
    onDeleteChild: (Child) -> Unit,
    onEditChild: (id: Int, name: String, age: String, dob: String, height: String, weight: String) -> Unit,
    onAddChildClick: () -> Unit,
    onAddMedicationForChild: (String) -> Unit
) {
    var childToDelete by remember { mutableStateOf<Child?>(null) }
    var childToEdit by remember { mutableStateOf<Child?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)) {
            Text(
                text = "KIDS PROFILES",
                style = MaterialTheme.typography.labelSmall,
                color = PolishOnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Children & Shared Access",
                style = MaterialTheme.typography.titleLarge,
                color = PolishOnSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ==========================================
            // SECTION 1: CHILDREN (NO APP ACCESS)
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ChildCare, contentDescription = null, tint = PolishPrimary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Children Profiles",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishOnSurface
                        )
                    }
                    Button(
                        onClick = onAddChildClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryContainer, contentColor = PolishOnPrimaryContainer),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_add_child_inline")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Child", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Children do not have direct app access. Add them as medication recipients managed entirely by your account owner.",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishOnSurfaceVariant
                )
            }

            if (children.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = PolishSurface.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No children added to the account yet.", style = MaterialTheme.typography.bodyMedium, color = PolishOnSurfaceVariant)
                        }
                    }
                }
            } else {
                items(children, key = { "child_${it.id}" }) { child ->
                    ChildMemberRow(
                        child = child,
                        onAddMedication = { onAddMedicationForChild(child.name) },
                        onEdit = { childToEdit = child },
                        onDelete = { childToDelete = child }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (childToDelete != null) {
        AlertDialog(
            onDismissRequest = { childToDelete = null },
            title = { Text("Remove Child?") },
            text = { Text("Are you sure you want to remove ${childToDelete?.name} from your care circle? Their medication records will remain in the logs.") },
            confirmButton = {
                TextButton(onClick = {
                    childToDelete?.let { onDeleteChild(it) }
                    childToDelete = null
                }) { Text("Remove", color = PolishAlertAction) }
            },
            dismissButton = {
                TextButton(onClick = { childToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (childToEdit != null) {
        EditChildDialog(
            child = childToEdit!!,
            onDismiss = { childToEdit = null },
            onConfirm = { id, name, age, dob, height, weight ->
                onEditChild(id, name, age, dob, height, weight)
                childToEdit = null
            }
        )
    }
}

@Composable
fun ChildMemberRow(child: Child, onAddMedication: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PolishOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .testTag("child_member_${child.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    color = getChildAvatarColor(child.name)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = child.avatarLetter,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = AvatarTextDefault
                            )
                        )
                    }
                }

                Column {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = PolishOnSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = buildString {
                                append(child.age)
                                if (child.dob.isNotBlank()) append(" • DOB: ${child.dob}")
                                if (child.height.isNotBlank()) append(" • ${child.height}")
                                if (child.weight.isNotBlank()) append(" • ${child.weight}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishOnSurfaceVariant
                        )
                        Badge(
                            containerColor = Color(0xFFF3EDF7),
                            contentColor = Color(0xFF1D192B)
                        ) {
                            Text("No App Access", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(2.dp))
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onAddMedication,
                    modifier = Modifier.testTag("add_med_child_${child.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddReaction,
                        contentDescription = "Quick Add Medication",
                        tint = PolishPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit child profile",
                        tint = PolishOnSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove child profile",
                        tint = PolishAlertAction.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileMemberRow(profile: SharedProfile, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PolishOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .testTag("profile_member_${profile.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    color = if (profile.sharingType != "FULL") AvatarGreyBg else AvatarBgDefault
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = profile.avatarLetter,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (profile.sharingType != "FULL") PolishOnSurfaceVariant else AvatarTextDefault
                            )
                        )
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = PolishOnSurface
                        )
                        when (profile.sharingType) {
                            "DURATION" -> {
                                Badge(
                                    containerColor = PolishAlertBackground,
                                    contentColor = PolishAlertTextBrand
                                ) {
                                    Text("Temporary", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                            "SCHEDULED" -> {
                                Badge(
                                    containerColor = PolishSecondaryContainer,
                                    contentColor = PolishOnPrimaryContainer
                                ) {
                                    Text("Scheduled Window", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                        }
                    }
                    Text(
                        text = profile.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishOnSurfaceVariant
                    )
                    
                    when (profile.sharingType) {
                        "FULL" -> {
                            Text(
                                text = "Full Account Access • Constant/Lifetime",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "DURATION" -> {
                            val duration = profile.durationHours ?: 4
                            Text(
                                text = "Access Window: Active for next $duration hours",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishAlertTextBrand,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "SCHEDULED" -> {
                            val start = profile.scheduleTimeStart ?: "08:00 AM"
                            val end = profile.scheduleTimeEnd ?: "05:00 PM"
                            Text(
                                text = "Daily access enabled: $start - $end",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Expose removal configuration (disable deletion of Admin/Mother Sarah)
            val isSarah = profile.name.equals("Sarah", ignoreCase = true)
            if (!isSarah) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove family access",
                        tint = PolishAlertAction.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    text = "Admin",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PolishPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }
}

// ==========================================
// SELECTION & SIMULATION OCR ENGINES OVERLAYS
// ==========================================
@Composable
fun AddMedicationMethodChooserDialog(
    recipientName: String?,
    onDismiss: () -> Unit,
    onChooseManual: () -> Unit,
    onChooseScan: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("app_method_chooser_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = PolishPrimary,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = if (recipientName != null) "Schedule for $recipientName" else "Add New Medication",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishOnSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Would you like to manually schedule the medication doses or scan a prescription label to autofill?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PolishOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Manual Button
                Button(
                    onClick = onChooseManual,
                    modifier = Modifier.fillMaxWidth().testTag("chooser_btn_manual"),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Manually")
                }
                
                // Scan Button
                Button(
                    onClick = onChooseScan,
                    modifier = Modifier.fillMaxWidth().testTag("chooser_btn_scan"),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishSecondaryContainer, contentColor = PolishOnPrimaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Bottle Label (OCR)")
                }
                
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = PolishOnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ScanOCRDialog(
    recipientName: String?,
    onDismiss: () -> Unit,
    onScanComplete: (Medication) -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Mock presets
    val presets = listOf(
        Medication(name = "Amoxicillin", dosage = "3.5ml", recipient = recipientName ?: "Leo", scheduledTime = "8:00 AM", isCritical = true, category = "Antibiotic"),
        Medication(name = "Zyrtec Allergy", dosage = "5ml", recipient = recipientName ?: "Maya", scheduledTime = "6:30 PM", isCritical = false, category = "Allergy"),
        Medication(name = "Infant Ibuprofen", dosage = "2.5ml", recipient = recipientName ?: "Sarah", scheduledTime = "12:00 PM", isCritical = true, category = "General"),
        Medication(name = "Pedialyte Hydrate", dosage = "100ml", recipient = recipientName ?: "Leo", scheduledTime = "1:00 PM", isCritical = false, category = "General")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("scan_ocr_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Camera, contentDescription = null, tint = PolishPrimary)
                        Text(
                            text = "Prescription OCR Scan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PolishOnSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close scanner")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (isScanning) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxHeight = maxHeight
                        val infiniteTransition = rememberInfiniteTransition(label = "laser_trans")
                        val laserVal by infiniteTransition.animateFloat(
                            initialValue = 0.1f,
                            targetValue = 0.9f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "laser_offset"
                        )

                        // Camera Reticle Border
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .border(2.dp, PolishPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        ) {
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(12.dp).border(3.dp, PolishPrimary, RoundedCornerShape(topStart = 4.dp)))
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(12.dp).border(3.dp, PolishPrimary, RoundedCornerShape(topEnd = 4.dp)))
                            Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(12.dp).border(3.dp, PolishPrimary, RoundedCornerShape(bottomStart = 4.dp)))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(12.dp).border(3.dp, PolishPrimary, RoundedCornerShape(bottomEnd = 4.dp)))
                        }

                        // Laser line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = maxHeight * (laserVal - 0.5f))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFF4CAF50), Color.Transparent)
                                    )
                                )
                        )

                        // Loading text
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = PolishPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = progressMessage,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Align the medicine bottle's official instruction label within the camera reticle, or choose a test bottle below to mock the instant OCR validation stream:",
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.forEach { preset ->
                            Button(
                                onClick = {
                                    isScanning = true
                                    scope.launch {
                                        progressMessage = "Locating label container..."
                                        delay(700)
                                        progressMessage = "Extracting OCR instruction strings..."
                                        delay(800)
                                        progressMessage = "Validating scheduled prescription..."
                                        delay(600)
                                        isScanning = false
                                        onScanComplete(preset)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PolishSecondaryContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scan_preset_dialog_${preset.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = preset.name,
                                            color = PolishOnPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Recipient target: ${preset.recipient} (${preset.dosage})",
                                            color = PolishOnPrimaryContainer.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Camera,
                                        contentDescription = "Simulate scan bottle",
                                        tint = PolishPrimary,
                                        modifier = Modifier.size(20.dp)
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

// ==========================================
// ACCOUNT TAB (POLISHED SYSTEM DASHBOARD & SETTINGS)
// ==========================================
@Composable
fun AccountScreen(
    logs: List<DoseLog>,
    profiles: List<SharedProfile>,
    medications: List<Medication>,
    onDeleteLog: (Int) -> Unit,
    onDeleteProfile: (SharedProfile) -> Unit,
    onShareAccessClick: () -> Unit
) {
    var doseRemindersEnabled by remember { mutableStateOf(true) }
    var coParentAlertsEnabled by remember { mutableStateOf(true) }
    var criticalEmailReports by remember { mutableStateOf(false) }
    
    var selectedTimezone by remember { mutableStateOf("US/Eastern (EST)") }
    var automaticGpsSyncTimeZone by remember { mutableStateOf(true) }
    
    var showInviteDialogCode by remember { mutableStateOf(false) }
    var generatedJoinCode by remember { mutableStateOf("") }
    
    var logToDelete by remember { mutableStateOf<Int?>(null) }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)) {
                Text(
                    text = "ADMINISTRATION PANEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishOnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Account & System Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = PolishOnSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Action stats cards row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCompactCard("Shared Circles", "${profiles.size}", Modifier.weight(1f))
                StatsCompactCard("Active Meds", "${medications.size}", Modifier.weight(1f))
                StatsCompactCard("Doses Logged", "${logs.size}", Modifier.weight(1f))
            }
        }

        // SECTION 1: NOTIFICATION SETTINGS GROUP
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PolishOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PolishPrimary)
                        Text("Notification preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PolishOnSurface)
                    }
                    HorizontalDivider(color = PolishOutlineVariant.copy(alpha = 0.2f))
                    
                    // Toggle 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("Dose Overdue Reminders", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PolishOnSurface)
                            Text("Receive persistent push alarms when a child's critical dose is past scheduled window", style = MaterialTheme.typography.labelSmall, color = PolishOnSurfaceVariant)
                        }
                        Switch(
                            checked = doseRemindersEnabled,
                            onCheckedChange = { doseRemindersEnabled = it },
                            modifier = Modifier.testTag("switch_dose_reminders")
                        )
                    }

                    // Toggle 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("Co-Parent Activity Feeds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PolishOnSurface)
                            Text("Notify immediately on device when caretaker or babysitter completes a safety log", style = MaterialTheme.typography.labelSmall, color = PolishOnSurfaceVariant)
                        }
                        Switch(
                            checked = coParentAlertsEnabled,
                            onCheckedChange = { coParentAlertsEnabled = it },
                            modifier = Modifier.testTag("switch_co_parent_logs")
                        )
                    }

                    // Toggle 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("Weekly Critical PDF Audits", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PolishOnSurface)
                            Text("Automatically compile and export weekly dose compliance spreadsheets directly to family admins", style = MaterialTheme.typography.labelSmall, color = PolishOnSurfaceVariant)
                        }
                        Switch(
                            checked = criticalEmailReports,
                            onCheckedChange = { criticalEmailReports = it },
                            modifier = Modifier.testTag("switch_safety_reports")
                        )
                    }
                }
            }
        }

        // SECTION 2: TIMEZONE & REGIONAL MATRIX
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PolishOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = PolishPrimary)
                        Text("Timezone & Regional Context", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PolishOnSurface)
                    }
                    HorizontalDivider(color = PolishOutlineVariant.copy(alpha = 0.2f))
                    
                    Text(
                        text = "Medication reminders automatically convert timelines during interstate travels. Current locked timezone reference: $selectedTimezone",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishOnSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("EST", "PST", "GMT", "UTC").forEach { tz ->
                            val isSelected = selectedTimezone.contains(tz)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTimezone = when (tz) {
                                        "EST" -> "US/Eastern (EST)"
                                        "PST" -> "US/Pacific (PST)"
                                        "GMT" -> "Europe/London (GMT)"
                                        else -> "Universal Coordinated (UTC)"
                                    }
                                },
                                label = { Text(tz, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f).testTag("timezone_chip_$tz")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("Automatic Network Override", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PolishOnSurface)
                            Text("Automatically synch device medication records using network cellular clocks", style = MaterialTheme.typography.labelSmall, color = PolishOnSurfaceVariant)
                        }
                        Switch(
                            checked = automaticGpsSyncTimeZone,
                            onCheckedChange = { automaticGpsSyncTimeZone = it },
                            modifier = Modifier.testTag("switch_auto_timezone")
                        )
                    }
                }
            }
        }

        // SECTION 3: SHARED ACCOUNT ACCESS (CARETAKERS & GUESTS)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PolishOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = PolishPrimary, modifier = Modifier.size(20.dp))
                            Text("Shared Account Access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PolishOnSurface)
                        }
                        IconButton(
                            onClick = onShareAccessClick,
                            modifier = Modifier.testTag("btn_share_access_inline")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Shared Profile", tint = PolishPrimary)
                        }
                    }
                    HorizontalDivider(color = PolishOutlineVariant.copy(alpha = 0.2f))
                    
                    Text(
                        text = "Grant customized app access and set safety permissions for co-parents, caretakers, or babysitters.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishOnSurfaceVariant
                    )
                    
                    if (profiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No caretakers mapped to this account.", style = MaterialTheme.typography.bodyMedium, color = PolishOnSurfaceVariant)
                        }
                    } else {
                        profiles.forEach { profile ->
                            ProfileMemberRow(
                                profile = profile,
                                onDelete = { onDeleteProfile(profile) }
                            )
                        }
                    }
                }
            }
        }

        // SECTION 4: CIRCLE MEMBERS & DEVICES TRACKER
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PolishOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = PolishPrimary)
                        Text("Active Care Circle Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PolishOnSurface)
                    }
                    HorizontalDivider(color = PolishOutlineVariant.copy(alpha = 0.2f))

                    Text(
                        text = "The following authenticated smart devices can securely view medication histories mapped to your Care Circle family account:",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishOnSurfaceVariant
                    )

                    // Connected devices list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeviceActiveSessionRow("Sarah's Pixel 9 (Admin Owner)", "Active session now • Android 15 Edge", Icons.Default.PhoneAndroid)
                        DeviceActiveSessionRow("David's iPhone 15 Pro (Co-Parent)", "Last synchronized: 5 minutes ago", Icons.Default.PhoneIphone)
                        DeviceActiveSessionRow("Chloe's Babysitter Tablet Link", "Session duration limit: Expires in 4 hours", Icons.Default.Laptop)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val secureRandom = java.security.SecureRandom()
                            generatedJoinCode = "DOSE-${secureRandom.nextInt(9000) + 1000}-LINK"
                            showInviteDialogCode = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("btn_generate_invite_code")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Secure Caretaker Code")
                    }
                }
            }
        }

        // SECTION 5: MEDICATION HISTORIC SAFETY LOGS (AUDIT)
        item {
            Text(
                text = "MEDICATION HISTORIC AUDIT LOG",
                style = MaterialTheme.typography.labelSmall,
                color = PolishOnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PolishOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No doses have been logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PolishOnSurfaceVariant
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        logs.forEachIndexed { index, log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .testTag("log_row_${log.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${log.medicationName} logged safely",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PolishOnSurface
                                    )
                                    val formattedTime = remember(log.timestamp) {
                                        java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
                                    }
                                    Text(
                                        text = "For ${log.recipient} (${log.dosage}) • By ${log.loggedBy} • $formattedTime",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PolishOnSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { logToDelete = log.id }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete log entry",
                                        tint = PolishOnSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (index < logs.size - 1) {
                                HorizontalDivider(color = PolishOutlineVariant.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Log Entry?") },
            text = { Text("Are you sure you want to delete this dosage log? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    logToDelete?.let { onDeleteLog(it) }
                    logToDelete = null
                }) { Text("Delete", color = PolishAlertAction) }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showInviteDialogCode) {
        Dialog(onDismissRequest = { showInviteDialogCode = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurface),
                modifier = Modifier.padding(16.dp).fillMaxWidth().testTag("dialog_invite_code")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = PolishPrimary, modifier = Modifier.size(48.dp))
                    Text("Caretaker Access Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PolishOnSurface)
                    Text("Provide this temporary join token code to a babysitter or guest caregiver to let them easily log medication doses on their own device.", style = MaterialTheme.typography.bodyMedium, color = PolishOnSurfaceVariant, textAlign = TextAlign.Center)
                    
                    Surface(
                        color = PolishSecondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = generatedJoinCode,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
                            color = PolishOnPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }

                    Text("Token expires in 24 hours. Grants standard restricted logging permissions under care monitoring guidelines.", style = MaterialTheme.typography.labelSmall, color = PolishOnSurfaceVariant, textAlign = TextAlign.Center)
                    
                    Button(
                        onClick = { showInviteDialogCode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceActiveSessionRow(deviceName: String, sessionStatus: String, iconType: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = PolishPrimaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(iconType, contentDescription = null, tint = PolishPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Column {
            Text(deviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PolishOnSurface)
            Text(sessionStatus, style = MaterialTheme.typography.labelSmall, color = PolishOnSurfaceVariant)
        }
    }
}

@Composable
fun StatsCompactCard(title: String, score: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PolishSurface),
        modifier = modifier.border(1.dp, PolishOutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = PolishPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = PolishOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// CUSTOM BOTTOM NAVIGATION
// ==========================================
@Composable
fun FamilyDoseBottomNavigation(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        color = Color(0xFFF3EDF7),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0x80CAC4D0), shape = RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(80.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                AppNavigationTabItem("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
                AppNavigationTabItem("Kids", Icons.Filled.ChildCare, Icons.Outlined.ChildCare, "tab_family"),
                AppNavigationTabItem("Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "tab_account")
            )

            tabs.forEachIndexed { index, tab ->
                val isActive = index == selectedTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp)
                        .alpha(if (isActive) 1.0f else 0.6f)
                        .testTag(tab.testTag)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isActive) Color(0xFFE8DEF8) else Color.Transparent)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) tab.activeIcon else tab.inactiveIcon,
                            contentDescription = tab.label,
                            tint = if (isActive) Color(0xFF1D192B) else PolishOnSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        style = if (isActive) {
                            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1D192B))
                        } else {
                            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, color = PolishOnSurfaceVariant)
                        },
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

data class AppNavigationTabItem(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String
)

// ==========================================
// ADD DIALOGS COMPOSABLES
// ==========================================
@Composable
fun AddMedicationDialog(
    prefilled: Medication?,
    childrenList: List<Child>,
    defaultRecipient: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, dosage: String, recipient: String, time: String, isCritical: Boolean, category: String) -> Unit
) {
    var name by remember { mutableStateOf(prefilled?.name ?: "") }
    var dosage by remember { mutableStateOf(prefilled?.dosage ?: "") }
    var recipient by remember { mutableStateOf(prefilled?.recipient ?: defaultRecipient ?: "") }
    var time by remember { mutableStateOf(prefilled?.scheduledTime ?: "8:00 AM") }
    var isCtirical by remember { mutableStateOf(prefilled?.isCritical ?: false) }
    var category by remember { mutableStateOf(prefilled?.category ?: "General") }

    val recipients = if (childrenList.isNotEmpty()) {
        childrenList.map { it.name }
    } else {
        listOf("Leo", "Maya", "Sarah")
    }

    // Prefill default recipient if empty
    LaunchedEffect(recipients, recipient) {
        if (recipient.isBlank() && recipients.isNotEmpty()) {
            recipient = defaultRecipient ?: recipients.first()
        }
    }

    val categories = listOf("Antibiotic", "Vitamin", "Allergy", "General")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("app_add_medication_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (prefilled != null) "Review Scanned Label" else "New Scheduled Dose",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishOnSurface
                )

                // Input Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_input_name")
                )

                // Input Dosage Field
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 3.5ml, 1 Tab)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_input_dosage")
                )

                // Input Scheduled Time Field
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Schedule Time (e.g. 8:00 AM)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_input_time")
                )

                // Recipient Dropdown/selector row
                Column {
                    Text("Recipient Selection", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recipients.forEach { rec ->
                            val isSelected = rec == recipient
                            SuggestionChip(
                                onClick = { recipient = rec },
                                label = { Text(rec) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) PolishSecondaryContainer else Color.Transparent,
                                    labelColor = if (isSelected) PolishOnPrimaryContainer else PolishOnSurfaceVariant
                                ),
                                modifier = Modifier.testTag("med_recipient_$rec")
                            )
                        }
                    }
                }

                // Category selector row
                Column {
                    Text("Category Tag", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = cat == category
                            SuggestionChip(
                                onClick = { category = cat },
                                label = { Text(cat) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) PolishPrimaryContainer else Color.Transparent,
                                    labelColor = if (isSelected) PolishOnPrimaryContainer else PolishOnSurfaceVariant
                                ),
                                modifier = Modifier.testTag("med_category_$cat")
                            )
                        }
                    }
                }

                // Critical Alert Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Due Now / High Priority",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Prompts a critical due warning card immediately",
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishOnSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCtirical,
                        onCheckedChange = { isCtirical = it },
                        modifier = Modifier.testTag("med_input_critical")
                    )
                }

                // Action Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PolishOnSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && dosage.isNotBlank()) {
                                onConfirm(name, dosage, recipient, time, isCtirical, category)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                        modifier = Modifier.testTag("med_confirm_button")
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun AddChildDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, age: String, dob: String, height: String, weight: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("app_add_child_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Child Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishOnSurface
                )
                Text(
                    text = "Children profiles do not have direct app access. They are added as medication recipients managed entirely by you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishOnSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Child Name (e.g. Leo)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("child_input_name")
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age / Description (e.g. 4 years old)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("child_input_age")
                )

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (e.g. MM/DD/YYYY)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("child_input_dob")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("child_input_height")
                    )

                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("child_input_weight")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PolishOnSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && age.isNotBlank()) {
                                onConfirm(name, age, dob, height, weight)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                        modifier = Modifier.testTag("child_confirm_button")
                    ) {
                        Text("Save Child")
                    }
                }
            }
        }
    }
}

@Composable
fun EditChildDialog(
    child: Child,
    onDismiss: () -> Unit,
    onConfirm: (id: Int, name: String, age: String, dob: String, height: String, weight: String) -> Unit
) {
    var name by remember { mutableStateOf(child.name) }
    var age by remember { mutableStateOf(child.age) }
    var dob by remember { mutableStateOf(child.dob) }
    var height by remember { mutableStateOf(child.height) }
    var weight by remember { mutableStateOf(child.weight) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Child Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PolishOnSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Child Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age / Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PolishOnSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && age.isNotBlank()) {
                                onConfirm(child.id, name, age, dob, height, weight)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String, sharingType: String, durationHours: Int?, scheduleTimeStart: String?, scheduleTimeEnd: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var sharingType by remember { mutableStateOf("FULL") } // "FULL", "DURATION", "SCHEDULED"
    var durationHours by remember { mutableStateOf("4") }
    
    var startHour by remember { mutableStateOf("09:00 AM") }
    var endHour by remember { mutableStateOf("05:00 PM") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("app_add_profile_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Share Account Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PolishOnSurface
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Caretaker Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_input_name")
                    )
                }

                item {
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Role / Relationship (e.g. Babysitter)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_input_role")
                    )
                }

                // Sharing Access Scope Selection (FULL, DURATION, SCHEDULED)
                item {
                    Column {
                        Text(
                            text = "Access Restrictions & Sharing Options",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishOnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("FULL", "Full Access", "profile_type_full"),
                                Triple("DURATION", "Set Duration", "profile_type_duration"),
                                Triple("SCHEDULED", "Daily Schedule", "profile_type_scheduled")
                            ).forEach { (type, label, testTagValue) ->
                                val isSelected = sharingType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { sharingType = type },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag(testTagValue)
                                )
                            }
                        }
                    }
                }

                // Optional Duration panel
                if (sharingType == "DURATION") {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PolishSecondaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("duration_field_container")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Temporary Session (Expires)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = PolishOnPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(2, 4, 8, 12, 24).forEach { hr ->
                                        val isCurrent = durationHours == hr.toString()
                                        SuggestionChip(
                                            onClick = { durationHours = hr.toString() },
                                            label = { Text("${hr}h") },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (isCurrent) PolishPrimaryContainer else Color.Transparent
                                            ),
                                            modifier = Modifier.testTag("chip_${hr}h")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = durationHours,
                                    onValueChange = { durationHours = it },
                                    label = { Text("Custom Hours Duration") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("profile_input_expiry")
                                )
                            }
                        }
                    }
                }

                // Optional Daily Schedule panel
                if (sharingType == "SCHEDULED") {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PolishPrimaryContainer.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("scheduled_field_container")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Daily Set Window Restrictions",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = PolishPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = startHour,
                                    onValueChange = { startHour = it },
                                    label = { Text("Active From (e.g. 09:00 AM)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("schedule_start_input")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = endHour,
                                    onValueChange = { endHour = it },
                                    label = { Text("Active Until (e.g. 05:00 PM)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("schedule_end_input")
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = PolishOnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank() && role.isNotBlank()) {
                                    onConfirm(
                                        name,
                                        role,
                                        sharingType,
                                        if (sharingType == "DURATION") durationHours.toIntOrNull() ?: 4 else null,
                                        if (sharingType == "SCHEDULED") startHour else null,
                                        if (sharingType == "SCHEDULED") endHour else null
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimary),
                            modifier = Modifier.testTag("profile_confirm_button")
                        ) {
                            Text("Invite Caretaker")
                        }
                    }
                }
            }
        }
    }
}
