package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.backup.ShopBackupData
import com.example.data.local.ExpenseCategories
import com.example.data.local.ExpenseEntity
import com.example.data.local.PaymentMethods
import com.example.data.local.SaleWithItems
import com.example.ui.model.Formatters
import com.example.ui.viewmodel.ReportPeriodMode
import com.example.ui.viewmodel.ShopUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    uiState: ShopUiState,
    onPeriodModeChange: (ReportPeriodMode) -> Unit,
    onShiftPeriod: (Int) -> Unit,
    onPickDateMillis: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onInspectBackupUri: (Uri) -> Unit,
    onConfirmRestoreBackup: () -> Unit,
    onDismissRestoreBackup: () -> Unit,
    onVoidSale: (SaleWithItems) -> Unit,
    onAddExpense: (title: String, category: String, amountAr: Long) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Varotra, 1 = Fandaniana
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var saleToVoid by remember { mutableStateOf<SaleWithItems?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackupUri(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onInspectBackupUri(uri)
        }
    }

    val report = uiState.periodReport

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tatitra & Caisse",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("backup_restore_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SaveAlt,
                            contentDescription = stringResource(R.string.desc_backup_restore)
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddExpenseDialog = true },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_expense_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.desc_add_expense),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fandaniana")
                    }
                }
            }
        }

        // Period Filter Mode (Andro / Volana / Taona) + Navigator
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ReportPeriodMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = report.mode == mode,
                            onClick = { onPeriodModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ReportPeriodMode.entries.size
                            ),
                            modifier = Modifier.testTag("period_mode_${mode.name.lowercase()}")
                        ) {
                            Text(mode.label)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { onShiftPeriod(-1) },
                            modifier = Modifier.testTag("period_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = stringResource(R.string.desc_prev_period)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = report.mode == ReportPeriodMode.DAY) {
                                    showDatePicker = true
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp)
                                .testTag("period_label_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = stringResource(R.string.desc_pick_date),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = report.periodLabel,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!report.isCurrentPeriod) {
                            AssistChip(
                                onClick = onJumpToToday,
                                label = { Text("Anio") },
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = { onShiftPeriod(1) },
                            modifier = Modifier.testTag("period_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = stringResource(R.string.desc_next_period)
                            )
                        }
                    }
                }
            }
        }

        // Illustrated Hero Cashbook Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_shop_hero),
                    contentDescription = stringResource(R.string.desc_shop_hero),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x701F1A17),
                                    Color(0xE01F1A17)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = report.periodLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${report.salesCount} varotra",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Varotra (Lafo)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = Formatters.formatAriary(report.totalSalesAr),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Tombony",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = Formatters.formatAriary(report.estimatedProfitAr),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFA7F3B5)
                            )
                        }
                    }
                }
            }
        }

        // Period Breakdown Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiMetricCard(
                        label = "Vola madio tavela",
                        value = Formatters.formatAriary(report.netCashAr),
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    KpiMetricCard(
                        label = "Varotra Cash",
                        value = Formatters.formatAriary(report.cashSalesAr),
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiMetricCard(
                        label = "Trosa voaloa",
                        value = Formatters.formatAriary(report.debtRepaidAr),
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                    KpiMetricCard(
                        label = "Varotra Trosa",
                        value = Formatters.formatAriary(report.creditSalesAr),
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (report.expensesAr > 0L) {
                    KpiMetricCard(
                        label = "Fandaniana (${report.periodLabel})",
                        value = "- ${Formatters.formatAriary(report.expensesAr)}",
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = MaterialTheme.colorScheme.error,
                        trailingHint = "Lisitra",
                        onClick = { selectedSection = 1 }
                    )
                }
            }
        }

        // Segmented switcher for History
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Varotra (${report.filteredSales.size})")
                }
                SegmentedButton(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Fandaniana (${report.filteredExpenses.size})")
                }
            }
        }

        if (selectedSection == 0) {
            if (report.filteredSales.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tsy nisy varotra tamin'io fotoana io",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ampiasao ny zana-tsipìka na tsindrio ny daty raha hijery andro, volana na taona hafa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(report.filteredSales, key = { it.sale.id }) { saleWithItems ->
                    SaleHistoryCard(
                        saleWithItems = saleWithItems,
                        onVoidClick = { saleToVoid = saleWithItems }
                    )
                }
            }
        } else {
            if (report.filteredExpenses.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tsy nisy fandaniana tamin'io fotoana io",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tsindrio ny '+ Fandaniana' raha misy vola nivoaka (saran-dalana, fividianana entana, jiro...).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(report.filteredExpenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        onDelete = { onDeleteExpense(expense.id) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Formatters.localMillisToUtcDatePickerMillis(report.anchorMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onPickDateMillis(Formatters.utcDatePickerMillisToLocalNoon(utcMillis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Ekena")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Aoka ihany")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            productsCount = uiState.products.size,
            customersCount = uiState.customers.size,
            salesCount = uiState.sales.size,
            expensesCount = uiState.expenses.size,
            onDismiss = { showBackupDialog = false },
            onExportClick = {
                showBackupDialog = false
                exportLauncher.launch(Formatters.suggestedBackupFileName())
            },
            onImportClick = {
                showBackupDialog = false
                importLauncher.launch(arrayOf("application/json", "*/*"))
            }
        )
    }

    uiState.pendingRestoreBackup?.let { backup ->
        ConfirmRestoreBackupDialog(
            backup = backup,
            onConfirm = onConfirmRestoreBackup,
            onDismiss = onDismissRestoreBackup
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, category, amount ->
                onAddExpense(title, category, amount)
                showAddExpenseDialog = false
            }
        )
    }

    saleToVoid?.let { target ->
        AlertDialog(
            onDismissRequest = { saleToVoid = null },
            title = { Text("Hanafoana varotra?") },
            text = {
                Text(
                    "Hofafana ny varotra ${Formatters.formatAriary(target.sale.totalAr)} ary haverina ao amin'ny tahiry ireo entana?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onVoidSale(target)
                        saleToVoid = null
                    }
                ) {
                    Text("Eny, foano")
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToVoid = null }) {
                    Text("Tsia")
                }
            }
        )
    }
}

