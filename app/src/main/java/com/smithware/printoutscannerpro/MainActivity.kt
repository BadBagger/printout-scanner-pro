package com.smithware.printoutscannerpro

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val Charcoal = Color(0xFF171916)
private val Lime = Color(0xFF98DE10)
private val OffWhite = Color(0xFFF8F7F1)
private val SoftYellow = Color(0xFFFFF3CA)
private val SoftRed = Color(0xFFFFE2E2)
private val SoftGreen = Color(0xFFEAF7D8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PrintoutScannerApp() }
    }
}

@Composable
fun PrintoutScannerApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Lime,
            onPrimary = Charcoal,
            secondary = Color(0xFFF3B33D),
            surface = OffWhite,
            background = Color(0xFFEFEDE5)
        )
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { BottomNav(nav) }
        ) { padding ->
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") { HomeScreen(vm, nav) }
                composable("picker") { DocumentTypePickerScreen(nav) }
                composable("scan") { ScanScreen(vm, nav, snackbar) }
                composable("review") { ScanReviewScreen(vm, nav) }
                composable("trackers") { TrainingRadarScreen(vm, nav) }
                composable("associates") { AssociateDatabaseScreen(vm) }
                composable("working") { WorkingTodayScreen(vm) }
                composable("imports") { TrackerListScreen(vm) }
                composable("detail/{id}") { backStack ->
                    TrainingItemDetailScreen(vm, backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L, nav)
                }
                composable("reports") { ReportsScreen(vm) }
                composable("settings") { SettingsScreen(vm, snackbar) }
            }
        }
    }
}

