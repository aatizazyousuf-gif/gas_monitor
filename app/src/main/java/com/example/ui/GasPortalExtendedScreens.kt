package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// HOMEOWNER PORTAL TABS & SUB-COMPONENTS
// ==========================================

@Composable
fun HomeownerRefillTab(viewModel: GasViewModel) {
    val orders by viewModel.allOrders.collectAsStateWithLifecycle()
    val connections by viewModel.allConnections.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var selectedTankId by remember { mutableStateOf("") }
    var selectedSlot by remember { mutableStateOf("Morning (8 AM - 12 PM)") }
    var successMessage by remember { mutableStateOf("") }

    LaunchedEffect(connections) {
        if (connections.isNotEmpty() && selectedTankId.isEmpty()) {
            selectedTankId = connections.first().connectionId
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("refill_tab_scroll"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Book Refill Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Book Cylinder Refill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Gas Connection:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    if (connections.isEmpty()) {
                        Text(
                            "No registered connections found. Add a connection first.",
                            color = DangerRed,
                            fontSize = 11.sp
                        )
                    } else {
                        connections.forEach { conn ->
                            val isSelected = conn.connectionId == selectedTankId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SafeGreenBg else Color.Transparent)
                                    .border(1.dp, if (isSelected) GreenPrimary else BorderSlate, RoundedCornerShape(8.dp))
                                    .clickable { selectedTankId = conn.connectionId }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(conn.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text("Meter ID: ${conn.connectionId}", fontSize = 11.sp, color = TextSecondary)
                                }
                                Text(
                                    "${String.format("%.1f", (conn.currentGasLiters / conn.tankCapacityLiters) * 100.0)}% Left",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (conn.currentGasLiters < 20.0) DangerRed else GreenPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Delivery Slot:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    val slots = listOf("Morning (8 AM - 12 PM)", "Afternoon (1 PM - 5 PM)", "Evening (6 PM - 9 PM)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slots.forEach { slot ->
                            val isSelected = selectedSlot == slot
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) GasTeal else DarkSlateBg)
                                    .border(1.dp, if (isSelected) GasTeal else BorderSlate, RoundedCornerShape(10.dp))
                                    .clickable { selectedSlot = slot }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slot.split(" ")[0],
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (successMessage.isNotEmpty()) {
                        Surface(
                            color = SafeGreenBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(successMessage, fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedTankId.isNotEmpty()) {
                                viewModel.placeCylinderOrder(selectedTankId, selectedSlot)
                                successMessage = "Cylinder booking placed! Delivery scheduled for $selectedSlot."
                                coroutineScope.launch {
                                    delay(4000)
                                    successMessage = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("book_refill_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedTankId.isNotEmpty()
                    ) {
                        Text("Book Refill Cylinder ($45.00)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Deliveries / Bookings Stepper Header
        item {
            Text(
                "Active Booking Status Tracker",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val activeOrders = orders.filter { it.status != "Delivered" }
        if (activeOrders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No active orders being processed at the moment.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(activeOrders) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSlate, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Booking ID: #${order.id}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                Text("Delivery: ${order.deliverySlot}", color = TextSecondary, fontSize = 11.sp)
                            }
                            Surface(
                                color = when (order.status) {
                                    "Placed" -> Color(0xFFE3F2FD)
                                    "Dispatched" -> Color(0xFFFFF3E0)
                                    else -> SafeGreenBg
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = order.status.uppercase(),
                                    color = when (order.status) {
                                        "Placed" -> Color(0xFF1E88E5)
                                        "Dispatched" -> Color(0xFFFB8C00)
                                        else -> GreenPrimary
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stepper Visualization
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val steps = listOf("Placed", "Dispatched", "Delivered")
                            val currentStepIndex = steps.indexOf(order.status).coerceAtLeast(0)

                            steps.forEachIndexed { index, step ->
                                val isActive = index <= currentStepIndex
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) GreenPrimary else BorderSlate),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isActive) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text((index + 1).toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = step,
                                    fontSize = 10.sp,
                                    fontWeight = if (order.status == step) FontWeight.Bold else FontWeight.Medium,
                                    color = if (order.status == step) GreenPrimary else TextSecondary,
                                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                                )

                                if (index < steps.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(2.dp)
                                            .background(if (index < currentStepIndex) GreenPrimary else BorderSlate)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }

                        if (order.assignedDriver.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderSlate, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalShipping, null, tint = GasTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assigned Driver: ", fontSize = 11.sp, color = TextSecondary)
                                Text(order.assignedDriver, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Booking History
        item {
            Text(
                "Refill History Log",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val completedOrders = orders.filter { it.status == "Delivered" }
        if (completedOrders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No completed cylinder bookings recorded.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(completedOrders) { order ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(CardDarkBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SafeGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Standard 45L Cylinder Refill", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                            Text("Completed on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(order.timestamp))}", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                    Text("$${String.format("%.2f", order.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun HomeownerBillingTab(viewModel: GasViewModel) {
    val bills by viewModel.allBills.collectAsStateWithLifecycle()
    var selectedPaymentMethod by remember { mutableStateOf("Visa ending 4242") }
    val coroutineScope = rememberCoroutineScope()
    var paymentStatusMessage by remember { mutableStateOf("") }

    val unpaidBills = bills.filter { it.status == "UNPAID" }
    val totalAmountDue = unpaidBills.sumOf { it.amountDue }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("billing_tab_scroll"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Amount Due Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = if (totalAmountDue > 0) Color(0xFFFFF8F8) else SafeGreenBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total Outstanding Balance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$${String.format("%.2f", totalAmountDue)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = if (totalAmountDue > 0) DangerRed else GreenPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Payment,
                            contentDescription = null,
                            tint = if (totalAmountDue > 0) DangerRed else GreenPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (totalAmountDue > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderSlate, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Select Payment Method:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        val paymentMethods = listOf("Visa ending 4242", "Mastercard ending 9801", "Digital LPG Wallet")
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            paymentMethods.forEach { method ->
                                val isSelected = selectedPaymentMethod == method
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GasTeal else DarkSlateBg)
                                        .border(1.dp, if (isSelected) GasTeal else BorderSlate, RoundedCornerShape(8.dp))
                                        .clickable { selectedPaymentMethod = method }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        method.split(" ")[0],
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (paymentStatusMessage.isNotEmpty()) {
                            Text(
                                paymentStatusMessage,
                                color = GreenPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val billToPay = unpaidBills.firstOrNull()
                                if (billToPay != null) {
                                    viewModel.payGasBill(billToPay.id, selectedPaymentMethod)
                                    paymentStatusMessage = "Invoice #${billToPay.id} paid successfully via $selectedPaymentMethod!"
                                    coroutineScope.launch {
                                        delay(4000)
                                        paymentStatusMessage = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("pay_invoice_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Settle Outstanding Bill", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Excellent! Your account has a zero balance. No payments are currently due.",
                            fontSize = 11.sp,
                            color = GreenPrimary
                        )
                    }
                }
            }
        }

        // Outstanding invoices list
        item {
            Text(
                "Unpaid Invoices Queue",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (unpaidBills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No unpaid invoices outstanding.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(unpaidBills) { bill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarkBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Invoice #${bill.id} - ${bill.billingPeriod}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        Text("Due by ${bill.dueDate}", fontSize = 10.sp, color = TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$${String.format("%.2f", bill.amountDue)}", fontWeight = FontWeight.Black, color = DangerRed, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Warning, null, tint = AlertAmber, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Billing History
        item {
            Text(
                "Invoice Payment Receipts",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val paidBills = bills.filter { it.status == "PAID" }
        if (paidBills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No paid invoice history found.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(paidBills) { bill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarkBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Invoice #${bill.id} - ${bill.billingPeriod}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        Text("Paid via ${bill.paymentMethod} on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(bill.timestamp))}", fontSize = 10.sp, color = TextSecondary)
                    }
                    Surface(
                        color = SafeGreenBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$${String.format("%.2f", bill.amountDue)} Paid",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeownerTicketsTab(viewModel: GasViewModel) {
    val tickets by viewModel.allServiceTickets.collectAsStateWithLifecycle()
    var ticketType by remember { mutableStateOf("Gas Leak Check") }
    var description by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tickets_tab_scroll"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Submit Ticket Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Build, null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report Leak / Service Request", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Issue Type:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    val issueTypes = listOf("Gas Leak Check", "Meter Calibration", "Hardware Repair", "General Complaint")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        issueTypes.forEach { type ->
                            val isSelected = ticketType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GasTeal else DarkSlateBg)
                                    .border(1.dp, if (isSelected) GasTeal else BorderSlate, RoundedCornerShape(8.dp))
                                    .clickable { ticketType = type }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    type.split(" ")[0] + if (type.contains("Leak")) " 🚨" else "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("ticket_description_input"),
                        label = { Text("Details of Complaint / Failure symptoms") },
                        placeholder = { Text("Example: Smell of sulfur/rotten eggs near primary kitchen meter pipe connection...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = GreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (successMessage.isNotEmpty()) {
                        Surface(
                            color = SafeGreenBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(successMessage, fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (description.isNotEmpty()) {
                                viewModel.submitServiceTicket(ticketType, description)
                                successMessage = "Complaint filed! Support Ticket submitted to supplier technicians."
                                description = ""
                                coroutineScope.launch {
                                    delay(4000)
                                    successMessage = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("submit_ticket_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = description.isNotEmpty()
                    ) {
                        Text("Settle Service Ticket Dispatch", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active ticket tracking list
        item {
            Text(
                "Open & Assigned Dispatch Tickets",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        val activeTickets = tickets.filter { it.status != "Resolved" }
        if (activeTickets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No active service tickets. All clear!", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(activeTickets) { tk ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Build, null, tint = AlertAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tk.ticketType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            }
                            Surface(
                                color = if (tk.status == "Open") Color(0xFFFFEBEE) else Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    tk.status.uppercase(),
                                    color = if (tk.status == "Open") DangerRed else AlertAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(tk.description, fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderSlate, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (tk.assignedTechnician.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape).background(AlertAmber)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assigned Tech: ", fontSize = 11.sp, color = TextSecondary)
                                Text(tk.assignedTechnician, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                            }
                        } else {
                            Text("Awaiting technician dispatch assignment.", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Ticket history
        item {
            Text(
                "Resolved Services Log",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        val resolvedTickets = tickets.filter { it.status == "Resolved" }
        if (resolvedTickets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No completed service tickets found.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(resolvedTickets) { tk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarkBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tk.ticketType, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        }
                        Text("Report: ${tk.description}", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        if (tk.resolutionNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Resolution: ${tk.resolutionNotes}", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeownerChatTab(viewModel: GasViewModel) {
    val chats by viewModel.allChatMessages.collectAsStateWithLifecycle()
    var inputMsg by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    data class HelpChip(val label: String, val message: String, val autoTicketType: String)
    val helpChips = listOf(
        HelpChip("⚠️ Report Gas Leak", "I smell gas near my main cylinder regulator. Please inspect immediately!", "Gas Leak Check"),
        HelpChip("📅 Schedule Calibration", "Hello, I would like to schedule a meter/flow sensor calibration check.", "Meter Calibration"),
        HelpChip("💳 Billing Inquiry", "Hi! I have a question regarding my last LPG cylinder invoice.", ""),
        HelpChip("🚛 Check Delivery ETA", "Where is the active delivery truck for my cylinder refill request?", "")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Text(
            "Supplier Conversation Pipeline",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Text(
            "Tap any quick help desk preset to start a safety/service case:",
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Help Desk Preset Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            helpChips.forEach { chip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (chip.autoTicketType.isNotEmpty()) Color(0xFFFFF1F0) else SafeGreenBg)
                        .border(
                            1.dp,
                            if (chip.autoTicketType.isNotEmpty()) Color(0xFFFFD0CC) else Color(0xFFCDEBD3),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            viewModel.sendChatMessage(chip.message, "HOMEOWNER")
                            if (chip.autoTicketType.isNotEmpty()) {
                                viewModel.submitServiceTicket(chip.autoTicketType, chip.message)
                            }
                            coroutineScope.launch {
                                delay(1200)
                                val automatedReply = when (chip.autoTicketType) {
                                    "Gas Leak Check" -> "🚨 CRITICAL SAFETY ADVISORY: A potential gas leak ticket has been created. A technician has been dispatched. Please immediately close your cylinder valve, open all doors/windows, do not light any matches, and step outside!"
                                    "Meter Calibration" -> "📅 CALIBRATION REQUESTED: We have logged your sensor calibration request. Our technical staff will contact you shortly to coordinate physical access."
                                    else -> if (chip.label.contains("Billing")) {
                                        "💳 BILLING DEPT RESPONSE: We have received your query. Our billing team is reviewing your digital ledger. We will post any adjustment updates directly to your Billing tab."
                                    } else {
                                        "🚛 LOGISTICS ADVISORY: Your order dispatch status has been flagged for prioritized tracking. You can check the live progress bar in the Refills tab."
                                    }
                                }
                                viewModel.sendChatMessage(automatedReply, "SUPPLIER")
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chip.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (chip.autoTicketType.isNotEmpty()) DangerRed else GreenPrimary
                    )
                }
            }
        }

        // Messaging Box Scroll Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DarkSlateBg, RoundedCornerShape(16.dp))
                .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            if (chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages yet. Send a query to Apex LPG Supplier support.", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("chats_scroll_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = false
                ) {
                    items(chats) { msg ->
                        val isMe = msg.senderType == "HOMEOWNER"
                        val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                        val bubbleBg = if (isMe) GasTeal else CardDarkBg
                        val bubbleTextColor = if (isMe) Color.White else TextPrimary
                        val borderMod = if (isMe) Modifier else Modifier.border(1.dp, BorderSlate, RoundedCornerShape(12.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = alignment
                        ) {
                            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 2.dp,
                                                bottomEnd = if (isMe) 2.dp else 12.dp
                                            )
                                        )
                                        .background(bubbleBg)
                                        .then(borderMod)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = msg.message,
                                        color = bubbleTextColor,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isMe) "You" else "LPG Supplier support",
                                    fontSize = 8.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputMsg,
                onValueChange = { inputMsg = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text"),
                placeholder = { Text("Type support query...", fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderSlate,
                    focusedLabelColor = GreenPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputMsg.isNotEmpty()) {
                        val toSend = inputMsg
                        viewModel.sendChatMessage(toSend, "HOMEOWNER")
                        inputMsg = ""

                        // Auto simulated helper replies to keep UX highly engaging
                        coroutineScope.launch {
                            delay(1500)
                            viewModel.sendChatMessage(
                                "This is automated Apex dispatch support. We have received your query: \"$toSend\". A technician is analyzing your volumetric telemetry.",
                                "SUPPLIER"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary)
                    .testTag("send_chat_btn")
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun MultipleConnectionsCard(viewModel: GasViewModel) {
    val connections by viewModel.allConnections.collectAsStateWithLifecycle()
    var isFormExpanded by remember { mutableStateOf(false) }

    var newId by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newCapacity by remember { mutableStateOf("120") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
            .testTag("multiple_connections_card"),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Router, null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Active IoT Connections", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Switch between multiple residential meters", color = TextSecondary, fontSize = 10.sp)
                    }
                }
                
                IconButton(
                    onClick = { isFormExpanded = !isFormExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFormExpanded) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = "Add Meter Connection",
                        tint = GreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable Add Connection Form
            AnimatedVisibility(visible = isFormExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSlateBg, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Register New Tank Connection", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("conn_id_input"),
                        label = { Text("Connection Meter ID", fontSize = 11.sp, color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = TextSecondary
                        )
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("conn_name_input"),
                        label = { Text("Location Name (e.g. Backyard grill)", fontSize = 11.sp, color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = TextSecondary
                        )
                    )

                    OutlinedTextField(
                        value = newCapacity,
                        onValueChange = { newCapacity = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("conn_capacity_input"),
                        label = { Text("Cylinder Capacity (Liters)", fontSize = 11.sp, color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = TextSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (newId.isNotEmpty() && newName.isNotEmpty()) {
                                val cap = newCapacity.toDoubleOrNull() ?: 120.0
                                viewModel.addGasConnection(newId, newName, cap, cap * 0.85)
                                newId = ""
                                newName = ""
                                isFormExpanded = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("save_connection_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add connection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isFormExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Connection Switcher List
            connections.forEach { conn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSlateBg)
                        .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                        .clickable { viewModel.switchActiveConnection(conn) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Cloud,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(conn.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                            Text("ID: ${conn.connectionId}", fontSize = 9.sp, color = TextSecondary)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${String.format("%.1f", conn.currentGasLiters)} / ${conn.tankCapacityLiters} L",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForwardIos, null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AutoReorderConfigCard(viewModel: GasViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    val isEnabled = profile?.isAutoReorderEnabled ?: false
    val threshold = profile?.autoReorderThresholdPercent ?: 15.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
            .testTag("auto_reorder_card"),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Auto-Reorder Threshold", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Order cylinder automatically when tank low", color = TextSecondary, fontSize = 10.sp)
                    }
                }
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.updateAutoReorderSettings(it, threshold) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GreenPrimary),
                    modifier = Modifier.testTag("auto_reorder_toggle")
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderSlate, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Trigger refill at:", fontSize = 11.sp, color = TextSecondary)
                    Text("${String.format("%.0f", threshold)}% Capacity", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GreenPrimary)
                }

                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { viewModel.updateAutoReorderSettings(isEnabled, it.toDouble()) },
                    valueRange = 10f..40f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = GreenPrimary,
                        activeTrackColor = GreenPrimary,
                        inactiveTrackColor = BorderSlate
                    ),
                    modifier = Modifier.testTag("reorder_threshold_slider")
                )
            }
        }
    }
}

@Composable
fun EmergencySOSCard(viewModel: GasViewModel) {
    var isEmergencyActive by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isEmergencyActive) Color(0xFFFFEBEE) else CardDarkBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Emergency SOS Dispatch", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)
                    Text("Immediate threat warning & fire team notification", color = TextSecondary, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEmergencyActive) {
                Surface(
                    color = Color(0xFFBA1A1A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Campaign, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SOS ACTIVE ALARM!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Automated dispatcher completed. Local responder unit & company safety engineer sarah dispatched. Check your email alerts and local fire mains.",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Button(
                onClick = {
                    isEmergencyActive = true
                    viewModel.triggerEmergencySOS()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("sos_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TRIGGER IMMEDIATE EMERGENCY SOS", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PushNotificationHubCard(viewModel: GasViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val orders by viewModel.allOrders.collectAsStateWithLifecycle()
    val activeOrders = orders.filter { it.status != "Delivered" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSlate, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, null, tint = GasTeal, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulated Push Notification Feed", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // notification 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSlateBg, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("SYSTEM BROADCAST: ONLINE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GasTeal)
                        Text("ESP32 gas node calibration constants matched. SMTP channels armed.", fontSize = 10.sp, color = TextSecondary)
                    }
                }

                // notification 2 (depends on orders)
                if (activeOrders.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSlateBg, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            val ord = activeOrders.first()
                            Text("CYLINDER DISPATCHED: REF #${ord.id}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GreenPrimary)
                            Text("Refill cylinder has been loaded onto truck. Estimated delivery: ${ord.deliverySlot}.", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                // notification 3 (depends on profile level)
                if (profile != null && profile!!.currentGasLiters < 25.0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF2F2), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("LOW GAS DANGER WARNING", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DangerRed)
                            Text("Gas levels dropped below warning threshold (${String.format("%.1f", profile!!.currentGasLiters)}L remaining). Reorder advised.", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// GAS COMPANY PORTAL EXTRA SUB-PANELS
// ==========================================

@Composable
fun GasCompanyExtendedPanel(viewModel: GasViewModel) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val orders by viewModel.allOrders.collectAsStateWithLifecycle()
    val tickets by viewModel.allServiceTickets.collectAsStateWithLifecycle()
    val chats by viewModel.allChatMessages.collectAsStateWithLifecycle()
    val profiles by viewModel.userProfile.collectAsStateWithLifecycle()
    val firebaseNotifications by viewModel.firebaseNotifications.collectAsStateWithLifecycle()

    val activeCompanyTab by viewModel.currentCompanyTab.collectAsStateWithLifecycle()
    val companyTabs = listOf("CUSTOMERS", "ORDERS", "BILLING", "TICKETS", "CHAT", "FORECAST", "PROMOS")

    // Local Inventory State
    var depotStock by remember { mutableStateOf(185f) }
    var currentPricePerLiter by remember { mutableStateOf("0.38") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Operations Command Center KPI Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Dispatches En Route
            val activeDispatches = orders.count { it.status == "Dispatched" }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = if (activeDispatches > 0) Color(0xFFE8F3EB) else CardDarkBg)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.LocalShipping,
                        contentDescription = null,
                        tint = if (activeDispatches > 0) GreenPrimary else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ACTIVE TRUCKS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text("$activeDispatches En Route", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }
            }

            // Card 2: Safe Pipelines Alert
            val activeAlertTickets = tickets.count { it.status != "Resolved" }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, if (activeAlertTickets > 0) DangerRed else BorderSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = if (activeAlertTickets > 0) Color(0xFFFFF1F0) else CardDarkBg)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (activeAlertTickets > 0) Icons.Filled.Warning else Icons.Filled.Security,
                        contentDescription = null,
                        tint = if (activeAlertTickets > 0) DangerRed else GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("URGENT ALERTS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(
                        text = if (activeAlertTickets > 0) "$activeAlertTickets Pending" else "All Secure ✓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (activeAlertTickets > 0) DangerRed else GreenPrimary
                    )
                }
            }

            // Card 3: Live Gas Telemetry Flow count
            val completedRefills = orders.count { it.status == "Delivered" }
            val totalLitersDelivered = completedRefills * 45
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Speed,
                        contentDescription = null,
                        tint = GasTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("VOL DELIVERED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text("${totalLitersDelivered}L Total", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }
            }
        }

        // Live Administrative Safety Advisories / Announcements
        val promos by viewModel.allPromos.collectAsStateWithLifecycle()
        val adminBroadcasts = promos.filter { it.companyName == "SYSTEM ADMIN" }
        if (adminBroadcasts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                adminBroadcasts.forEach { broadcast ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, DangerRed, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Campaign, null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "SYSTEM ADMIN SAFETY ANNOUNCEMENT",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = DangerRed,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = broadcast.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = broadcast.content,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Sub tabs selection
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(DarkSlateBg, RoundedCornerShape(12.dp))
                .padding(4.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            companyTabs.forEach { tab ->
                val isSelected = activeCompanyTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GreenPrimary else Color.Transparent)
                        .clickable { viewModel.setCompanyTab(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else TextSecondary)
                }
            }
        }

        when (activeCompanyTab) {
            "CUSTOMERS" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("co_customers_scroll"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Depot Stock Control
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Depot Cylinder Stocks", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text("Local distribution hub inventory levels", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Text("${depotStock.toInt()} Cylinders", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (depotStock < 50f) DangerRed else GreenPrimary)
                                }

                                Slider(
                                    value = depotStock,
                                    onValueChange = { depotStock = it },
                                    valueRange = 0f..300f,
                                    colors = SliderDefaults.colors(thumbColor = GreenPrimary, activeTrackColor = GreenPrimary)
                                )

                                if (depotStock < 60f) {
                                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text("⚠️ WARNING: Distribution depot cylinder supply critical. Schedule bulk resupply.", color = DangerRed, fontSize = 10.sp, modifier = Modifier.padding(8.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Affiliated Accounts & Connections", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }

                    val homeowners = accounts.filter { it.role == "HOMEOWNER" }
                    if (homeowners.isEmpty()) {
                        item {
                            Text("No customers registered yet.", fontSize = 11.sp, color = TextSecondary)
                        }
                    } else {
                        items(homeowners) { ho ->
                            val activeLeaksForUser = firebaseNotifications.filter { !it.isResolved && it.userEmail == ho.email }
                            val hasLeak = activeLeaksForUser.isNotEmpty()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (hasLeak) 2.dp else 1.dp,
                                        color = if (hasLeak) DangerRed else BorderSlate,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (hasLeak) Color(0xFFFFF5F5) else CardDarkBg
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(ho.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                                if (hasLeak) {
                                                    Surface(color = DangerRed, shape = RoundedCornerShape(4.dp)) {
                                                        Text("⚠️ LEAK DETECTED", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text(ho.email, fontSize = 10.sp, color = TextSecondary)
                                        }

                                        Surface(
                                            color = if (ho.isSuspended) Color(0xFFFFEBEE) else SafeGreenBg,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (ho.isSuspended) "SUSPENDED" else "ACTIVE",
                                                color = if (ho.isSuspended) DangerRed else GreenPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (hasLeak) {
                                        val firstLeak = activeLeaksForUser.first()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = DangerRed.copy(alpha = 0.2f), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Sensor Value: ${firstLeak.mq2ValuePpm.toInt()} PPM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                                                Text(firstLeak.message, fontSize = 10.sp, color = TextPrimary)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.resolveFirebaseNotification(firstLeak.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Resolve Alarm", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.submitServiceTicketForUser(
                                                        userEmail = ho.email,
                                                        ticketType = "EMERGENCY_LEAK",
                                                        description = "Emergency response dispatch triggered by Utility supplier for customer ${ho.name} (${ho.email}). Detected MQ-2 gas concentration at ${firstLeak.mq2ValuePpm.toInt()} PPM."
                                                    )
                                                    viewModel.resolveFirebaseNotification(firstLeak.id)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                                modifier = Modifier.weight(1.2f).height(32.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Filled.LocalShipping, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Dispatch Crew", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "ORDERS" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("co_orders_scroll"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Cylinder Bookings Dispatcher Board", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }

                    if (orders.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSlateBg)
                            ) {
                                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No customer cylinder refill requests recorded.", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        items(orders) { ord ->
                            var selectedDriver by remember { mutableStateOf("Sarah Cobb") }
                            val driversList = listOf("Sarah Cobb 🚛", "David Miller 🚚", "Michael Vance 🚒")

                            Card(
                                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Order Ref: #${ord.id}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                            Text("Customer: ${ord.userEmail}", fontSize = 11.sp, color = TextSecondary)
                                            Text("Slot Requested: ${ord.deliverySlot}", fontSize = 11.sp, color = TextSecondary)
                                            if (ord.assignedDriver.isNotEmpty()) {
                                                Text("Assigned Logistics: ${ord.assignedDriver}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GasTeal)
                                            }
                                        }
                                        Surface(
                                            color = when (ord.status) {
                                                "Placed" -> Color(0xFFE3F2FD)
                                                "Dispatched" -> Color(0xFFFFF3E0)
                                                else -> SafeGreenBg
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                ord.status.uppercase(),
                                                color = when (ord.status) {
                                                    "Placed" -> Color(0xFF1E88E5)
                                                    "Dispatched" -> Color(0xFFFB8C00)
                                                    else -> GreenPrimary
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Shipment lifecycle visual progress bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val steps = listOf("Placed", "Dispatched", "Delivered")
                                        val activeIndex = steps.indexOf(ord.status).coerceAtLeast(0)
                                        
                                        steps.forEachIndexed { idx, step ->
                                            val isCompleted = idx <= activeIndex
                                            val stepColor = if (isCompleted) {
                                                if (ord.status == "Dispatched") Color(0xFFFB8C00) else GreenPrimary
                                            } else BorderSlate
                                            
                                            Row(
                                                modifier = Modifier.weight(if (idx < steps.size - 1) 1f else 0.4f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(stepColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isCompleted) {
                                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(step, fontSize = 9.sp, fontWeight = if (idx == activeIndex) FontWeight.Bold else FontWeight.Normal, color = if (idx == activeIndex) TextPrimary else TextSecondary)
                                                
                                                if (idx < steps.size - 1) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(2.dp)
                                                            .background(if (idx < activeIndex) stepColor else BorderSlate.copy(alpha = 0.5f))
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    if (ord.status == "Placed") {
                                        // Driver assignment selection UI
                                        Text("Select Fleet Logistics Personnel:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            driversList.forEach { drv ->
                                                val chipCleanName = drv.split(" ")[0] + " " + drv.split(" ")[1]
                                                val isSelected = selectedDriver == chipCleanName
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) GasTeal.copy(alpha = 0.12f) else DarkSlateBg)
                                                        .border(1.dp, if (isSelected) GasTeal else BorderSlate, RoundedCornerShape(8.dp))
                                                        .clickable { selectedDriver = chipCleanName }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(drv, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) GasTeal else TextSecondary)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                viewModel.updateCylinderOrderStatus(ord.id, "Dispatched", selectedDriver)
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("dispatch_btn_${ord.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GasTeal),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.LocalShipping, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Confirm Dispatch with $selectedDriver", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (ord.status == "Dispatched") {
                                        Button(
                                            onClick = {
                                                viewModel.updateCylinderOrderStatus(ord.id, "Delivered", ord.assignedDriver)
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("complete_btn_${ord.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Mark Cylinder Refilled & Handed Over", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Surface(
                                            color = SafeGreenBg,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.AssignmentTurnedIn, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Cylinder delivered by ${ord.assignedDriver}. Handover secure.",
                                                    color = GreenPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = FontFamily.Monospace
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

            "BILLING" -> {
                var invoiceEmail by remember { mutableStateOf("") }
                var invoiceAmt by remember { mutableStateOf("45.00") }
                var statusMsg by remember { mutableStateOf("") }
                val coroutineScope = rememberCoroutineScope()

                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("co_billing_scroll"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Payment, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create Customer Invoice", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = invoiceEmail,
                                    onValueChange = { invoiceEmail = it },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("bill_cust_email"),
                                    label = { Text("Customer Account Email", fontSize = 11.sp, color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = GreenPrimary,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = GreenPrimary,
                                        unfocusedLabelColor = TextSecondary
                                    )
                                )

                                OutlinedTextField(
                                    value = invoiceAmt,
                                    onValueChange = { invoiceAmt = it },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("bill_amt"),
                                    label = { Text("LPG Utility Charges ($)", fontSize = 11.sp, color = TextSecondary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = GreenPrimary,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = GreenPrimary,
                                        unfocusedLabelColor = TextSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (statusMsg.isNotEmpty()) {
                                    Text(statusMsg, color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                }

                                Button(
                                    onClick = {
                                        if (invoiceEmail.isNotEmpty()) {
                                            val amt = invoiceAmt.toDoubleOrNull() ?: 45.00
                                            viewModel.createGasBill(invoiceEmail, amt, "JULY 2026", "2026-07-28")
                                            statusMsg = "Gas invoice successfully created and mailed to $invoiceEmail!"
                                            invoiceEmail = ""
                                            coroutineScope.launch {
                                                delay(4000)
                                                statusMsg = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("generate_bill_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = invoiceEmail.isNotEmpty()
                                ) {
                                    Text("Publish Invoice Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text("Pricing Administration Manager", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDarkBg, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Global Gas Tariffs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                Text("Standard retail rate per liter", fontSize = 9.sp, color = TextSecondary)
                            }
                            OutlinedTextField(
                                value = currentPricePerLiter,
                                onValueChange = { currentPricePerLiter = it },
                                modifier = Modifier.width(90.dp).height(50.dp).testTag("price_rate_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary)
                            )
                        }
                    }
                }
            }

            "TICKETS" -> {
                val openTickets = tickets.filter { it.status != "Resolved" }
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("co_tickets_scroll"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Active Complaint Dispatch Queue", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }

                    if (openTickets.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSlateBg)) {
                                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No outstanding support tickets reported.", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        items(openTickets) { tk ->
                            var selectedTech by remember { mutableStateOf("Sarah Cobb") }
                            var resolutionNotes by remember { mutableStateOf("") }
                            val techList = listOf("Sarah Cobb 🔧", "David Miller 🛠️", "Michael Vance 📐")

                            Card(
                                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Ticket ID: #${tk.id}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                            Text("Account Owner: ${tk.userEmail}", fontSize = 11.sp, color = TextSecondary)
                                            Text("Reported Issue: ${tk.ticketType}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DangerRed)
                                            if (tk.assignedTechnician.isNotEmpty()) {
                                                Text("Dispatched Specialist: ${tk.assignedTechnician}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GasTeal)
                                            }
                                        }
                                        Surface(
                                            color = when (tk.status) {
                                                "Open" -> Color(0xFFFFEBEE)
                                                "Assigned" -> Color(0xFFFFF3E0)
                                                else -> SafeGreenBg
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                tk.status.uppercase(),
                                                color = when (tk.status) {
                                                    "Open" -> DangerRed
                                                    "Assigned" -> AlertAmber
                                                    else -> GreenPrimary
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Client Description:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text(tk.description, fontSize = 11.sp, color = TextPrimary, modifier = Modifier.padding(vertical = 4.dp))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (tk.status == "Open") {
                                        // Tech selection UI
                                        Text("Dispatch Service Specialist:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            techList.forEach { tech ->
                                                val techCleanName = tech.split(" ")[0] + " " + tech.split(" ")[1]
                                                val isSelected = selectedTech == techCleanName
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) GasTeal.copy(alpha = 0.12f) else DarkSlateBg)
                                                        .border(1.dp, if (isSelected) GasTeal else BorderSlate, RoundedCornerShape(8.dp))
                                                        .clickable { selectedTech = techCleanName }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(tech, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) GasTeal else TextSecondary)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                viewModel.updateServiceTicketStatus(tk.id, "Assigned", selectedTech, "")
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("assign_tech_${tk.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GasTeal),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Engineering, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Dispatch $selectedTech to Site", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // Assigned status -> show resolution notes field
                                        Text("Field Resolution Log entry:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = resolutionNotes,
                                            onValueChange = { resolutionNotes = it },
                                            placeholder = { Text("Log actions taken (e.g., replaced main valve, sensor calibrated...)", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("resolution_notes_${tk.id}"),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                val finalNotes = if (resolutionNotes.isNotEmpty()) resolutionNotes else "Service case resolved. Telemetry channels stable."
                                                viewModel.updateServiceTicketStatus(tk.id, "Resolved", tk.assignedTechnician, finalNotes)
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("resolve_ticket_${tk.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Log Actions & Resolve Support Ticket", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "CHAT" -> {
                var replyMsg by remember { mutableStateOf("") }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("Direct Line Customer Chat Messages", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(DarkSlateBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        if (chats.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No incoming customer support chats.", fontSize = 11.sp, color = TextSecondary)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().testTag("co_chats_list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(chats) { msg ->
                                    val isMe = msg.senderType == "SUPPLIER"
                                    val bubbleBg = if (isMe) GreenPrimary else CardDarkBg
                                    val bubbleTextColor = if (isMe) Color.White else TextPrimary
                                    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                                    
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                                        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(bubbleBg)
                                                    .padding(8.dp)
                                            ) {
                                                Text(msg.message, color = bubbleTextColor, fontSize = 11.sp)
                                            }
                                            Text(if (isMe) "You (Supplier Support)" else msg.userEmail, fontSize = 8.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = replyMsg,
                            onValueChange = { replyMsg = it },
                            modifier = Modifier.weight(1f).testTag("co_reply_input"),
                            placeholder = { Text("Reply to homeowner...", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (replyMsg.isNotEmpty()) {
                                    viewModel.sendChatMessage(replyMsg, "SUPPLIER")
                                    replyMsg = ""
                                }
                            },
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(GreenPrimary).testTag("co_send_reply_btn")
                        ) {
                            Icon(Icons.Filled.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            "FORECAST" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("co_forecast_scroll"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Analytics, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LPG Demand Forecasting", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Using regression curves on flow rate telemetry, we predict when active consumers will reach 15% minimum threshold limits:", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().background(DarkSlateBg, RoundedCornerShape(8.dp)).padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Predictive Target Account", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text("ETA Refill Needed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("homeowner@example.com (Alex Johnson)", fontSize = 11.sp, color = TextPrimary)
                                    Text("In 3 days 🚨", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("rental-prop@example.com (Unit B)", fontSize = 11.sp, color = TextPrimary)
                                    Text("In 12 days", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GreenPrimary)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Map, null, tint = GasTeal, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Algorithmic Delivery Route Optimizer", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Suggested optimized cluster route for Sarah Cobb (Truck #3):", fontSize = 10.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Depot Hub ➔ homeowner@example.com (Priority #1) ➔ Rental Unit B ➔ Return Hub", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = GasTeal, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            "PROMOS" -> {
                var pTitle by remember { mutableStateOf("") }
                var pContent by remember { mutableStateOf("") }
                var pCode by remember { mutableStateOf("") }
                var pCompany by remember { mutableStateOf("Apex Gas Corp") }
                var pSuccess by remember { mutableStateOf("") }
                val coroutineScope = rememberCoroutineScope()

                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("co_promos_scroll"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Campaign, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Publish Customer Promotion", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = pTitle,
                                    onValueChange = { pTitle = it },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("promo_title_input"),
                                    label = { Text("Promotion Title") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary)
                                )

                                OutlinedTextField(
                                    value = pContent,
                                    onValueChange = { pContent = it },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("promo_desc_input"),
                                    label = { Text("Promotion Offer Details") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary)
                                )

                                OutlinedTextField(
                                    value = pCode,
                                    onValueChange = { pCode = it },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("promo_code_input"),
                                    label = { Text("Promo Code (E.g. APEXLOW25)") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (pSuccess.isNotEmpty()) {
                                    Text(pSuccess, color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                }

                                Button(
                                    onClick = {
                                        if (pTitle.isNotEmpty() && pContent.isNotEmpty()) {
                                            viewModel.publishPromo(pTitle, pContent, pCompany, pCode)
                                            pSuccess = "Promotion coupon successfully published to all homeowner dashboards!"
                                            pTitle = ""
                                            pContent = ""
                                            pCode = ""
                                            coroutineScope.launch {
                                                delay(4000)
                                                pSuccess = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("publish_promo_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = pTitle.isNotEmpty() && pContent.isNotEmpty()
                                ) {
                                    Text("Broadcast Promotion", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
// ADMINISTRATIVE PORTAL EXTRA SUB-PANELS
// ==========================================

@Composable
fun AdminExtendedPanel(viewModel: GasViewModel) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    val tickets by viewModel.allServiceTickets.collectAsStateWithLifecycle()

    val activeAdminTab by viewModel.currentAdminTab.collectAsStateWithLifecycle()
    val adminTabs = listOf("ACCOUNTS", "AUDIT_LOGS", "DISPUTES", "ANNOUNCEMENTS")

    var isAuditLogsExpanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(DarkSlateBg, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            adminTabs.forEach { tab ->
                val isSelected = activeAdminTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GreenPrimary else Color.Transparent)
                        .clickable { viewModel.setAdminTab(tab) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab.replace("_", " "), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else TextSecondary)
                }
            }
        }

        when (activeAdminTab) {
            "ACCOUNTS" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("admin_accounts_scroll"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("User Account Administration Console", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }

                    items(accounts) { acc ->
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("Email: ${acc.email}", fontSize = 10.sp, color = TextSecondary)
                                    Text("System Role: ${acc.role}", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = GasTeal)
                                }

                                Button(
                                    onClick = {
                                        viewModel.updateAccountSuspendStatus(acc.email, !acc.isSuspended)
                                    },
                                    modifier = Modifier.height(34.dp).testTag("suspend_btn_${acc.email}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (acc.isSuspended) GreenPrimary else DangerRed
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (acc.isSuspended) "Lift Suspend" else "Suspend", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            "AUDIT_LOGS" -> {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Administrative Audit Trails", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("${auditLogs.size} records", fontSize = 10.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(DarkSlateBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        if (auditLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Audit logging trails are clean. No safety changes captured.", fontSize = 11.sp, color = TextSecondary)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().testTag("admin_audit_logs"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(auditLogs.reversed()) { log ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(log.actionType, fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 11.sp)
                                                Text(
                                                    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp)),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Account: ${log.userEmail}", fontSize = 10.sp, color = TextSecondary)
                                            Text(log.details, fontSize = 11.sp, color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "DISPUTES" -> {
                val complaints = tickets.filter { it.status == "Open" && it.ticketType == "Incorrect Billing" }
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("admin_disputes_scroll"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Billing disputes & arbitration desk", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }

                    if (complaints.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSlateBg)) {
                                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No active billing disputes flagged. Customer-Supplier trust high.", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        items(complaints) { tk ->
                            Card(
                                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Dispute Ref: #${tk.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    Text("Disputant: ${tk.userEmail}", fontSize = 10.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(tk.description, fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.updateServiceTicketStatus(tk.id, "Resolved", "Admin Moderator", "Reviewed dispute. Adjusted billing ledger and issued a $10 courtesy credit.")
                                        },
                                        modifier = Modifier.fillMaxWidth().height(34.dp).testTag("resolve_dispute_${tk.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Settle Dispute: Issue Credit Note", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "ANNOUNCEMENTS" -> {
                var announcementMsg by remember { mutableStateOf("") }
                var statusMsg by remember { mutableStateOf("") }
                val coroutineScope = rememberCoroutineScope()

                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("admin_announcements_scroll"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderSlate, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Campaign, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Broadcast Global safety message", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = announcementMsg,
                                    onValueChange = { announcementMsg = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("announcement_input"),
                                    placeholder = { Text("E.g. SYSTEM ADVISORY: Scheduled cloud telemetry database maintenance tonight at 02:00 UTC...", fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (statusMsg.isNotEmpty()) {
                                    Text(statusMsg, color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                                }

                                Button(
                                    onClick = {
                                        if (announcementMsg.isNotEmpty()) {
                                            viewModel.publishPromo(
                                                title = "System Safety Advisory",
                                                content = announcementMsg,
                                                companyName = "SYSTEM ADMIN",
                                                promoCode = ""
                                            )
                                            statusMsg = "Advisory broadcasted live to all homeowner dashboards!"
                                            announcementMsg = ""
                                            coroutineScope.launch {
                                                delay(4000)
                                                statusMsg = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("broadcast_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = announcementMsg.isNotEmpty()
                                ) {
                                    Text("Broadcast Safety Advisory", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeownerIoTSetupScreen(viewModel: GasViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }

    var wifiSsid by remember { mutableStateOf("My_Home_WiFi") }
    var wifiPassword by remember { mutableStateOf("secure_password_123") }
    var firebaseProjectId by remember { mutableStateOf("smartgasmonitor-vbtqkx") }
    var firebaseApiKey by remember { mutableStateOf("AIzaSyFakeKeyForLocalInitialization_SmartGasMonitor") }
    var inputMeterId by remember { mutableStateOf("") }
    
    var successMsg by remember { mutableStateOf("") }
    var copiedMsg by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(profile) {
        profile?.let {
            firebaseProjectId = it.firebaseProjectId
            firebaseApiKey = it.firebaseApiKey
            if (inputMeterId.isEmpty()) {
                inputMeterId = it.meterId
            }
        }
    }

    val activeReading = allReadings.firstOrNull()
    val isDeviceOnline = activeReading != null && (System.currentTimeMillis() - activeReading.timestamp < 15000)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Header banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, GreenPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "IoT Logo",
                            tint = GreenPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            "Smart LPG IoT Connection Hub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            "Securely pair your ESP32 & MQ-2 sensor to stream live safety data.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Section 1: Active Connection Status & Telemetry Indicators
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1. ACTIVE DEVICE METRICS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GreenPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Linked Meter ID:", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                profile?.meterId ?: "NOT_CONFIGURED",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }

                        // Connection badge
                        val badgeColor = if (isDeviceOnline) SafeGreenBg else Color(0xFFFFF1F1)
                        val badgeTextColor = if (isDeviceOnline) GreenPrimary else DangerRed
                        val badgeLabel = if (isDeviceOnline) "● LIVE ONLINE" else "● OFFLINE (STANDBY)"

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                badgeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.3f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Telemetry Readouts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("MQ-2 Value", fontSize = 10.sp, color = TextSecondary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Warning, null, tint = AlertAmber, modifier = Modifier.size(16.dp))
                                    Text(
                                        "${String.format("%.1f", activeReading?.mq2ValuePpm ?: 0.0)} PPM",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Line Pressure", fontSize = 10.sp, color = TextSecondary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, null, tint = GasTeal, modifier = Modifier.size(16.dp))
                                    Text(
                                        "${String.format("%.1f", activeReading?.pressureDiffPa ?: 0.0)} Pa",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ESP32 Battery", fontSize = 10.sp, color = TextSecondary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Build, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                    Text(
                                        "${String.format("%.2f", activeReading?.batteryVoltage ?: 0.0)} V",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Step-by-Step Device Setup Procedure Wizard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "2. HARDWARE SETUP PROCEDURE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GreenPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Follow this 4-step guide to wire, configure, flash, and connect any ESP32 based gas flow or smoke meter to your secure homeowner profile.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 1: Wire
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Hardware Pin Connections", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(
                                "Connect your MQ-2 Smoke/Gas Sensor to your ESP32 developer board as follows:",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Visual wiring table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSlateBg, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("MQ-2 Pin", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                    Text("ESP32 Node / Pin", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                }
                                HorizontalDivider(color = BorderSlate.copy(alpha = 0.2f), thickness = 0.5.dp)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("VCC (Power)", fontSize = 11.sp, color = TextSecondary)
                                    Text("5V / VIN Pin", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("GND (Ground)", fontSize = 11.sp, color = TextSecondary)
                                    Text("GND Pin", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("AO (Analog Out)", fontSize = 11.sp, color = TextSecondary)
                                    Text("GPIO 34 (A0 Pin)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.15f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 2: Configure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Configure Device Credentials", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(
                                "Enter your local Wi-Fi and device parameters below. We will dynamically inject them directly into your copyable Arduino C++ source code below!",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("Local Wi-Fi SSID (Name)", color = TextSecondary) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().testTag("wifi_ssid_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = GreenPrimary,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedLabelColor = GreenPrimary,
                                    unfocusedLabelColor = TextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("Local Wi-Fi Password", color = TextSecondary) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().testTag("wifi_pass_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = GreenPrimary,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedLabelColor = GreenPrimary,
                                    unfocusedLabelColor = TextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = firebaseProjectId,
                                onValueChange = { firebaseProjectId = it },
                                label = { Text("Firebase Project ID", color = TextSecondary) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().testTag("firebase_project_id_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = GreenPrimary,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedLabelColor = GreenPrimary,
                                    unfocusedLabelColor = TextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = firebaseApiKey,
                                onValueChange = { firebaseApiKey = it },
                                label = { Text("Firebase Web API Key (For REST Auth)", color = TextSecondary) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().testTag("firebase_api_key_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = GreenPrimary,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedLabelColor = GreenPrimary,
                                    unfocusedLabelColor = TextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputMeterId,
                                onValueChange = { inputMeterId = it },
                                label = { Text("Register New IoT Meter ID", color = TextSecondary) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().testTag("iot_meter_id_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = GreenPrimary,
                                    unfocusedBorderColor = BorderSlate,
                                    focusedLabelColor = GreenPrimary,
                                    unfocusedLabelColor = TextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (inputMeterId.isNotBlank() && firebaseProjectId.isNotBlank() && firebaseApiKey.isNotBlank()) {
                                        viewModel.updateIotConfig(inputMeterId, firebaseProjectId, firebaseApiKey)
                                        successMsg = "IoT Configuration updated and meter $inputMeterId bound successfully!"
                                        coroutineScope.launch {
                                            delay(4000)
                                            successMsg = ""
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("save_meter_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Register & Bind Meter ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            AnimatedVisibility(visible = successMsg.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SafeGreenBg),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = successMsg,
                                        fontSize = 11.sp,
                                        color = GreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.15f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 3: Arduino Code Generator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("3", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Flash Custom Arduino C++ Sketch", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(
                                "Copy the dynamically tailored C++ sketch below, paste it into your Arduino IDE, and upload it to your ESP32 board. It will connect to your router and upload readings automatically!",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom generated Arduino Code
                            val currentEmail = profile?.email ?: "homeowner@example.com"
                            val currentMeterId = profile?.meterId ?: inputMeterId

                            val arduinoSketchCode = """
                            #include <WiFi.h>
                            #include <HTTPClient.h>
                            #include <WiFiClientSecure.h>

                            // Local Wi-Fi router parameters
                            const char* ssid = "$wifiSsid";
                            const char* password = "$wifiPassword";

                            // Cloud Firestore telemetry ingestion endpoint
                            const char* firestore_endpoint = "https://firestore.googleapis.com/v1/projects/$firebaseProjectId/databases/(default)/documents/gas_readings?key=$firebaseApiKey";

                            // Homeowner Account & IoT Meter Parameters
                            const char* user_email = "$currentEmail";
                            const char* meter_id = "$currentMeterId";
                            const double mq2_threshold = ${profile?.mq2ThresholdPpm ?: 700.0};

                            // Pin declarations
                            const int mq2_pin = 34; // MQ-2 input (GPIO34)
                            const int alert_led = 2; // ESP32 status led (GPIO2)
                            const int buzzer_pin = 25; // Active buzzer (GPIO25)

                            void setup() {
                              Serial.begin(115200);
                              pinMode(alert_led, OUTPUT);
                              pinMode(buzzer_pin, OUTPUT);
                              
                              WiFi.begin(ssid, password);
                              Serial.print("[IoT] Connecting Wi-Fi");
                              while (WiFi.status() != WL_CONNECTED) {
                                delay(500);
                                Serial.print(".");
                              }
                              Serial.println("\n[IoT] Connected successfully!");
                            }

                            void loop() {
                              if (WiFi.status() == WL_CONNECTED) {
                                int rawValue = analogRead(mq2_pin);
                                // Volumetric gas concentration estimation
                                double ppmValue = (rawValue / 4095.0) * 2000.0;
                                
                                bool isLeak = (ppmValue > mq2_threshold);
                                digitalWrite(alert_led, isLeak ? HIGH : LOW);
                                
                                // Active buzzer tone frequency of 4200 Hz on leak
                                if (isLeak) {
                                  tone(buzzer_pin, 4200);
                                } else {
                                  noTone(buzzer_pin);
                                }
                                
                                double pressurePa = 125.0 + random(-10, 10);
                                double batteryV = 3.96;
                                
                                // Serial monitor outputs for easy diagnostics
                                Serial.println("\n--- [IoT ESP32 Telemetry Monitor] ---");
                                Serial.print("  * MQ-2 Gas Concentration : "); Serial.print(ppmValue); Serial.println(" PPM");
                                Serial.print("  * MQ-2 Threshold         : "); Serial.print(mq2_threshold); Serial.println(" PPM");
                                Serial.print("  * MPXV7002DP Pressure    : "); Serial.print(pressurePa); Serial.println(" Pa");
                                Serial.print("  * Gas Leak Detected      : "); Serial.println(isLeak ? "🚨 YES (Leak Danger!)" : "✅ NO (Safe)");
                                Serial.print("  * Active Buzzer state    : "); Serial.println(isLeak ? "🔊 ACTIVE (4200 Hz)" : "🔇 OFF");
                                Serial.print("  * Battery Voltage        : "); Serial.print(batteryV); Serial.println(" V");
                                Serial.print("  * WiFi Status            : "); Serial.println("Connected");
                                Serial.print("  * Meter ID Bound         : "); Serial.println(meter_id);
                                
                                WiFiClientSecure client;
                                client.setInsecure(); // Skip TLS Certificate validation
                                
                                HTTPClient http;
                                http.begin(client, firestore_endpoint);
                                http.addHeader("Content-Type", "application/json");
                                
                                // Firestore REST schema structure (including email & bound meterId)
                                String payload = "{\"fields\":{"
                                  "\"userEmail\":{\"stringValue\":\"" + String(user_email) + "\"},"
                                  "\"meterId\":{\"stringValue\":\"" + String(meter_id) + "\"},"
                                  "\"timestamp\":{\"integerValue\":\"" + String(millis() + 1626000000000ULL) + "\"},"
                                  "\"mq2ValuePpm\":{\"doubleValue\":" + String(ppmValue) + "},"
                                  "\"pressureDiffPa\":{\"doubleValue\":" + String(pressurePa) + "},"
                                  "\"calculatedFlowRate\":{\"doubleValue\":" + String(isLeak ? 0.0 : 4.5) + "},"
                                  "\"isLeakDetected\":{\"booleanValue\":" + String(isLeak ? "true" : "false") + "},"
                                  "\"wifiConnected\":{\"booleanValue\":true},"
                                  "\"batteryVoltage\":{\"doubleValue\":" + String(batteryV) + "}"
                                  "}}";
                                  
                                Serial.println("[IoT] Uploading telemetry to Cloud Firestore...");
                                int code = http.POST(payload);
                                if (code > 0) {
                                  Serial.println("[IoT] Success! Server response: " + String(code));
                                } else {
                                  Serial.println("[IoT] Upload failed: " + http.errorToString(code));
                                }
                                http.end();
                              } else {
                                Serial.println("[IoT] Wi-Fi link lost. Retrying connection...");
                              }
                              delay(5000); // Send reading every 5 seconds
                            }
                            """.trimIndent()

                            // Styled code block
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = arduinoSketchCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFFD4D4D4)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val clip = android.content.ClipData.newPlainText("ESP32_MQ2_Sketch", arduinoSketchCode)
                                    clipboardManager.setPrimaryClip(clip)
                                    copiedMsg = "Copied to Clipboard! 📋"
                                    coroutineScope.launch {
                                        delay(3000)
                                        copiedMsg = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(38.dp).testTag("copy_sketch_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2EBF2)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, null, tint = GasTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Complete Sketch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GasTeal)
                            }

                            AnimatedVisibility(visible = copiedMsg.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = copiedMsg,
                                    fontSize = 11.sp,
                                    color = GasTeal,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.15f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 4: Test Bench Simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("4", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        val isSimActive by viewModel.isSimActive.collectAsStateWithLifecycle()
                        val simMq2Ppm by viewModel.simMq2Ppm.collectAsStateWithLifecycle()
                        val simBatteryVoltage by viewModel.simBatteryVoltage.collectAsStateWithLifecycle()

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Virtual Live Test Bench", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(
                                "No soldering iron? Use our live simulated pipeline transmitter to test how your dashboard metrics updates in real-time!",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ESP32 Simulated Transmitter", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Switch(
                                    checked = isSimActive,
                                    onCheckedChange = { 
                                        if (it) viewModel.startSimulationLoop() else viewModel.stopSimulationLoop()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPrimary, checkedTrackColor = GreenPrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.testTag("sim_toggle_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // MQ-2 PPM Slider
                            Text("Simulated MQ-2 Gas Level: ${String.format("%.1f", simMq2Ppm)} PPM", fontSize = 11.sp, color = TextSecondary)
                            Slider(
                                value = simMq2Ppm.toFloat(),
                                onValueChange = { viewModel.setSimMq2Ppm(it.toDouble()) },
                                valueRange = 50f..1500f,
                                colors = SliderDefaults.colors(thumbColor = AlertAmber, activeTrackColor = AlertAmber.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().testTag("sim_mq2_slider")
                            )

                            // Battery Slider
                            Text("ESP32 Battery Input: ${String.format("%.2f", simBatteryVoltage)} V", fontSize = 11.sp, color = TextSecondary)
                            Slider(
                                value = simBatteryVoltage.toFloat(),
                                onValueChange = { viewModel.setSimBatteryVoltage(it.toDouble()) },
                                valueRange = 3.3f..4.2f,
                                colors = SliderDefaults.colors(thumbColor = GreenPrimary, activeTrackColor = GreenPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().testTag("sim_batt_slider")
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Troubleshooting Chat Shortcut
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, GreenPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SafeGreenBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "3. TROUBLESHOOTING & SUPPORT CHAT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GreenPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "• Calibration Warning: Raw MQ-2 readings require a 24-48 hour heater burn-in period to stabilize accurately.\n" +
                        "• Multi-Meter Support: Any homeowner can register up to 3 parallel smart IoT meters to partition kitchen, barbecue, or commercial usage panels.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Button(
                        onClick = { viewModel.setTab("CHAT") },
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("iot_support_chat_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Forum, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Chat Support Channel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