@Composable
private fun BackupRestoreDialog(
    productsCount: Int,
    customersCount: Int,
    salesCount: Int,
    expensesCount: Int,
    onDismiss: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tahiry & Famerenana Angona") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tehirizo ho rakitra ao amin'ny findainao ny angon'ny Tsenakely rehetra mba ho azo averina raha manova finday na manala (désinstaller) ny app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ankehitriny: $productsCount entana • $customersCount mpanjifa • $salesCount varotra • $expensesCount fandaniana",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Button(
                    onClick = onExportClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_backup_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hitahiry kopia (Export)")
                }
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_backup_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hamerina avy amin'ny kopia")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hikatona")
            }
        }
    )
}

@Composable
private fun ConfirmRestoreBackupDialog(
    backup: ShopBackupData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hamerina ity kopia ity?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Rakitra tamin'ny ${Formatters.formatDateTime(backup.exportedAt)}:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• Entana: ${backup.products.size}\n• Mpanjifa: ${backup.customers.size}\n• Varotra: ${backup.sales.size}\n• Fandaniana: ${backup.expenses.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Fanamarihana: Hosoloina ireo angona ankehitriny raha tohizana.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_restore_backup_button")
            ) {
                Text("Eny, avereno")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tsia")
            }
        }
    )
}

@Composable
private fun SaleHistoryCard(
    saleWithItems: SaleWithItems,
    onVoidClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sale = saleWithItems.sale
    val isCredit = sale.paymentMethod == PaymentMethods.CREDIT

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCredit) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = if (isCredit) "Trosa: ${sale.customerName ?: ""}" else "Vola (Cash)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCredit) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Formatters.formatDateTime(sale.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = sale.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) 5 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatAriary(sale.totalAr),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    saleWithItems.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.productName} (${Formatters.formatQty(item.quantity, item.unit)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatAriary((item.unitPriceAr * item.quantity).toLong()),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tombony: ${Formatters.formatAriary(sale.estimatedProfitAr)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        TextButton(onClick = onVoidClick) {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Foano ny varotra")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${expense.category} • ${Formatters.formatDateTime(expense.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "- ${Formatters.formatAriary(expense.amountAr)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.desc_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, amountAr: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategories.LIST.first()) }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hanampy fandaniana") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Antony (ohatra: Saran-dalana)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Sokajy", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExpenseCategories.LIST) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw -> amountText = raw.filter { it.isDigit() } },
                    label = { Text("Vola lany (Ar)") },
                    suffix = { Text("Ar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amount = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onConfirm(title, category, amount) },
                enabled = title.isNotBlank() && amount > 0L
            ) {
                Text("Tehirizo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Aoka ihany")
            }
        }
    )
}
