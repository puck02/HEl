package com.heldairy.feature.medication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.heldairy.ui.theme.HElDairyTheme
import java.time.LocalDate

@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        startDate: LocalDate,
        frequency: String,
        dose: String?,
        timeHints: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var timeHints by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "添加药品",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("药品名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("用药频率") },
                    placeholder = { Text("例如：每日3次") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("剂量（可选）") },
                    placeholder = { Text("例如：每次1片") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = timeHints,
                    onValueChange = { timeHints = it },
                    label = { Text("用药时间（可选）") },
                    placeholder = { Text("例如：早 / 中 / 晚") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && frequency.isNotBlank()) {
                                onConfirm(
                                    name.trim(),
                                    LocalDate.now(),
                                    frequency.trim(),
                                    dose.takeIf { it.isNotBlank() }?.trim(),
                                    timeHints.takeIf { it.isNotBlank() }?.trim()
                                )
                            }
                        },
                        enabled = name.isNotBlank() && frequency.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewAddMedicationDialog() {
    HElDairyTheme(darkTheme = false) {
        AddMedicationDialog(
            onDismiss = {},
            onConfirm = { _, _, _, _, _ -> }
        )
    }
}
