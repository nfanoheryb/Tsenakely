package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProductEntity
import com.example.ui.model.Formatters

private enum class RestockListFilter(val label: String) {
    ALL("Rehetra"),
    OUT_ONLY("Lany ihany"),
    LOW_ONLY("Kely sisa")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutOfStockListSheet(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onRestockProduct: (productId: Long, addedQty: Double) -> Unit,
    onNotify: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    var filter by remember { mutableStateOf(RestockListFilter.ALL) }
    var restockTarget by remember { mutableStateOf<ProductEntity?>(null) }

    val outProducts = remember(products) {
        products.filter { it.stockQty <= 0.0 }.sortedBy { it.name.lowercase() }
    }
    val lowProducts = remember(products) {
        products.filter { it.stockQty > 0.0 && it.stockQty <= it.minStockAlert }
            .sortedBy { it.name.lowercase() }
    }

    val displayedProducts = remember(outProducts, lowProducts, filter) {
        when (filter) {
            RestockListFilter.ALL -> outProducts + lowProducts
            RestockListFilter.OUT_ONLY -> outProducts
            RestockListFilter.LOW_ONLY -> lowProducts
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lisitry ny entana lany",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${outProducts.size} lany tanteraka • ${lowProducts.size} kely sisa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (displayedProducts.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val text = buildString {
                                appendLine("Lisitry ny entana ho vidiana (Tsenakely):")
                                displayedProducts.forEach { p ->
                                    val status = if (p.stockQty <= 0.0) {
                                        "Lany"
                                    } else {
                                        "Sisa ${Formatters.formatQty(p.stockQty, p.unit)}"
                                    }
                                    appendLine("• ${p.name} — $status (Nividianana: ${Formatters.formatAriary(p.costPriceAr)}/${p.unit})")
                                }
                            }.trim()
                            clipboardManager.setText(AnnotatedString(text))
                            onNotify("Voadika ny lisitry ny entana lany!")
                        },
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("copy_out_of_stock_list_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.desc_copy_list),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adika", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Segmented Filter inside sheet
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RestockListFilter.entries.forEachIndexed { index, option ->
                    val count = when (option) {
                        RestockListFilter.ALL -> outProducts.size + lowProducts.size
                        RestockListFilter.OUT_ONLY -> outProducts.size
                        RestockListFilter.LOW_ONLY -> lowProducts.size
                    }
                    SegmentedButton(
                        selected = filter == option,
                        onClick = { filter = option },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = RestockListFilter.entries.size
                        )
                    ) {
                        Text(
                            text = "${option.label} ($count)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (displayedProducts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (products.isEmpty()) {
                                "Tsy mbola misy entana voasoratra"
                            } else {
                                "Tsy misy entana ao amin'ity sokajy ity"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Rehefa misy entana lany na kely sisa dia hiseho eto ho azy mba ho mora jerena rehefa miantsena.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedProducts, key = { it.id }) { product ->
                        val isOut = product.stockQty <= 0.0
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isOut) {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Inventory2,
                                            contentDescription = null,
                                            tint = if (isOut) {
                                                MaterialTheme.colorScheme.onErrorContainer
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
                                            text = product.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isOut) {
                                                "Lany • Nividianana: ${Formatters.formatAriary(product.costPriceAr)}/${product.unit}"
                                            } else {
                                                "Sisa: ${Formatters.formatQty(product.stockQty, product.unit)} • Vidiny: ${Formatters.formatAriary(product.costPriceAr)}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isOut) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = { restockTarget = product },
                                    modifier = Modifier.height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddBox,
                                        contentDescription = stringResource(R.string.desc_restock),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Tahiry", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Hikatona")
            }
        }
    }

    restockTarget?.let { target ->
        QuickRestockDialog(
            product = target,
            onDismiss = { restockTarget = null },
            onConfirm = { added ->
                onRestockProduct(target.id, added)
                restockTarget = null
            }
        )
    }
}

@Composable
fun QuickRestockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var addedText by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (product.stockQty <= 0.0) "Lany tahiry: ${product.name}"
                else "Hanampy tahiry: ${product.name}"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (product.stockQty <= 0.0) {
                        "Tahiry ankehitriny: Lany (0 ${product.unit})"
                    } else {
                        "Tahiry ankehitriny: ${Formatters.formatQty(product.stockQty, product.unit)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20, 50).forEach { preset ->
                        AssistChip(
                            onClick = { addedText = preset.toString() },
                            label = { Text("+$preset") }
                        )
                    }
                }
                OutlinedTextField(
                    value = addedText,
                    onValueChange = { raw -> addedText = raw.filter { it.isDigit() || it == '.' } },
                    label = { Text("Isan'ny entana nampiana (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val qty = addedText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirm(qty) },
                enabled = qty > 0.0
            ) {
                Text("Ampiana")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Aoka ihany")
            }
        }
    )
}