@Composable
fun BottomNav(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route.orEmpty()
    val items = listOf(
        Triple("home", "Home", Icons.Default.Home),
        Triple("scan", "Scan", Icons.Default.CameraAlt),
        Triple("trackers", "Trackers", Icons.Default.TaskAlt),
        Triple("reports", "Reports", Icons.Default.BarChart),
        Triple("settings", "Settings", Icons.Default.Settings)
    )
    NavigationBar(containerColor = Charcoal, contentColor = Color.White) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = current.startsWith(route),
                onClick = { nav.navigate(route) { launchSingleTop = true } },
                icon = { Icon(icon, label) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val cards by vm.trainingCards.collectAsState()
    val imports by vm.imports.collectAsState()
    AppPage {
        Header(title = "Printout Scanner Pro", subtitle = "Scan work printouts into organized trackers")
        Text("Choose a document type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        DocumentGrid(nav)
        Button(onClick = { nav.navigate("scan") }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Default.CameraAlt, null)
            Spacer(Modifier.width(10.dp))
            Text("Scan New Printout", fontWeight = FontWeight.Bold)
        }
        SectionTitle("Needs Attention", "View all") { nav.navigate("trackers") }
        AttentionRow(Icons.Default.Report, "${cards.count { it.status == TrainingStatus.OVERDUE }} overdue trainings", "Require follow-up", SoftRed)
        AttentionRow(Icons.Default.CalendarMonth, "0 schedule changes detected", "Review and confirm", SoftYellow)
        AttentionRow(Icons.Default.CheckCircle, "0 cleaning tasks due today", "Need to be completed", SoftGreen)
        AttentionRow(Icons.Default.Inventory2, "0 inventory issues flagged", "Review inventory log", Color(0xFFFFE9D6))
        SectionTitle("Recent Imports", "All") { nav.navigate("imports") }
        if (imports.isEmpty()) EmptyState("No confirmed imports yet", "Scan or manually enter a training report to start tracking.")
        imports.take(3).forEach { ImportCard(it) }
    }
}

@Composable
fun Header(title: String, subtitle: String) {
    Surface(color = Charcoal, shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).border(3.dp, Lime, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FileOpen, null, tint = Lime)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFFE4E7DD), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun DocumentGrid(nav: NavHostController) {
    val docs = listOf(
        Triple("Training Report", "Associate training due dates and statuses", Icons.Default.Assignment),
        Triple("Schedule", "Basic placeholder for schedule changes", Icons.Default.CalendarMonth),
        Triple("Cleaning Checklist", "Track cleaning tasks and due items", Icons.Default.TaskAlt),
        Triple("Inventory Sheet", "Flag counts and follow-up issues", Icons.Default.Inventory2),
        Triple("Sign-off Log", "Capture sign-off rows and missing names", Icons.Default.CheckCircle)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        docs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { doc ->
                    DocumentCard(doc.first, doc.second, doc.third, Modifier.weight(1f)) { nav.navigate("picker") }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DocumentCard(title: String, desc: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = modifier.heightIn(min = 94.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Charcoal, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(desc, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun DocumentTypePickerScreen(nav: NavHostController) {
    AppPage {
        Header("Pick Printout Type", "Training Report is fully working in this MVP.")
        listOf(
            "Training Report" to "OCR extracts associate, training, due date, and status rows.",
            "Schedule" to "Placeholder parser ready for future schedule-change detection.",
            "Cleaning Checklist" to "Manual/basic extraction shell for due cleaning tasks.",
            "Inventory Sheet" to "Manual/basic extraction shell for count issues.",
            "Sign-off Log" to "Manual/basic extraction shell for missing sign-offs.",
            "Generic Table" to "Capture rows now, specialize later."
        ).forEach { (title, desc) -> DocumentTypeCard(title, desc) { nav.navigate("scan") } }
    }
}

@Composable
fun DocumentTypeCard(title: String, desc: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, null, tint = Lime)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ScanScreen(vm: AppViewModel, nav: NavHostController, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCamera by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.processImage(uri)
            nav.navigate("review")
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(Unit) { if (!hasCamera) permissionLauncher.launch(Manifest.permission.CAMERA) }

    AppPage {
        Header("Scan Printout", "Keep the page inside the green guides.")
        if (hasCamera) {
            CameraPreview(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                onImageCaptureReady = { imageCapture = it }
            )
        } else {
            EmptyState("Camera permission is off", "You can still import an image from your gallery or enter rows manually.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Cancel") }
            OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(Icons.Default.FileOpen, null)
                Spacer(Modifier.width(6.dp))
                Text("Gallery")
            }
        }
        Button(
            onClick = {
                val capture = imageCapture
                if (capture == null) {
                    vm.useManualEntry()
                    nav.navigate("review")
                } else {
                    capturePrintout(context, capture) { uri, error ->
                        if (uri != null) {
                            vm.processImage(uri)
                            nav.navigate("review")
                        } else {
                            scope.launch { snackbar.showSnackbar(error ?: "Could not capture image.") }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null)
            Spacer(Modifier.width(10.dp))
            Text("Capture Printout", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { vm.useManualEntry(); nav.navigate("review") }, modifier = Modifier.fillMaxWidth()) {
            Text("Enter training report manually")
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier, onImageCaptureReady: (ImageCapture) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    Box(modifier.background(Charcoal, RoundedCornerShape(8.dp))) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    onImageCaptureReady(capture)
                }, ContextCompat.getMainExecutor(context))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        ScanFrameOverlay(Modifier.fillMaxSize())
    }
}

@Composable
fun ScanFrameOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val inset = 28.dp.toPx()
        val len = 42.dp.toPx()
        val stroke = 4.dp.toPx()
        val points = listOf(
            Offset(inset, inset) to Offset(inset + len, inset),
            Offset(inset, inset) to Offset(inset, inset + len),
            Offset(size.width - inset, inset) to Offset(size.width - inset - len, inset),
            Offset(size.width - inset, inset) to Offset(size.width - inset, inset + len),
            Offset(inset, size.height - inset) to Offset(inset + len, size.height - inset),
            Offset(inset, size.height - inset) to Offset(inset, size.height - inset - len),
            Offset(size.width - inset, size.height - inset) to Offset(size.width - inset - len, size.height - inset),
            Offset(size.width - inset, size.height - inset) to Offset(size.width - inset, size.height - inset - len)
        )
        points.forEach { drawLine(Lime, it.first, it.second, strokeWidth = stroke, cap = StrokeCap.Round) }
    }
}

fun capturePrintout(context: Context, imageCapture: ImageCapture, onResult: (Uri?, String?) -> Unit) {
    val file = File(context.cacheDir, "scan-${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(output, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onResult(Uri.fromFile(file), null)
        override fun onError(exception: ImageCaptureException) = onResult(null, exception.message)
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReviewScreen(vm: AppViewModel, nav: NavHostController) {
    val state by vm.reviewState.collectAsState()
    var editing by remember { mutableStateOf<EditableTrainingRow?>(null) }
    AppPage {
        Header("Scan & Review", "Always review OCR before saving.")
        Box(Modifier.fillMaxWidth().height(260.dp).background(Charcoal, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            if (state.imageUri != null) {
                Image(rememberAsyncImagePainter(state.imageUri), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("Manual Training Report", color = Color.White)
            }
            ScanFrameOverlay(Modifier.fillMaxSize())
        }
        AssistChip(onClick = {}, label = { Text(if (state.isProcessing) "Reading text..." else state.message) }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) })
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Extracted Data (${state.rows.size})", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { vm.addBlankRow() }) { Icon(Icons.Default.Add, null); Text("Add Row") }
                }
                state.rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.associate.ifBlank { "Associate" }, fontWeight = FontWeight.Bold)
                            Text("${row.training.ifBlank { "Training" }} • ${row.dueDateMillis?.asDateLabel() ?: "No date"}")
                        }
                        StatusChip(row.status)
                        IconButton(onClick = { editing = row }) { Icon(Icons.Default.Edit, null) }
                        IconButton(onClick = { vm.deleteRow(row.id) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { nav.navigate("scan") }, modifier = Modifier.weight(1f).height(54.dp)) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text("Rescan")
            }
            Button(onClick = { vm.confirmExtraction { nav.navigate("trackers") } }, modifier = Modifier.weight(1.5f).height(54.dp), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("Confirm Extraction")
            }
        }
        Text("Your data stays on this device.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4D6D22), modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    editing?.let { row -> EditRowDialog(row, onDismiss = { editing = null }, onSave = { vm.updateRow(it); editing = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRowDialog(row: EditableTrainingRow, onDismiss: () -> Unit, onSave: (EditableTrainingRow) -> Unit) {
    var associate by remember(row.id) { mutableStateOf(row.associate) }
    var training by remember(row.id) { mutableStateOf(row.training) }
    var date by remember(row.id) { mutableStateOf(row.dueDateMillis) }
    var showDate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Row") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(associate, { associate = it }, label = { Text("Associate") }, singleLine = true)
                OutlinedTextField(training, { training = it }, label = { Text("Training") }, singleLine = true)
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date?.asDateLabel() ?: "Choose due date")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(row.copy(associate = associate, training = training, dueDateMillis = date)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = { date = picker.selectedDateMillis; showDate = false }) { Text("Use Date") }
        }) { DatePicker(picker) }
    }
}

@Composable
fun TrainingRadarScreen(vm: AppViewModel, nav: NavHostController) {
    val cards by vm.trainingCards.collectAsState()
    AppPage {
        Header("Training Radar", "Priority view for confirmed training trackers.")
        val overdue = cards.filter { it.status == TrainingStatus.OVERDUE || daysUntil(it.dueDate) < 0 }
        val today = cards.filter { daysUntil(it.dueDate) == 0 }
        val soon = cards.filter { daysUntil(it.dueDate) in 1..3 }
        val working = cards.filter { it.isWorking }
        val noDate = cards.filter { it.dueDate == null }
        val completed = cards.filter { it.status == TrainingStatus.COMPLETED }
        RadarSection("Overdue", overdue, nav)
        RadarSection("Due Today", today, nav)
        RadarSection("Due Soon", soon, nav)
        RadarSection("Working Today", working, nav)
        BestNextPerson(cards.firstOrNull { it.status != TrainingStatus.COMPLETED })
        RadarSection("No Due Date Found", noDate, nav)
        RadarSection("Completed", completed, nav)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { nav.navigate("associates") }, modifier = Modifier.weight(1f)) { Text("Associates") }
            OutlinedButton(onClick = { nav.navigate("working") }, modifier = Modifier.weight(1f)) { Text("Working Today") }
        }
    }
}

