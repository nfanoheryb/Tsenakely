package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CreditEntryEntity
import com.example.data.local.CreditEntryTypes
import com.example.data.local.CustomerEntity
import com.example.ui.model.Formatters
import com.example.ui.viewmodel.CreditFilter
import com.example.ui.viewmodel.ShopUiState

@Composable
fun CreditScreen(
    uiState: ShopUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (CreditFilter) -> Unit,
    onOpenCustomerDetail: (Long?) -> Unit,
    onAddCustomer: (name: String, phone: String, initialDebtAr: Long, note: String) -> Unit,
    onRecordPayment: (customerId: Long, amountAr: Long, note: String) -> Unit,
    onAddManualCredit: (customerId: Long, amountAr: Long, note: String) -> Unit,
    onDeleteCustomer: (customerId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCustomer = uiState.selectedCustomer
    if (selectedCustomer != null) {
        BackHandler { onOpenCustomerDetail(null) }
        CustomerLedgerDetailScreen(
            customer = selectedCustomer,
            entries = uiState.selectedCustomerEntries,
            onBack = { onOpenCustomerDetail(null) },
            onRecordPayment = { amount, note -> onRecordPayment(selectedCustomer.id, amount, note) },
            onAddCredit = { amount, note -> onAddManualCredit(selectedCustomer.id, amount, note) },
            onDeleteCustomer = { onDeleteCustomer(selectedCustomer.id) },
            modifier = modifier
        )
        return
    }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var paymentTarget by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(
        uiState.customers,
        uiState.creditSearchQuery,
        uiState.creditFilter
    ) {
        uiState.customers.filter { customer ->
            val matchesQuery = uiState.creditSearchQuery.isBlank() ||
                customer.name.contains(uiState.creditSearchQuery, ignoreCase = true) ||
                customer.phone.contains(uiState.creditSearchQuery, ignoreCase = true)
            val matchesFilter = when (uiState.creditFilter) {
                CreditFilter.ALL -> true
                CreditFilter.OWING -> customer.balanceAr > 0L
                CreditFilter.SETTLED -> customer.balanceAr == 0L
            }
            matchesQuery && matchesFilter
        }
    }

    val owingCount = remember(uiState.customers) {
        uiState.customers.count { it.balanceAr > 0L }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Karnety Trosa",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Total Unpaid Debt Hero Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trosa mbola tsy voaloa",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatAriary(uiState.totalDebtAr),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "$owingCount mpanjifa",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TsenakelySearchField(
                value = uiState.creditSearchQuery,
                onValueChange = onSearchChange,
                placeholder = "Hitady anaran'ny mpanjifa...",
                testTag = "credit_search_input"
            )

            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CreditFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.creditFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.customers.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item {
                        TsenakelyEmptyState(
                            title = "Mbola tsy misy karnety trosa",
                            subtitle = "Soraty eto ny mpanjifa maka entana trosa mba tsy ho hadino ny kaonty sy ny fandoavana.",
                            primaryLabel = "Hanampy mpanjifa voalohany",
                            onPrimaryClick = { showAddCustomerDialog = true },
                            primaryTestTag = "empty_credit_add_customer"
                        )
                    }
                }
            } else if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tsy misy mpanjifa mifanaraka amin'ny fikarohana.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onClick = { onOpenCustomerDetail(customer.id) },
                            onPayClick = { paymentTarget = customer }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddCustomerDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_customer_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Rounded.PersonAdd,
                contentDescription = stringResource(R.string.desc_add_customer)
            )
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, debt, note ->
                onAddCustomer(name, phone, debt, note)
                showAddCustomerDialog = false
            }
        )
    }

    paymentTarget?.let { customer ->
        CreditTransactionDialog(
            title = "Hanefa trosa: ${customer.name}",
            currentBalance = customer.balanceAr,
            isPayment = true,
            onDismiss = { paymentTarget = null },
            onConfirm = { amount, note ->
                onRecordPayment(customer.id, amount, note)
                paymentTarget = null
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    onClick: () -> Unit,
    onPayClick: () -> Unit
) {
    val hasDebt = customer.balanceAr > 0L
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomerInitialAvatar(name = customer.name, hasDebt = hasDebt)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (customer.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (hasDebt) "Trosa: ${Formatters.formatAriary(customer.balanceAr)}" else "Voaloa avokoa",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (hasDebt) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                )
            }

            if (hasDebt) {
                FilledTonalButton(
                    onClick = onPayClick,
                    modifier = Modifier.height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hanefa")
                }
            }
        }
    }
}

