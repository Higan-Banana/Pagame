package com.prugo8.pagame.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prugo8.pagame.model.Event

@Composable
fun AddExpenseDialog(
    event: Event,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        payerId: String,
        splitIds: List<String>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedPayerId by remember {
        mutableStateOf(event.participants.firstOrNull()?.id.orEmpty())
    }
    val selectedSplitIds = remember(event.id) {
        mutableStateListOf<String>().apply {
            addAll(event.participants.map { it.id })
        }
    }

    val amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val formIsValid = title.isNotBlank() &&
        amount > 0.0 &&
        selectedPayerId.isNotBlank() &&
        selectedSplitIds.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar gasto", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                enabled = formIsValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                onClick = {
                    onConfirm(
                        title.trim(),
                        amount,
                        selectedPayerId,
                        selectedSplitIds.toList()
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Ej. Pizza o transporte") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto") },
                    leadingIcon = { Text(event.currency, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("¿Quién pagó?", fontWeight = FontWeight.SemiBold)
                event.participants.forEach { participant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPayerId = participant.id }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPayerId == participant.id,
                            onClick = { selectedPayerId = participant.id }
                        )
                        Text(participant.name)
                    }
                }

                Text("¿Entre quiénes se divide?", fontWeight = FontWeight.SemiBold)
                event.participants.forEach { participant ->
                    val selected = participant.id in selectedSplitIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selected) {
                                    selectedSplitIds.remove(participant.id)
                                } else {
                                    selectedSplitIds.add(participant.id)
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (participant.id !in selectedSplitIds) {
                                        selectedSplitIds.add(participant.id)
                                    }
                                } else {
                                    selectedSplitIds.remove(participant.id)
                                }
                            }
                        )
                        Text(participant.name)
                    }
                }

                if (selectedSplitIds.isEmpty()) {
                    Text(
                        "Selecciona al menos una persona.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}