@Composable
fun RadarSection(title: String, cards: List<TrainingCardData>, nav: NavHostController) {
    SectionTitle(title, cards.size.toString()) {}
    if (cards.isEmpty()) EmptyState("Nothing here", "Items move here as due dates and statuses change.")
    cards.forEach { TrainingItemCard(it) { nav.navigate("detail/${it.id}") } }
}

@Composable
fun TrainingItemCard(item: TrainingCardData, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(statusColor(item.status), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = Charcoal)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${item.associateName} — ${item.trainingName}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.priorityReason, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.isWorking) Text("Working today${item.shiftEnd?.let { " until $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF50731D))
            }
            PriorityChip(item.priorityScore)
        }
    }
}

@Composable
fun BestNextPerson(item: TrainingCardData?) {
    SectionTitle("Best Next Person", if (item == null) "0" else "1") {}
    if (item == null) return
    Card(colors = CardDefaults.cardColors(SoftGreen), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF5A8416), modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Send ${item.associateName.substringBefore(" ")} first", fontWeight = FontWeight.Bold)
                Text(item.priorityReason.ifBlank { "Closest due training with a clear next step." })
            }
        }
    }
}

@Composable
fun AssociateDatabaseScreen(vm: AppViewModel) {
    val associates by vm.associates.collectAsState()
    val cards by vm.trainingCards.collectAsState()
    AppPage {
        Header("Associates", "Local associate database")
        associates.forEach { associate ->
            val open = cards.count { it.associateId == associate.id && it.status != TrainingStatus.COMPLETED }
            Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = Lime)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(associate.name, fontWeight = FontWeight.Bold)
                            Text("$open active trainings • ${associate.department ?: "No department"}")
                        }
                    }
                    Text("Notes: ${associate.notes.ifBlank { "None" }}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun WorkingTodayScreen(vm: AppViewModel) {
    val associates by vm.associates.collectAsState()
    val cards by vm.trainingCards.collectAsState()
    AppPage {
        Header("Working Today", "Mark who can realistically train today.")
        associates.forEach { associate ->
            val working = cards.any { it.associateId == associate.id && it.isWorking }
            Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(associate.name, fontWeight = FontWeight.Bold)
                        Text(if (working) "Working until 4:00 PM" else "Off or unknown")
                    }
                    Switch(checked = working, onCheckedChange = { vm.setWorkingToday(associate, it) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingItemDetailScreen(vm: AppViewModel, id: Long, nav: NavHostController) {
    val cards by vm.trainingCards.collectAsState()
    val item = cards.firstOrNull { it.id == id }
    var showDate by remember { mutableStateOf(false) }
    AppPage {
        Header("Training Detail", "Update status, notes, and due date.")
        if (item == null) {
            EmptyState("Item not found", "It may have been deleted.")
            return@AppPage
        }
        TrainingItemCard(item) {}
        listOf(
            "Mark Completed" to TrainingStatus.COMPLETED,
            "Mark Incomplete" to TrainingStatus.NOT_STARTED,
            "Mark Sent to Training" to TrainingStatus.SENT_TO_TRAINING,
            "Mark Could Not Complete" to TrainingStatus.COULD_NOT_COMPLETE,
            "Needs Follow-Up" to TrainingStatus.NEEDS_FOLLOW_UP
        ).forEach { (label, status) ->
            Button(onClick = { vm.updateTrainingStatus(id, status) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
        OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) { Text("Change Due Date") }
        OutlinedButton(onClick = { vm.deleteTraining(id); nav.popBackStack() }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color.Red)) { Text("Delete Item") }
    }
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = item?.dueDate)
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = { vm.updateTrainingDueDate(id, picker.selectedDateMillis); showDate = false }) { Text("Use Date") }
        }) { DatePicker(picker) }
    }
}