@Composable
private fun CustomerLedgerDetailScreen(
    customer: CustomerEntity,
    entries: List<CreditEntryEntity>,
    onBack: () -> Unit,
    onRecordPayment: (Long, String) -> Unit,
    onAddCredit: (Long, String) -> Unit,
    onDeleteCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPayDialog by remember { mutableStateOf(false) }
    var showAddCreditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.desc_back)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.desc_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (customer.balanceAr > 0L) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Trosa tavela",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.formatAriary(customer.balanceAr),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showPayDialog = true },
                        enabled = customer.balanceAr > 0L,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hanefa trosa")
                    }
                    OutlinedButton(
                        onClick = { showAddCreditDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hanampy trosa")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Tantaran'ny kaonty",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mbola tsy misy soratra tato amin'ity karnety ity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    val isPayment = entry.type == CreditEntryTypes.PAYMENT
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPayment) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPayment) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isPayment) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = Formatters.formatDateTime(entry.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = (if (isPayment) "- " else "+ ") + Formatters.formatAriary(entry.amountAr),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPayment) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPayDialog) {
        CreditTransactionDialog(
            title = "Fandoavana trosa",
            currentBalance = customer.balanceAr,
            isPayment = true,
            onDismiss = { showPayDialog = false },
            onConfirm = { amount, note ->
                onRecordPayment(amount, note)
                showPayDialog = false
            }
        )
    }

    if (showAddCreditDialog) {
        CreditTransactionDialog(
            title = "Hanampy trosa",
            currentBalance = customer.balanceAr,
            isPayment = false,
            onDismiss = { showAddCreditDialog = false },
            onConfirm = { amount, note ->
                onAddCredit(amount, note)
                showAddCreditDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hamafa mpanjifa?") },
            text = { Text("Hofafana ny karnety sy ny tantaran'i ${customer.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCustomer()
                    }
                ) {
                    Text("Eny, fafao")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Tsia")
                }
            }
        )
    }
}

@Composable
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, initialDebtAr: Long, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var debtText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mpanjifa vaovao") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Anaran'ny mpanjifa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Finday (tsy voatery)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = debtText,
                    onValueChange = { raw -> debtText = raw.filter { it.isDigit() } },
                    label = { Text("Trosa taloha raha misy (Ar)") },
                    suffix = { Text("Ar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if ((debtText.toLongOrNull() ?: 0L) > 0L) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Fanamarihana") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, phone, debtText.toLongOrNull() ?: 0L, note)
                },
                enabled = name.isNotBlank()
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

@Composable
private fun CreditTransactionDialog(
    title: String,
    currentBalance: Long,
    isPayment: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amountAr: Long, note: String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (isPayment && currentBalance > 0L) currentBalance.toString() else "")
    }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Trosa ankehitriny: ${Formatters.formatAriary(currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPayment && currentBalance > 0L) {
                    AssistChip(
                        onClick = { amountText = currentBalance.toString() },
                        label = { Text("Aloavy manontolo (${Formatters.formatAriary(currentBalance)})") }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw -> amountText = raw.filter { it.isDigit() } },
                    label = { Text("Vola (Ar)") },
                    suffix = { Text("Ar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Antony / fanamarihana (tsy voatery)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amount = amountText.toLongOrNull() ?: 0L
            Button(
                onClick = { onConfirm(amount, note) },
                enabled = amount > 0L
            ) {
                Text(if (isPayment) "Voaray" else "Ampidiro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Aoka ihany")
            }
        }
    )
}
