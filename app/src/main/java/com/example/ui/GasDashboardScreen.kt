package com.example.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AlertRecord
import com.example.data.GasPromo
import com.example.data.GasReading
import com.example.data.UserProfile
import com.example.data.FirebaseNotification
import com.example.data.EmailAlertRecord
import com.example.data.CylinderOrder
import com.example.data.GasBill
import com.example.data.ServiceTicket
import com.example.data.GasConnection
import com.example.data.SupplierChatMessage
import com.example.data.AuditLog
import com.example.data.UserAccount
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GasDashboardScreen(
    viewModel: GasViewModel,
    modifier: Modifier = Modifier
) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val readings by viewModel.allReadings.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val allAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val promos by viewModel.allPromos.collectAsStateWithLifecycle()

    // Simulator slider states
    val simPressure by viewModel.simPressurePa.collectAsStateWithLifecycle()
    val simMq2 by viewModel.simMq2Ppm.collectAsStateWithLifecycle()
    val simWifi by viewModel.simWifiConnected.collectAsStateWithLifecycle()
    val simBattery by viewModel.simBatteryVoltage.collectAsStateWithLifecycle()
    val isSimActive by viewModel.isSimActive.collectAsStateWithLifecycle()

    var showSimulatorPanel by remember { mutableStateOf(false) }

    // Auth State
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    if (!isLoggedIn) {
        LoginSignupScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = CardDarkBg,
                    drawerContentColor = TextPrimary,
                    modifier = Modifier.width(280.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDDE2F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentRole == "HOMEOWNER") Icons.Filled.Person else if (currentRole == "GAS_COMPANY") Icons.Filled.Business else Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Role Icon",
                                    tint = Color(0xFF151B2C),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (currentRole == "HOMEOWNER") (profile?.name ?: "Alex Johnson") else if (currentRole == "GAS_COMPANY") "Apex Supplier" else "System Administrator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (currentRole == "HOMEOWNER") (profile?.email ?: "homeowner@example.com") else if (currentRole == "GAS_COMPANY") "company@example.com" else "admin@example.com",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        HorizontalDivider(color = BorderSlate, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "NAVIGATION MENU",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        )

                        // Navigation Items
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (currentRole == "HOMEOWNER") {
                                val homeownerTabs = listOf("DASHBOARD", "REFILLS", "BILLING", "TICKETS", "CHAT", "ALERTS", "PROMOTIONS", "EMAILS", "IOT_METER")
                                items(homeownerTabs) { tab ->
                                    val isSelected = currentTab == tab
                                    val displayName = if (tab == "IOT_METER") "IOT METER SETUP" else tab
                                    NavigationDrawerItem(
                                        label = { Text(displayName, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setTab(tab)
                                            scope.launch { drawerState.close() }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = when (tab) {
                                                    "DASHBOARD" -> Icons.Filled.Dashboard
                                                    "REFILLS" -> Icons.Filled.LocalShipping
                                                    "BILLING" -> Icons.Filled.ReceiptLong
                                                    "TICKETS" -> Icons.Filled.Build
                                                    "CHAT" -> Icons.Filled.Chat
                                                    "ALERTS" -> Icons.Filled.Warning
                                                    "PROMOTIONS" -> Icons.Filled.Campaign
                                                    "EMAILS" -> Icons.Filled.Email
                                                    else -> Icons.Filled.Router
                                                },
                                                contentDescription = tab,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                                            unselectedContainerColor = Color.Transparent,
                                            selectedIconColor = GreenPrimary,
                                            unselectedIconColor = TextSecondary,
                                            selectedTextColor = GreenPrimary,
                                            unselectedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            } else if (currentRole == "GAS_COMPANY") {
                                val companyTabs = listOf("CUSTOMERS", "ORDERS", "BILLING", "TICKETS", "CHAT", "FORECAST", "PROMOS")
                                items(companyTabs) { tab ->
                                    val activeCompanyTab by viewModel.currentCompanyTab.collectAsStateWithLifecycle()
                                    val isSelected = activeCompanyTab == tab
                                    NavigationDrawerItem(
                                        label = { Text(tab, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setCompanyTab(tab)
                                            scope.launch { drawerState.close() }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = when (tab) {
                                                    "CUSTOMERS" -> Icons.Filled.People
                                                    "ORDERS" -> Icons.Filled.LocalShipping
                                                    "BILLING" -> Icons.Filled.ReceiptLong
                                                    "TICKETS" -> Icons.Filled.Engineering
                                                    "CHAT" -> Icons.Filled.Forum
                                                    "FORECAST" -> Icons.Filled.Timeline
                                                    else -> Icons.Filled.Campaign
                                                },
                                                contentDescription = tab,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                                            unselectedContainerColor = Color.Transparent,
                                            selectedIconColor = GreenPrimary,
                                            unselectedIconColor = TextSecondary,
                                            selectedTextColor = GreenPrimary,
                                            unselectedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            } else {
                                val adminTabs = listOf("ACCOUNTS", "AUDIT_LOGS", "DISPUTES", "ANNOUNCEMENTS")
                                items(adminTabs) { tab ->
                                    val activeAdminTab by viewModel.currentAdminTab.collectAsStateWithLifecycle()
                                    val isSelected = activeAdminTab == tab
                                    NavigationDrawerItem(
                                        label = { Text(tab.replace("_", " "), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setAdminTab(tab)
                                            scope.launch { drawerState.close() }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = when (tab) {
                                                    "ACCOUNTS" -> Icons.Filled.ManageAccounts
                                                    "AUDIT_LOGS" -> Icons.Filled.ReceiptLong
                                                    "DISPUTES" -> Icons.Filled.Gavel
                                                    else -> Icons.Filled.Campaign
                                                },
                                                contentDescription = tab,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                                            unselectedContainerColor = Color.Transparent,
                                            selectedIconColor = GreenPrimary,
                                            unselectedIconColor = TextSecondary,
                                            selectedTextColor = GreenPrimary,
                                            unselectedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSlate, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                        // Quick Sign Out Option
                        Button(
                            onClick = {
                                viewModel.logout()
                                scope.launch { drawerState.close() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.fillMaxWidth().height(38.dp).testTag("drawer_logout_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.ExitToApp, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) {
            Scaffold(
                floatingActionButton = {
                    if (currentRole == "HOMEOWNER" && currentTab != "CHAT") {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.setTab("CHAT") },
                            containerColor = GreenPrimary,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("homeowner_chat_fab"),
                            shape = RoundedCornerShape(16.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Filled.Chat, "Chat Support", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Support Chat", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DarkSlateBg,
                            titleContentColor = TextPrimary
                        ),
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Open Drawer Menu",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Smart Gas Monitor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showSimulatorPanel = !showSimulatorPanel },
                                modifier = Modifier.testTag("simulator_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (showSimulatorPanel) Icons.Filled.SettingsInputHdmi else Icons.Filled.Router,
                                    contentDescription = "Toggle ESP32 Simulator Panel",
                                    tint = if (isSimActive) GreenPrimary else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                )
            },
            containerColor = DarkSlateBg,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // Main Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Global Emergency Header (Pulsing Red)
                    if (activeAlerts.any { it.alertType == "LEAK_DANGER" }) {
                        EmergencyPulsingBanner(
                            alertMessage = "CRITICAL LEAK DETECTED!",
                            onResolve = { viewModel.resolveAllAlerts() }
                        )
                    }

                    // Main Layout Switching based on Tenant Role
                    when (currentRole) {
                        "HOMEOWNER" -> HomeownerDashboard(
                            profile = profile,
                            readings = readings,
                            activeAlerts = activeAlerts,
                            promos = promos,
                            currentTab = currentTab,
                            onTabChange = { viewModel.setTab(it) },
                            onResolveAlert = { viewModel.resolveAlert(it) },
                            viewModel = viewModel
                        )
                        "GAS_COMPANY" -> GasCompanyPanel(
                            readings = readings,
                            activeAlerts = activeAlerts,
                            onPublishPromo = { title, content, co, code ->
                                viewModel.publishPromo(title, content, co, code)
                            },
                            viewModel = viewModel
                        )
                        "ADMIN" -> AdminConfigurationPanel(
                            profile = profile,
                            allAlerts = allAlerts,
                            viewModel = viewModel
                        )
                    }
                }

                // Collapsible Simulator Side Drawer Panel
                AnimatedVisibility(
                    visible = showSimulatorPanel,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(310.dp)
                            .background(CardDarkBg)
                            .border(1.dp, BorderSlate, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                            .padding(16.dp)
                    ) {
                        SimulatorDrawerContent(
                            isSimActive = isSimActive,
                            simPressure = simPressure,
                            simMq2 = simMq2,
                            simWifi = simWifi,
                            simBattery = simBattery,
                            onToggleSim = {
                                if (isSimActive) viewModel.stopSimulationLoop() else viewModel.startSimulationLoop()
                            },
                            onPressureChange = { viewModel.setSimPressurePa(it) },
                            onMq2Change = { viewModel.setSimMq2Ppm(it) },
                            onWifiToggle = { viewModel.setSimWifiConnected(it) },
                            onBatteryChange = { viewModel.setSimBatteryVoltage(it) },
                            onManualPulse = { viewModel.manualInsertTelemetry() },
                            calK = profile?.mpxCalibrationK ?: 0.45,
                            thresholdPpm = profile?.mq2ThresholdPpm ?: 700.0
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSignupScreen(
    viewModel: GasViewModel,
    modifier: Modifier = Modifier
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var selectedRole by remember { mutableStateOf("HOMEOWNER") } // "HOMEOWNER", "GAS_COMPANY", "ADMIN"
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SMART GAS",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontSize = 24.sp,
                    color = TextPrimary
                )
                Text(
                    text = " MONITOR",
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.5.sp,
                    fontSize = 24.sp,
                    color = GasTeal
                )
            }
            Text(
                text = "Secure Telemetry & Safety Portal",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDarkBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SELECT ROLE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = GasTeal,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSlateBg)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("HOMEOWNER", "Homeowner", Icons.Filled.Home),
                            Triple("GAS_COMPANY", "Company", Icons.Filled.Business),
                            Triple("ADMIN", "Owner", Icons.Filled.Settings)
                        ).forEach { (roleCode, label, icon) ->
                            val isSelected = selectedRole == roleCode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) GasTeal else Color.Transparent)
                                    .clickable {
                                        selectedRole = roleCode
                                        viewModel.clearLoginError()
                                        if (roleCode == "ADMIN") {
                                            isLoginMode = true
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoginMode) "Access Account" else "Register Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        
                        if (selectedRole != "ADMIN") {
                            Text(
                                text = if (isLoginMode) "Sign Up" else "Log In",
                                color = GasTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        isLoginMode = !isLoginMode
                                        viewModel.clearLoginError()
                                    }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(if (selectedRole == "GAS_COMPANY") "Company / Utility Name" else "Full Name", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GasTeal,
                                unfocusedBorderColor = BorderSlate,
                                focusedLabelColor = GasTeal,
                                unfocusedLabelColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GasTeal,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = GasTeal,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GasTeal,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = GasTeal,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                        singleLine = true
                    )
                    
                    if (!isLoginMode && selectedRole == "HOMEOWNER") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Installation Address", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null, tint = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GasTeal,
                                unfocusedBorderColor = BorderSlate,
                                focusedLabelColor = GasTeal,
                                unfocusedLabelColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("auth_address_input"),
                            singleLine = true
                        )
                    }
                    
                    if (selectedRole == "ADMIN") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Admin console access is reserved. Pre-configured: admin@smartgas.com / admin123",
                            fontSize = 11.sp,
                            color = AlertAmber,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loginError ?: "",
                            color = DangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                viewModel.login(email, password, selectedRole)
                            } else {
                                viewModel.signup(name, email, password, selectedRole, address)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GasTeal),
                        modifier = Modifier.fillMaxWidth().testTag("auth_submit_btn")
                    ) {
                        Text(
                            text = if (isLoginMode) "Log In to Portal" else "Create Account",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "QUICK AUTO-FILL DEMO CREDENTIALS",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        email = "homeowner@example.com"
                        password = "password"
                        selectedRole = "HOMEOWNER"
                        isLoginMode = true
                        viewModel.clearLoginError()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardDarkBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                ) {
                    Text("Homeowner", fontSize = 10.sp, color = TextPrimary, maxLines = 1)
                }
                Button(
                    onClick = {
                        email = "company@example.com"
                        password = "password"
                        selectedRole = "GAS_COMPANY"
                        isLoginMode = true
                        viewModel.clearLoginError()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardDarkBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                ) {
                    Text("Company", fontSize = 10.sp, color = TextPrimary, maxLines = 1)
                }
                Button(
                    onClick = {
                        email = "admin@smartgas.com"
                        password = "admin123"
                        selectedRole = "ADMIN"
                        isLoginMode = true
                        viewModel.clearLoginError()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardDarkBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                ) {
                    Text("Admin Owner", fontSize = 10.sp, color = TextPrimary, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Homeowner Mode Components ---

@Composable
fun HomeownerDashboard(
    profile: UserProfile?,
    readings: List<GasReading>,
    activeAlerts: List<AlertRecord>,
    promos: List<GasPromo>,
    currentTab: String,
    onTabChange: (String) -> Unit,
    onResolveAlert: (Long) -> Unit,
    viewModel: GasViewModel
) {
    var showNotifDialog by remember { mutableStateOf(false) }
    val firebaseNotifications by viewModel.firebaseNotifications.collectAsStateWithLifecycle()
    val activeFirebaseNotifs = firebaseNotifications.filter { notif ->
        !notif.isResolved && (profile?.email?.let { notif.userEmail == it } ?: false)
    }

    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = GasTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Real-Time Safety & Service Alerts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeFirebaseNotifs.isEmpty() && activeAlerts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No pending alerts or notifications.", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        if (activeAlerts.isNotEmpty()) {
                            Text("System / Sensor Alarms:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            activeAlerts.forEach { alert ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                                    border = BorderStroke(1.dp, DangerRed)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Alert: ${alert.alertType}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                                            Text(alert.message, fontSize = 10.sp, color = TextPrimary)
                                        }
                                        Button(
                                            onClick = { onResolveAlert(alert.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Mute", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (activeFirebaseNotifs.isNotEmpty()) {
                            Text("Cloud Security Notifications:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            activeFirebaseNotifs.forEach { notif ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                    border = BorderStroke(1.dp, GasTeal)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Safety Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GasTeal)
                                            Text(notif.message, fontSize = 10.sp, color = TextPrimary)
                                        }
                                        Button(
                                            onClick = { viewModel.resolveFirebaseNotification(notif.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GasTeal),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Dismiss", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifDialog = false }) {
                    Text("Close", color = GasTeal)
                }
            },
            containerColor = CardDarkBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (currentTab == "DASHBOARD") {
            // Sleek profile welcome header from the theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDDE2F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile icon",
                            tint = Color(0xFF151B2C),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Welcome back,",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = profile?.name ?: "Alex Johnson",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
                
                // Sleek notification icon card with active alarm indicator
                val hasPendingAlerts = activeAlerts.isNotEmpty() || activeFirebaseNotifs.isNotEmpty()
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .clickable { showNotifDialog = true }
                        .testTag("homeowner_notif_bell_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = if (hasPendingAlerts) GasTeal else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (hasPendingAlerts) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DangerRed)
                        )
                    }
                }
            }
        }

        when (currentTab) {
            "DASHBOARD" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // System Admin Safety Broadcast / Advisory Announcement
                    val adminBroadcasts = promos.filter { it.companyName == "SYSTEM ADMIN" }
                    if (adminBroadcasts.isNotEmpty()) {
                        item {
                            adminBroadcasts.forEach { broadcast ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, DangerRed, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Campaign, null, tint = DangerRed, modifier = Modifier.size(24.dp))
                                            Text(
                                                text = "SYSTEM SAFETY BROADCAST",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = DangerRed,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = broadcast.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = broadcast.content,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Gauge & Quick Info Block
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDarkBg, RoundedCornerShape(16.dp))
                                .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular tank gauge
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val capacity = profile?.tankCapacityLiters ?: 120.0
                                val current = profile?.currentGasLiters ?: 84.5
                                val percentage = (current / capacity).coerceIn(0.0, 1.0)
                                val angle = percentage * 360f

                                // Animate level indicator color
                                val indicatorColor by animateColorAsState(
                                    targetValue = when {
                                        percentage > 0.4 -> GreenPrimary
                                        percentage > 0.15 -> AlertAmber
                                        else -> DangerRed
                                    },
                                    label = "levelColor"
                                )

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Background circle track
                                    drawCircle(
                                        color = BorderSlate,
                                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    // Foreground progress arch
                                    drawArc(
                                        color = indicatorColor,
                                        startAngle = -90f,
                                        sweepAngle = angle.toFloat(),
                                        useCenter = false,
                                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${String.format("%.1f", percentage * 100)}%",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                    Text(
                                        text = "CYLINDER",
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Tank Stats Text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.name ?: "Cylinder Meter 01",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Addr: ${profile?.address ?: ""}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Meter ID: ${profile?.meterId ?: ""}",
                                    color = GasTeal,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Divider(color = BorderSlate, thickness = 1.dp)

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Remaining", color = TextSecondary, fontSize = 10.sp)
                                        Text(
                                            "${String.format("%.1f", profile?.currentGasLiters ?: 84.5)} L",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Cylinder Max", color = TextSecondary, fontSize = 10.sp)
                                        Text(
                                            "${String.format("%.1f", profile?.tankCapacityLiters ?: 120.0)} L",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Telemetry Cards Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val latestReading = readings.firstOrNull()

                            // MPXV7002DP card
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Filled.Compress,
                                        contentDescription = null,
                                        tint = GasTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GasTeal.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "MPXV7002DP",
                                            color = GasTeal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Differential Pressure", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = "${String.format("%.1f", latestReading?.pressureDiffPa ?: 0.0)} Pa",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Flow: ${String.format("%.2f", latestReading?.calculatedFlowRate ?: 0.0)} L/min",
                                    color = GreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Q = K * √|ΔP|",
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            }

                            // MQ-2 Sensor card
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                val mq2Val = latestReading?.mq2ValuePpm ?: 110.0
                                val isDanger = mq2Val > (profile?.mq2ThresholdPpm ?: 700.0)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (isDanger) DangerRed else GreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                (if (isDanger) DangerRed else GreenPrimary).copy(
                                                    alpha = 0.15f
                                                )
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "MQ-2 GAS",
                                            color = if (isDanger) DangerRed else GreenPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Ambient Leak Index", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = "${String.format("%.1f", mq2Val)} PPM",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isDanger) "LEAK DANGER!" else "AIR IS SECURE",
                                    color = if (isDanger) DangerRed else GreenPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Limit: ${profile?.mq2ThresholdPpm?.toInt()} PPM",
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    // Raw Gas Sensor Meter (1-1000)
                    item {
                        val latestReading = readings.firstOrNull()
                        GasSensorMeter(value = (latestReading?.mq2ValuePpm ?: 110.0).toFloat())
                    }

                    // Canvas Consumption Curve
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDarkBg, RoundedCornerShape(16.dp))
                                .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Consumption Analytics",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Real-time Volumetric Flow Rate (L/min)",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(
                                    Icons.Filled.ShowChart,
                                    contentDescription = null,
                                    tint = GreenPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Draw Native Custom Flow Chart
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                val chartReadings = readings.take(15).reversed()
                                if (chartReadings.isNotEmpty()) {
                                     FlowCurveCanvas(readings = chartReadings)
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No flow history available. Start simulator.",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Past readings (Live Stream)", color = TextSecondary, fontSize = 9.sp)
                                Text("Newest ➔", color = TextSecondary, fontSize = 9.sp)
                            }
                        }
                    }

                    // Recharts Styled Firestore consumption graph
                    item {
                        val firestoreReadings by viewModel.firestoreReadings.collectAsStateWithLifecycle()
                        val isFirestoreLoading by viewModel.isFirestoreLoading.collectAsStateWithLifecycle()

                        RechartsStyledDashboardChart(
                            readingsFromFirestore = firestoreReadings,
                            isFirestoreLoading = isFirestoreLoading,
                            onRefresh = { viewModel.fetchFirestoreReadings() }
                        )
                    }

                    // PDF Summary Sheet Generation
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDarkBg, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Need Consumption History?",
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Generate compiled safety & flow reports",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.manualInsertTelemetry() },
                                colors = ButtonDefaults.buttonColors(containerColor = GasTeal),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Log", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Supplier Support Desk Chat Quick Card
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDarkBg, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Supplier Support Desk",
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Direct two-way safety & dispatch support chat",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = { onTabChange("CHAT") },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Chat", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Multiple Connections Switching / Adding
                    item {
                        MultipleConnectionsCard(viewModel = viewModel)
                    }

                    // Auto-Reorder Threshold config
                    item {
                        AutoReorderConfigCard(viewModel = viewModel)
                    }

                    // Emergency SOS Trigger
                    item {
                        EmergencySOSCard(viewModel = viewModel)
                    }

                    // Simulated push notifications logger
                    item {
                        PushNotificationHubCard(viewModel = viewModel)
                    }
                }
            }

            "REFILLS" -> {
                HomeownerRefillTab(viewModel = viewModel)
            }
            "BILLING" -> {
                HomeownerBillingTab(viewModel = viewModel)
            }
            "TICKETS" -> {
                HomeownerTicketsTab(viewModel = viewModel)
            }
            "CHAT" -> {
                HomeownerChatTab(viewModel = viewModel)
            }

            "ALERTS" -> {
                if (activeAlerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "All Systems Operational",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                "No active leaks or pressure errors detected.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Active Pipeline Faults (${activeAlerts.size})",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Resolve All",
                                    color = GasTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onResolveAlert(-1) }
                                )
                            }
                        }

                        items(activeAlerts) { alert ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        if (alert.severity == "CRITICAL") DangerRed else AlertAmber,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Error,
                                            contentDescription = null,
                                            tint = if (alert.severity == "CRITICAL") DangerRed else AlertAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = alert.alertType,
                                            color = if (alert.severity == "CRITICAL") DangerRed else AlertAmber,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
                                            Date(alert.timestamp)
                                        ),
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = alert.message,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onResolveAlert(alert.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (alert.severity == "CRITICAL") DangerRed else AlertAmber
                                    ),
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text("Mark Resolved", fontSize = 11.sp, color = DarkSlateBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            "PROMOTIONS" -> {
                if (promos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No utility broadcasts available.", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        items(promos) { promo ->
                            val isSystemAdmin = promo.companyName == "SYSTEM ADMIN"
                            var acknowledged by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSystemAdmin) Color(0xFFFFF9E6) else CardDarkBg, RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSystemAdmin) 1.5.dp else 1.dp,
                                        color = if (isSystemAdmin) AlertAmber else BorderSlate,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSystemAdmin) {
                                            Icon(
                                                Icons.Filled.Campaign,
                                                contentDescription = null,
                                                tint = AlertAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "OFFICIAL SAFETY BROADCAST",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                color = AlertAmber
                                            )
                                        } else {
                                            Text(
                                                text = promo.companyName,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = GasTeal
                                            )
                                        }
                                    }
                                    Text(
                                        text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(
                                            Date(promo.timestamp)
                                        ),
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = promo.title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSystemAdmin) DangerRed else TextPrimary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = promo.content,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                if (isSystemAdmin) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { acknowledged = !acknowledged },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (acknowledged) SafeGreenBg else Color(0xFFFFF1F0),
                                            contentColor = if (acknowledged) GreenPrimary else AlertAmber
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            if (acknowledged) Icons.Filled.CheckCircle else Icons.Filled.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (acknowledged) "Safety Advisory Acknowledged ✓" else "Mark Advisory as Acknowledged",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (promo.promoCode.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkSlateBg, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "COUPON CODE:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                        Text(
                                            promo.promoCode,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "EMAILS" -> {
                EmailAlertsLogsScreen(viewModel = viewModel)
            }
            "IOT_METER" -> {
                HomeownerIoTSetupScreen(viewModel = viewModel)
            }
        }
    }
}

// --- Gas Utility Company Module Components ---

@Composable
fun GasCompanyPanel(
    readings: List<GasReading>,
    activeAlerts: List<AlertRecord>,
    onPublishPromo: (String, String, String, String) -> Unit,
    viewModel: GasViewModel
) {
    val firebaseNotifications by viewModel.firebaseNotifications.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FirebaseNotificationBanner(
            notifications = firebaseNotifications,
            currentUserEmail = "company",
            currentUserRole = "GAS_COMPANY",
            onResolve = { viewModel.resolveFirebaseNotification(it) }
        )

        GasCompanyExtendedPanel(viewModel = viewModel)
    }
}

@Composable
fun OldGasCompanyPanel(
    readings: List<GasReading>,
    activeAlerts: List<AlertRecord>,
    onPublishPromo: (String, String, String, String) -> Unit,
    viewModel: GasViewModel
) {
    var promoTitle by remember { mutableStateOf("") }
    var promoContent by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("Apex Gas Corp") }
    var promoCode by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    val firestoreReadings by viewModel.firestoreReadings.collectAsStateWithLifecycle()
    val isFirestoreLoading by viewModel.isFirestoreLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Firebase Cloud Alerts
        item {
            val firebaseNotifications by viewModel.firebaseNotifications.collectAsStateWithLifecycle()
            FirebaseNotificationBanner(
                notifications = firebaseNotifications,
                currentUserEmail = "company",
                currentUserRole = "GAS_COMPANY",
                onResolve = { viewModel.resolveFirebaseNotification(it) }
            )
        }

        // SMTP Email Alerts Dispatcher Console Card
        item {
            var isExpanded by remember { mutableStateOf(false) }
            val emailAlerts by viewModel.emailAlerts.collectAsStateWithLifecycle()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = GreenPrimary
                            )
                            Column {
                                Text(
                                    text = "Automated Email Alerts Monitor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Outgoing leak notification SMTP logs",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = SafeGreenBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${emailAlerts.size} SENT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }
                    
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (emailAlerts.isEmpty()) {
                            Text(
                                text = "No email dispatches triggered yet. Trigger or simulate a gas leak to monitor SMTP delivery.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                emailAlerts.take(6).forEach { alert ->
                                    EmailAlertItemView(alert = alert)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Stats Cards of Grid Customers
        item {
            Text(
                "Grid Meter Telemetry Overview",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total connected customer meters
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CardDarkBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Monitored Nodes", color = TextSecondary, fontSize = 10.sp)
                    Text("348 Meters", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: ALL CHANNELS ACTIVE", color = GreenPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Grid Leakage alerts
                val leaks = activeAlerts.filter { it.alertType == "LEAK_DANGER" }.size
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CardDarkBg, RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (leaks > 0) DangerRed else BorderSlate,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text("Active Leak Incidents", color = TextSecondary, fontSize = 10.sp)
                    Text("$leaks Alerts", color = if (leaks > 0) DangerRed else TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (leaks > 0) "CRITICAL VALVE INTERRUPTS" else "All pipelines sealed",
                        color = if (leaks > 0) DangerRed else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Firestore Analytics (Recharts styled area graph)
        item {
            RechartsStyledDashboardChart(
                readingsFromFirestore = firestoreReadings,
                isFirestoreLoading = isFirestoreLoading,
                onRefresh = { viewModel.fetchFirestoreReadings() }
            )
        }

        // Live Grid Visualizer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Virtual Customer Grid Nodes",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Simulate other customer meters grid
                val latestLocalReading = readings.firstOrNull()
                val gridNodes = listOf(
                    Triple("Meter 99B (Current)", latestLocalReading?.calculatedFlowRate ?: 0.0, latestLocalReading?.isLeakDetected ?: false),
                    Triple("Meter 42C", 0.0, false),
                    Triple("Meter 12A", 4.12, false),
                    Triple("Meter 87D", 1.25, false),
                    Triple("Meter 03F", 0.0, false),
                    Triple("Meter 55E", 8.42, false)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gridNodes.forEach { (nodeName, flow, isLeak) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSlateBg, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isLeak) DangerRed else if (flow > 0) GasTeal else TextSecondary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    nodeName,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (flow > 0) "${String.format("%.2f", flow)} L/min" else "Idle",
                                    color = if (flow > 0) GreenPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            (if (isLeak) DangerRed else if (flow > 0) GasTeal else TextSecondary).copy(
                                                alpha = 0.15f
                                            )
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isLeak) "LEAK" else if (flow > 0) "FLOWING" else "CLOSED",
                                        color = if (isLeak) DangerRed else if (flow > 0) GasTeal else TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Publish Promotions / Maintenance Announcements console
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Broadcast Utility Notification",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    "Announcements immediately sync onto homeowner device feeds.",
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Sender Company Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = GasTeal,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = GasTeal,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("promo_company_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = promoTitle,
                    onValueChange = { promoTitle = it },
                    label = { Text("Notice Title (e.g. Scheduled Maintenance)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = GasTeal,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = GasTeal,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("promo_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = promoContent,
                    onValueChange = { promoContent = it },
                    label = { Text("Notice Content / Announcement Description", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = GasTeal,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = GasTeal,
                        unfocusedBorderColor = BorderSlate
                    ),
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .testTag("promo_desc_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it },
                    label = { Text("Optional Promo / Inspection Code", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = GasTeal,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = GasTeal,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("promo_code_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (promoTitle.isNotBlank() && promoContent.isNotBlank()) {
                            onPublishPromo(promoTitle, promoContent, companyName, promoCode)
                            promoTitle = ""
                            promoContent = ""
                            promoCode = ""
                            focusManager.clearFocus()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GasTeal),
                    modifier = Modifier.fillMaxWidth().testTag("promo_submit_btn")
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast to Customers", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// --- Admin Configuration & Safety Module Panel ---

@Composable
fun AdminConfigurationPanel(
    profile: UserProfile?,
    allAlerts: List<AlertRecord>,
    viewModel: GasViewModel
) {
    val firebaseNotifications by viewModel.firebaseNotifications.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FirebaseNotificationBanner(
            notifications = firebaseNotifications,
            currentUserEmail = "admin",
            currentUserRole = "ADMIN",
            onResolve = { viewModel.resolveFirebaseNotification(it) }
        )

        AdminExtendedPanel(viewModel = viewModel)
    }
}

@Composable
fun OldAdminConfigurationPanel(
    profile: UserProfile?,
    allAlerts: List<AlertRecord>,
    viewModel: GasViewModel
) {
    val calibrationK by viewModel.calibrationKStr.collectAsStateWithLifecycle()
    val thresholdPpm by viewModel.thresholdPpmStr.collectAsStateWithLifecycle()

    var editName by remember { mutableStateOf(profile?.name ?: "") }
    var editAddress by remember { mutableStateOf(profile?.address ?: "") }
    var editCapacity by remember { mutableStateOf(profile?.tankCapacityLiters?.toString() ?: "120.0") }
    var editAffiliatedCompanyEmail by remember { mutableStateOf(profile?.affiliatedCompanyEmail ?: "company@example.com") }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(profile) {
        if (profile != null) {
            editName = profile.name
            editAddress = profile.address
            editAffiliatedCompanyEmail = profile.affiliatedCompanyEmail
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                "IoT Calibration & Hardware Settings",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // MPXV7002DP Flow Constants & MQ-2 Alarm Config
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Sensor Parameter Calibration",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    "Calibrate flow calculations and dangerous leak thresholds.",
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // K parameter config
                OutlinedTextField(
                    value = calibrationK,
                    onValueChange = { viewModel.setCalibrationK(it) },
                    label = { Text("MPXV7002DP Flow Constant (K)", color = TextSecondary) },
                    supportingText = { Text("Formula: Q = K * sqrt(|delta_P|). Range: 0.1 - 2.0", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = AlertAmber,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = AlertAmber,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("cal_k_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // MQ-2 smoke threshold config
                OutlinedTextField(
                    value = thresholdPpm,
                    onValueChange = { viewModel.setThresholdPpm(it) },
                    label = { Text("MQ-2 LPG/Smoke Warning Level (PPM)", color = TextSecondary) },
                    supportingText = { Text("Trigger safety alarm above this limit. Standard: 700 PPM", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = AlertAmber,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = AlertAmber,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("cal_threshold_input")
                )
            }
        }

        // Profile & User Management Screen
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "User & Gas Meter Identity",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Customer Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = AlertAmber,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = AlertAmber,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_customer_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editAddress,
                    onValueChange = { editAddress = it },
                    label = { Text("Service Installation Address", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = AlertAmber,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = AlertAmber,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_customer_address")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editCapacity,
                    onValueChange = { editCapacity = it },
                    label = { Text("Cylinder Max Capacity (Liters)", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = AlertAmber,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = AlertAmber,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_customer_capacity")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editAffiliatedCompanyEmail,
                    onValueChange = { editAffiliatedCompanyEmail = it },
                    label = { Text("Affiliated Gas Company Email", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = AlertAmber,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = AlertAmber,
                        unfocusedBorderColor = BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_customer_company_email")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val capVal = editCapacity.toDoubleOrNull() ?: 120.0
                        viewModel.updateProfileSettings(editName, editAddress, capVal, editAffiliatedCompanyEmail)
                        focusManager.clearFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_identity_btn")
                ) {
                    Text("Save Identity Info", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Administrative Utility Tools (Reset, Refill Tank)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "System Reset Operations",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.resetCylinder() },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier.weight(1f).testTag("refill_tank_btn")
                    ) {
                        Text("Refill Tank", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.clearAllHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.weight(1f).testTag("clear_history_btn")
                    ) {
                        Text("Clear History", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // System Log Audit
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarkBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Pipeline Safety Historical Log",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val historicalAlerts = allAlerts.take(15)
                if (historicalAlerts.isEmpty()) {
                    Text("No safety events logged.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        historicalAlerts.forEach { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSlateBg, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.message,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "Type: ${alert.alertType} • Severity: ${alert.severity}",
                                        color = TextSecondary,
                                        fontSize = 8.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            (if (alert.isResolved) GreenPrimary else DangerRed).copy(
                                                alpha = 0.15f
                                            )
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (alert.isResolved) "RESOLVED" else "ACTIVE",
                                        color = if (alert.isResolved) GreenPrimary else DangerRed,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
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

// --- Interactive ESP32 Telemetry Simulator Panel ---

@Composable
fun SimulatorDrawerContent(
    isSimActive: Boolean,
    simPressure: Double,
    simMq2: Double,
    simWifi: Boolean,
    simBattery: Double,
    onToggleSim: () -> Unit,
    onPressureChange: (Double) -> Unit,
    onMq2Change: (Double) -> Unit,
    onWifiToggle: (Boolean) -> Unit,
    onBatteryChange: (Double) -> Unit,
    onManualPulse: () -> Unit,
    calK: Double,
    thresholdPpm: Double
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Router,
                    contentDescription = null,
                    tint = if (isSimActive) GreenPrimary else TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "ESP32 Sim Bench",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            Switch(
                checked = isSimActive,
                onCheckedChange = { onToggleSim() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GreenPrimary
                ),
                modifier = Modifier.testTag("sim_active_switch")
            )
        }

        Text(
            text = "Generate virtual live sensor telemetry to inspect alerts, charts, and calculations.",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Divider(color = BorderSlate, modifier = Modifier.padding(vertical = 12.dp))

        // Live calculation debug block
        val currentFlow = if (simPressure > 0) calK * sqrt(simPressure) else 0.0
        val isLeaking = simMq2 > thresholdPpm

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSlateBg, RoundedCornerShape(8.dp))
                .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Column {
                Text("VIRTUAL CALCULATIONS", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = GasTeal)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Flow equation: Q = K * √|ΔP|", fontSize = 10.sp, color = TextSecondary)
                Text(
                    "• Q = $calK * √${String.format("%.1f", simPressure)} = ${String.format("%.2f", currentFlow)} L/min",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "• MQ-2 state: ${if (isLeaking) "🚨 OUT OF BOUNDS ($simMq2 PPM > $thresholdPpm PPM)" else "✅ SAFE"}",
                    fontSize = 11.sp,
                    color = if (isLeaking) DangerRed else GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Slider 1: MPXV7002DP (Pressure delta Pa)
        Text(
            "MPXV7002DP Sensor (Pa)",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 12.sp
        )
        Text("Differential Pressure (range: -200 to 1000 Pa)", color = TextSecondary, fontSize = 10.sp)
        Slider(
            value = simPressure.toFloat(),
            onValueChange = { onPressureChange(it.toDouble()) },
            valueRange = -200f..1000f,
            colors = SliderDefaults.colors(
                thumbColor = GasTeal,
                activeTrackColor = GasTeal,
                inactiveTrackColor = BorderSlate
            ),
            modifier = Modifier.testTag("pressure_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-200 Pa", color = TextSecondary, fontSize = 9.sp)
            Text("${String.format("%.1f", simPressure)} Pa", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("1000 Pa", color = TextSecondary, fontSize = 9.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Slider 2: MQ-2 Gas sensor (PPM)
        Text(
            "MQ-2 Ambient Gas (PPM)",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 12.sp
        )
        Text("Leak concentrations (range: 0 to 1000 PPM)", color = TextSecondary, fontSize = 10.sp)
        Slider(
            value = simMq2.toFloat(),
            onValueChange = { onMq2Change(it.toDouble()) },
            valueRange = 0f..1000f,
            colors = SliderDefaults.colors(
                thumbColor = if (isLeaking) DangerRed else GreenPrimary,
                activeTrackColor = if (isLeaking) DangerRed else GreenPrimary,
                inactiveTrackColor = BorderSlate
            ),
            modifier = Modifier.testTag("mq2_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0 PPM", color = TextSecondary, fontSize = 9.sp)
            Text("${String.format("%.1f", simMq2)} PPM", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("1000 PPM", color = TextSecondary, fontSize = 9.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wifi Connection Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ESP32 Wi-Fi Signal", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                Text(if (simWifi) "Wi-Fi Stack: CONNECTED" else "Wi-Fi Stack: DISCONNECTED", color = TextSecondary, fontSize = 10.sp)
            }
            Switch(
                checked = simWifi,
                onCheckedChange = { onWifiToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GasTeal
                ),
                modifier = Modifier.testTag("wifi_toggle")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Battery Voltage Slider
        Text(
            "ESP32 Battery Voltage (V)",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 12.sp
        )
        Slider(
            value = simBattery.toFloat(),
            onValueChange = { onBatteryChange(it.toDouble()) },
            valueRange = 3.2f..4.2f,
            colors = SliderDefaults.colors(
                thumbColor = AlertAmber,
                activeTrackColor = AlertAmber,
                inactiveTrackColor = BorderSlate
            ),
            modifier = Modifier.testTag("battery_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("3.2 V (Empty)", color = TextSecondary, fontSize = 9.sp)
            Text("${String.format("%.2f", simBattery)} V", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("4.2 V (Full)", color = TextSecondary, fontSize = 9.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onManualPulse() },
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            modifier = Modifier.fillMaxWidth().testTag("pulse_send_btn")
        ) {
            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pulse Send Now", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- Emergency Pulsing Danger Alert Banner ---

@Composable
fun EmergencyPulsingBanner(
    alertMessage: String,
    onResolve: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DangerRed.copy(alpha = alpha))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Filled.Dangerous,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = alertMessage,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "LPG leak detected by MQ-2 sensor. Main gas valve closed.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 10.sp
                )
            }
        }

        Button(
            onClick = { onResolve() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Shut Valve", color = DangerRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

// --- Native Curved Bezier Canvas Usage Chart ---

@Composable
fun FlowCurveCanvas(readings: List<GasReading>) {
    val maxFlow = (readings.maxOfOrNull { it.calculatedFlowRate } ?: 5.0).coerceAtLeast(3.0)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg.copy(alpha = 0.3f))
    ) {
        val width = size.width
        val height = size.height

        val paddingLeft = 40.dp.toPx()
        val paddingRight = 10.dp.toPx()
        val paddingTop = 15.dp.toPx()
        val paddingBottom = 15.dp.toPx()

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        // Draw horizontal grid guide lines & Y labels
        val gridLines = 3
        for (i in 0..gridLines) {
            val yFraction = i.toFloat() / gridLines
            val yCoord = paddingTop + yFraction * graphHeight
            
            // Draw guideline
            drawLine(
                color = BorderSlate.copy(alpha = 0.3f),
                start = Offset(paddingLeft, yCoord),
                end = Offset(width - paddingRight, yCoord),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw curve path
        val points = readings.mapIndexed { index, reading ->
            val xFraction = if (readings.size > 1) index.toFloat() / (readings.size - 1) else 0.5f
            val yFraction = reading.calculatedFlowRate / maxFlow
            
            val x = paddingLeft + xFraction * graphWidth
            val y = paddingTop + (1.0f - yFraction).toFloat() * graphHeight
            Offset(x, y)
        }

        if (points.size > 1) {
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val pPrev = points[i - 1]
                    val pCurr = points[i]
                    // Smooth quadratic Bezier control points
                    val controlX = (pPrev.x + pCurr.x) / 2
                    quadraticTo(controlX, pPrev.y, controlX, pCurr.y)
                    lineTo(pCurr.x, pCurr.y)
                }
            }

            // Fill gradient path underneath
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, paddingTop + graphHeight)
                lineTo(points.first().x, paddingTop + graphHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(GasTeal.copy(alpha = 0.25f), Color.Transparent),
                    startY = paddingTop,
                    endY = paddingTop + graphHeight
                )
            )

            drawPath(
                path = strokePath,
                color = GasTeal,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw interactive point ticks
            points.forEachIndexed { index, point ->
                // Draw glow halo around active newest reading
                if (index == points.size - 1) {
                    drawCircle(
                        color = GasTeal.copy(alpha = 0.3f),
                        radius = 6.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = GreenPrimary,
                        radius = 3.dp.toPx(),
                        center = point
                    )
                } else {
                    drawCircle(
                        color = BorderSlate,
                        radius = 2.dp.toPx(),
                        center = point
                    )
                }
            }
        }
    }
}

@Composable
fun GasSensorMeter(value: Float) {
    val coercedValue = value.coerceIn(1.0f, 1000.0f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gas_sensor_meter_card"),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MQ-2 Raw Sensor Meter",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Standard safety index range 1-1000",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = GasTeal,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gauge Canvas
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 130.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerOffset = Offset(width / 2f, height)
                    val radius = (width / 2f) - 15.dp.toPx()

                    // Draw outer arc tracks for zones: Safe (0-700), Danger (700-1000)
                    val strokeWidthValue = 12.dp.toPx()
                    
                    val safeSweep = 180f * (700f / 1000f)
                    drawArc(
                        color = Color(0xFFE8F5E9),
                        startAngle = 180f,
                        sweepAngle = safeSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidthValue, cap = StrokeCap.Butt)
                    )
                    drawArc(
                        color = Color(0xFF4CAF50),
                        startAngle = 180f,
                        sweepAngle = safeSweep,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt)
                    )

                    val dangerStart = 180f + safeSweep
                    val dangerSweep = 180f * (300f / 1000f)
                    drawArc(
                        color = Color(0xFFFFEBEE),
                        startAngle = dangerStart,
                        sweepAngle = dangerSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidthValue, cap = StrokeCap.Butt)
                    )
                    drawArc(
                        color = Color(0xFFF44336),
                        startAngle = dangerStart,
                        sweepAngle = dangerSweep,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt)
                    )

                    // Draw scale labels/ticks
                    val labels = listOf("1", "200", "400", "600", "800", "1000")
                    labels.forEachIndexed { index, label ->
                        val ratio = index.toFloat() / (labels.size - 1)
                        val angleRad = Math.toRadians((180f + ratio * 180f).toDouble())
                        val tickStart = Offset(
                            (centerOffset.x + (radius - 10.dp.toPx()) * Math.cos(angleRad)).toFloat(),
                            (centerOffset.y + (radius - 10.dp.toPx()) * Math.sin(angleRad)).toFloat()
                        )
                        val tickEnd = Offset(
                            (centerOffset.x + radius * Math.cos(angleRad)).toFloat(),
                            (centerOffset.y + radius * Math.sin(angleRad)).toFloat()
                        )
                        drawLine(
                            color = BorderSlate,
                            start = tickStart,
                            end = tickEnd,
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Needle calculation
                    val percentage = (coercedValue - 1f) / 999f
                    val needleAngleRad = Math.toRadians((180f + percentage * 180f).toDouble())
                    val needleLength = radius - 8.dp.toPx()
                    val needleEnd = Offset(
                        (centerOffset.x + needleLength * Math.cos(needleAngleRad)).toFloat(),
                        (centerOffset.y + needleLength * Math.sin(needleAngleRad)).toFloat()
                    )

                    // Draw needle shadow
                    drawLine(
                        color = Color.Black.copy(alpha = 0.15f),
                        start = Offset(centerOffset.x + 2.dp.toPx(), centerOffset.y + 2.dp.toPx()),
                        end = Offset(needleEnd.x + 2.dp.toPx(), needleEnd.y + 2.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw main needle
                    drawLine(
                        color = TextPrimary,
                        start = centerOffset,
                        end = needleEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw inner hub/pin
                    drawCircle(
                        color = TextPrimary,
                        radius = 8.dp.toPx(),
                        center = centerOffset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = centerOffset
                    )
                }
            }

            // Numeric Display and Status Badge
            val statusColor = when {
                coercedValue <= 700f -> Color(0xFF386B41)
                else -> DangerRed
            }
            val statusText = when {
                coercedValue <= 700f -> "SAFE"
                else -> "DANGER LEAK!"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT SENSOR VALUE",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${coercedValue.toInt()}",
                            color = statusColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                        Text(
                            text = " / 1000",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RechartsStyledDashboardChart(
    readingsFromFirestore: List<GasReading>,
    isFirestoreLoading: Boolean,
    onRefresh: () -> Unit
) {
    var selectedMetricTab by remember { mutableStateOf("FLOW") }
    val readings = readingsFromFirestore.take(12).reversed()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firestore_recharts_chart_card"),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg),
        border = BorderStroke(1.dp, BorderSlate),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Firestore Analytics",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Live Cloud Synced Gas Telemetry",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isFirestoreLoading) {
                        CircularProgressIndicator(
                            color = GasTeal,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Cloud Data",
                            tint = GasTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recharts-style tabs inside the card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSlateBg, RoundedCornerShape(8.dp))
                    .padding(3.dp)
            ) {
                val metricTabs = listOf("FLOW" to "Volumetric Flow", "PRESSURE" to "Pressure", "MQ2" to "MQ-2 PPM")
                metricTabs.forEach { (key, label) ->
                    val isSel = selectedMetricTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) Color.White else Color.Transparent)
                            .border(
                                width = if (isSel) 1.dp else 0.dp,
                                color = if (isSel) BorderSlate else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedMetricTab = key }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recharts styled area graph
            if (readings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(DarkSlateBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Firestore telemetry found",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Start ESP32 Simulator to stream readings",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                // Determine active data values & label/color theme
                val maxVal = when (selectedMetricTab) {
                    "FLOW" -> (readings.maxOfOrNull { it.calculatedFlowRate } ?: 5.0).coerceAtLeast(3.0)
                    "PRESSURE" -> (readings.maxOfOrNull { it.pressureDiffPa } ?: 120.0).coerceAtLeast(50.0)
                    else -> (readings.maxOfOrNull { it.mq2ValuePpm } ?: 350.0).coerceAtLeast(200.0)
                }

                val activeColor = when (selectedMetricTab) {
                    "FLOW" -> GreenPrimary
                    "PRESSURE" -> GasTeal
                    else -> AlertAmber
                }

                var touchX by remember { mutableStateOf<Float?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(readings) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    touchX = down.position.x
                                    do {
                                        val event = awaitPointerEvent()
                                        val drag = event.changes.firstOrNull()
                                        if (drag != null && drag.pressed) {
                                            touchX = drag.position.x
                                        } else {
                                            touchX = null
                                        }
                                    } while (event.changes.any { it.pressed })
                                    touchX = null
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height

                        val paddingLeft = 35.dp.toPx()
                        val paddingRight = 10.dp.toPx()
                        val paddingTop = 15.dp.toPx()
                        val paddingBottom = 20.dp.toPx()

                        val graphWidth = width - paddingLeft - paddingRight
                        val graphHeight = height - paddingTop - paddingBottom

                        // 1. Draw grid lines (horizontal and vertical) like Recharts standard grid
                        val horizontalLines = 4
                        for (i in 0..horizontalLines) {
                            val yRatio = i.toFloat() / horizontalLines
                            val y = paddingTop + yRatio * graphHeight
                            drawLine(
                                color = BorderSlate.copy(alpha = 0.2f),
                                start = Offset(paddingLeft, y),
                                end = Offset(width - paddingRight, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // 2. Map readings to Canvas Points
                        val points = readings.mapIndexed { index, reading ->
                            val xRatio = if (readings.size > 1) index.toFloat() / (readings.size - 1) else 0.5f
                            val value = when (selectedMetricTab) {
                                "FLOW" -> reading.calculatedFlowRate
                                "PRESSURE" -> reading.pressureDiffPa
                                else -> reading.mq2ValuePpm
                            }
                            val yRatio = (value / maxVal).coerceIn(0.0, 1.0)
                            val x = paddingLeft + xRatio * graphWidth
                            val y = paddingTop + (1f - yRatio.toFloat()) * graphHeight
                            Offset(x, y)
                        }

                        // 3. Find hovered index if interactive touch is happening
                        val selectedIndex = if (touchX != null && points.isNotEmpty()) {
                            points.minByOrNull { Math.abs(it.x - touchX!!) }?.let { points.indexOf(it) } ?: -1
                        } else {
                            -1
                        }

                        // 4. Draw continuous Bezier area under curve (Recharts styled area)
                        if (points.size > 1) {
                            val strokePath = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    val pPrev = points[i - 1]
                                    val pCurr = points[i]
                                    val ctrlX = (pPrev.x + pCurr.x) / 2f
                                    quadraticTo(ctrlX, pPrev.y, ctrlX, pCurr.y)
                                    lineTo(pCurr.x, pCurr.y)
                                }
                            }

                            val fillPath = Path().apply {
                                addPath(strokePath)
                                lineTo(points.last().x, paddingTop + graphHeight)
                                lineTo(points.first().x, paddingTop + graphHeight)
                                close()
                            }

                            // Draw Area Fill Gradient
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(activeColor.copy(alpha = 0.25f), Color.Transparent),
                                    startY = paddingTop,
                                    endY = paddingTop + graphHeight
                                )
                            )

                            // Draw Line Stroke
                            drawPath(
                                path = strokePath,
                                color = activeColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // 5. If interactive tooltip is active, draw indicator line & highlighted node
                            if (selectedIndex != -1) {
                                val selPoint = points[selectedIndex]
                                // Vertical dotted guide line
                                drawLine(
                                    color = activeColor.copy(alpha = 0.5f),
                                    start = Offset(selPoint.x, paddingTop),
                                    end = Offset(selPoint.x, paddingTop + graphHeight),
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )

                                // Highlight circle node
                                drawCircle(
                                    color = activeColor.copy(alpha = 0.3f),
                                    radius = 8.dp.toPx(),
                                    center = selPoint
                                )
                                drawCircle(
                                    color = activeColor,
                                    radius = 4.dp.toPx(),
                                    center = selPoint
                                )
                            }
                        }

                        // 6. Draw axis labels
                        val yMaxStr = when (selectedMetricTab) {
                            "FLOW" -> "${String.format("%.1f", maxVal)}L"
                            "PRESSURE" -> "${maxVal.toInt()}P"
                            else -> "${maxVal.toInt()}"
                        }
                        
                        drawIntoCanvas { canvas ->
                            val textPaint = android.graphics.Paint().apply {
                                color = TextSecondary.toArgb()
                                textSize = 8.dp.toPx()
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                            }
                            canvas.nativeCanvas.drawText(yMaxStr, 2.dp.toPx(), paddingTop + 8.dp.toPx(), textPaint)
                            canvas.nativeCanvas.drawText("0", 2.dp.toPx(), paddingTop + graphHeight, textPaint)

                            // X Axis labels
                            if (readings.size >= 2) {
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                val firstTime = sdf.format(Date(readings.first().timestamp))
                                val lastTime = sdf.format(Date(readings.last().timestamp))

                                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                                canvas.nativeCanvas.drawText(firstTime, paddingLeft, height - 4.dp.toPx(), textPaint)

                                textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                                canvas.nativeCanvas.drawText(lastTime, width - paddingRight, height - 4.dp.toPx(), textPaint)
                            }
                        }
                    }

                    // 7. Recharts Hover Tooltip Dialog overlay
                    val activeIndex = if (touchX != null && readings.isNotEmpty()) {
                        val pct = ((touchX!! - 35.dp.value) / (320.dp.value)).coerceIn(0f, 1f)
                        val est = (pct * (readings.size - 1)).toInt().coerceIn(0, readings.size - 1)
                        est
                    } else {
                        -1
                    }

                    if (activeIndex != -1 && activeIndex < readings.size) {
                        val activeReading = readings[activeIndex]
                        val activeVal = when (selectedMetricTab) {
                            "FLOW" -> "${String.format("%.2f", activeReading.calculatedFlowRate)} L/min"
                            "PRESSURE" -> "${String.format("%.1f", activeReading.pressureDiffPa)} Pa"
                            else -> "${activeReading.mq2ValuePpm.toInt()} PPM"
                        }
                        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                        val timeStr = sdf.format(Date(activeReading.timestamp))

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TextPrimary.copy(alpha = 0.92f))
                                .border(1.5.dp, activeColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Time: $timeStr",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${if (selectedMetricTab == "FLOW") "Flow Rate" else if (selectedMetricTab == "PRESSURE") "Differential P" else "MQ-2 Detector"}: $activeVal",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (selectedMetricTab) {
                            "FLOW" -> "Gas Flow Output (Recharts-Area)"
                            "PRESSURE" -> "Gas Pressure Gradient (Recharts-Area)"
                            else -> "MQ-2 Leak PPM Index (Recharts-Area)"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "• Touch & drag chart for tooltips",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
            ((this.red * 255.0f + 0.5f).toInt() shl 16) or
            ((this.green * 255.0f + 0.5f).toInt() shl 8) or
            (this.blue * 255.0f + 0.5f).toInt()
}

@Composable
fun FirebaseNotificationBanner(
    notifications: List<FirebaseNotification>,
    currentUserEmail: String,
    currentUserRole: String,
    onResolve: (String) -> Unit
) {
    val activeFirebaseNotifs = notifications.filter { notif ->
        !notif.isResolved && (currentUserRole != "HOMEOWNER" || notif.userEmail == currentUserEmail)
    }

    if (activeFirebaseNotifs.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeFirebaseNotifs.forEach { notif ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = DangerRed,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF5F5)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(DangerRed)
                                )
                                Text(
                                    text = "FIREBASE CLOUD ALERT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            
                            Surface(
                                color = DangerRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "CRITICAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DangerRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = notif.message,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C1C1C)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sensor: ${String.format("%.0f", notif.mq2ValuePpm)} PPM / Limit: ${String.format("%.0f", notif.thresholdPpm)} PPM",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Button(
                                onClick = { onResolve(notif.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DangerRed
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Resolve",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmailAlertItemView(alert: EmailAlertRecord) {
    var isRawExpanded by remember { mutableStateOf(false) }
    val timeStr = SimpleDateFormat("HH:mm:ss yyyy-MM-dd", Locale.getDefault()).format(Date(alert.timestamp))
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F3F9), RoundedCornerShape(12.dp))
            .border(1.dp, BorderSlate, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = alert.recipientType,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }
            
            Text(
                text = timeStr,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "To: ${alert.recipientEmail}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        
        Text(
            text = "Subject: ${alert.subject}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AlertAmber
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = alert.body,
            fontSize = 12.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .clickable { isRawExpanded = !isRawExpanded }
                .fillMaxWidth()
                .background(Color(0xFFE2E4EB), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "SMTP Delivery Handshake Logs",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = if (isRawExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        if (isRawExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                color = Color(0xFF1E1E24),
                shape = RoundedCornerShape(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = alert.smtpLog,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF4AF626),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmailAlertsLogsScreen(viewModel: GasViewModel) {
    val emailAlerts by viewModel.emailAlerts.collectAsStateWithLifecycle()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Automated Security & Safety Dispatch",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Whenever your IoT Gas Sensor registers a reading higher than the safety threshold, background alarms automatically negotiate standard SMTP mail gateways and transmit warnings to the homeowner and affiliated utility company.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
        
        if (emailAlerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Email Alarms Dispatched",
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "SMTP channels are armed and ready.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMTP TRANSACTION LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${emailAlerts.size} messages dispatched",
                        fontSize = 11.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            items(emailAlerts) { alert ->
                EmailAlertItemView(alert = alert)
            }
        }
    }
}