@Composable
fun TrackerListScreen(vm: AppViewModel) {
    val imports by vm.imports.collectAsState()
    AppPage {
        Header("All Trackers", "Confirmed imports and manual trackers")
        if (imports.isEmpty()) EmptyState("No imports yet", "Confirmed OCR and manual reports will appear here.")
        imports.forEach { ImportCard(it) }
    }
}

@Composable
fun ReportsScreen(vm: AppViewModel) {
    val cards by vm.trainingCards.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val report = buildReport(cards)
    AppPage {
        Header("Reports", "Copy or share plain-text follow-up summaries.")
        ReportCard("Training Follow-Up Report", report)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { clipboard.setText(AnnotatedString(report)) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, null)
                Spacer(Modifier.width(6.dp))
                Text("Copy Report")
            }
            OutlinedButton(onClick = {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report)
                }, "Share report"))
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(6.dp))
                Text("Share")
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, snackbar: SnackbarHostState) {
    val dueSoon by vm.dueSoonWindow.collectAsState()
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    AppPage {
        Header("Settings", "Smithware Studios • v0.1.0")
        Text("Default due-soon window", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 7, 14).forEach { days ->
                Button(onClick = { vm.setDueSoonWindow(days) }, enabled = dueSoon != days) { Text("$days days") }
            }
        }
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PrivacyTip, null, tint = Lime)
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy", fontWeight = FontWeight.Bold)
                }
                Text("Printout Scanner Pro stores scanned printouts, extracted text, associates, due dates, and notes locally on this device. Smithware Studios does not receive or upload your workplace data in this MVP. Do not scan documents containing sensitive personal information unless you are allowed to store them on your device.")
                Text("Printout Scanner Pro is a personal organization tool. Always follow your workplace's official training, scheduling, and compliance procedures.")
            }
        }
        OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color.Red)) { Text("Clear Local Data") }
        OutlinedButton(onClick = { scope.launch { snackbar.showSnackbar("Database export is a placeholder for a future local file export.") } }, modifier = Modifier.fillMaxWidth()) { Text("Export database placeholder") }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear local data?") },
            text = { Text("This removes local imports, associates, training items, and notes from this device.") },
            confirmButton = { TextButton(onClick = { vm.clearLocalData { confirmClear = false } }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AppPage(content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFEDEBE3)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = { item { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) } }
    )
}

