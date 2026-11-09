package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppConfig
import com.example.data.model.MobileRegistration
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IndigoPrimary

@Composable
fun OwnerControlPanelDialog(
    appConfig: AppConfig,
    registeredStudents: List<MobileRegistration>,
    onDismiss: () -> Unit,
    onSaveConfig: (AppConfig) -> Unit,
    onUpdateStudentMembership: (phoneNumber: String, ownerUid: String, activate: Boolean, durationDays: Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Customize App & Prices, 1 = Manage Students

    var announcement by remember(appConfig) { mutableStateOf(appConfig.announcementBanner) }
    var monthlyPrice by remember(appConfig) { mutableStateOf(appConfig.monthlyPriceInr.toString()) }
    var quarterlyPrice by remember(appConfig) { mutableStateOf(appConfig.quarterlyPriceInr.toString()) }
    var annualPrice by remember(appConfig) { mutableStateOf(appConfig.annualPriceInr.toString()) }
    var upiVpa by remember(appConfig) { mutableStateOf(appConfig.upiVpa) }
    var merchantName by remember(appConfig) { mutableStateOf(appConfig.merchantDisplayName) }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .testTag("owner_control_panel_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Owner Control Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Owner Mobile: 6261371961 • Full App Control",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("App & Prices", fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Students (${registeredStudents.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                ) {
                    if (selectedTab == 0) {
                        Text(
                            text = "1. Student Dashboard Notice / Banner",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = announcement,
                            onValueChange = { announcement = it },
                            label = { Text("Custom Notice Banner (Optional)") },
                            placeholder = { Text("e.g. Board Exam 2027 Important Links Added!") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Campaign, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "2. Custom Membership Plan Prices (₹)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = monthlyPrice,
                                onValueChange = { monthlyPrice = it.filter { c -> c.isDigit() } },
                                label = { Text("Monthly (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = quarterlyPrice,
                                onValueChange = { quarterlyPrice = it.filter { c -> c.isDigit() } },
                                label = { Text("Quarterly (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = annualPrice,
                                onValueChange = { annualPrice = it.filter { c -> c.isDigit() } },
                                label = { Text("Annual (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "3. Payment Receiver Settings (Number Masked for Students)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = upiVpa,
                            onValueChange = { upiVpa = it },
                            label = { Text("Receiver UPI ID (Hidden from Students)") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Payment, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = merchantName,
                            onValueChange = { merchantName = it },
                            label = { Text("Merchant Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val updatedConfig = AppConfig(
                                    announcementBanner = announcement.trim(),
                                    monthlyPriceInr = monthlyPrice.toLongOrNull()?.coerceAtLeast(1L) ?: 99L,
                                    quarterlyPriceInr = quarterlyPrice.toLongOrNull()?.coerceAtLeast(1L) ?: 249L,
                                    annualPriceInr = annualPrice.toLongOrNull()?.coerceAtLeast(1L) ?: 699L,
                                    upiVpa = upiVpa.trim().ifBlank { "6261371961@ybl" },
                                    merchantDisplayName = merchantName.trim().ifBlank { "Edustudy pro Study Hub" }
                                )
                                onSaveConfig(updatedConfig)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save All Customizations", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Tab 1: Manage Students & Memberships
                        Text(
                            text = "Control Student Memberships by Mobile Number",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (registeredStudents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No students registered yet. When students register with their mobile number, you can manage their memberships here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            registeredStudents.forEach { student ->
                                val isValid = student.isMembershipValid
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            1.dp,
                                            if (isValid) AmberGold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = student.studentName.ifBlank { "Student" },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Mobile: +91 ${student.phoneNumber}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isValid) AmberGold else MaterialTheme.colorScheme.errorContainer
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isValid) "ACTIVE (${student.daysRemaining}d)" else "LOCKED",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isValid) Color.Black else MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    onUpdateStudentMembership(student.phoneNumber, student.ownerUid, true, 30)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("+30 Days", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    onUpdateStudentMembership(student.phoneNumber, student.ownerUid, true, 365)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
                                            ) {
                                                Text("+1 Year", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }

                                            if (isValid && student.phoneNumber != "6261371961") {
                                                OutlinedButton(
                                                    onClick = {
                                                        onUpdateStudentMembership(student.phoneNumber, student.ownerUid, false, 0)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Lock", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