@Composable
fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(action, color = Color(0xFF5A8416), modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
fun AttentionRow(icon: ImageVector, title: String, subtitle: String, color: Color) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(color, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Charcoal) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StatusChip(status: TrainingStatus) {
    AssistChip(onClick = {}, label = { Text(status.name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }) })
}

@Composable
fun PriorityChip(score: Int) {
    val label = when {
        score >= 75 -> "Critical"
        score >= 55 -> "High"
        score >= 30 -> "Medium"
        score == 0 -> "Done"
        else -> "Low"
    }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
fun EmptyState(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ImportCard(import: ImportSummary) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(import.title, fontWeight = FontWeight.Bold)
            Text("${import.documentType.name.replace('_', ' ')} • ${import.importedAt.asDateLabel()} • ${import.rowCount} rows")
            Text("${import.activeCount} active items")
        }
    }
}

@Composable
fun ReportCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(body)
        }
    }
}

fun statusColor(status: TrainingStatus): Color = when (status) {
    TrainingStatus.OVERDUE, TrainingStatus.NEEDS_FOLLOW_UP -> SoftRed
    TrainingStatus.COMPLETED -> SoftGreen
    TrainingStatus.SENT_TO_TRAINING, TrainingStatus.IN_PROGRESS -> SoftYellow
    else -> Color(0xFFEDEDE7)
}

fun daysUntil(millis: Long?): Int {
    if (millis == null) return Int.MAX_VALUE
    val today = LocalDate.now()
    val due = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(today, due).toInt()
}

fun buildReport(cards: List<TrainingCardData>): String {
    val active = cards.filter { it.status != TrainingStatus.COMPLETED }.sortedByDescending { it.priorityScore }
    return buildString {
        appendLine("Training Follow-Up Report")
        if (active.isEmpty()) appendLine("- No active training follow-ups.")
        active.take(20).forEach {
            appendLine("- ${it.associateName} — ${it.trainingName} — ${it.priorityReason.ifBlank { it.dueDate?.asDateLabel() ?: "No due date" }}")
        }
        appendLine()
        appendLine("Personal organization summary only. Follow official workplace procedures.")
    }
}
