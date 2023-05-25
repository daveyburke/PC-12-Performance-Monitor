package com.pc12

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.pc12.ui.theme.Cyan
import com.pc12.ui.theme.PC12PerformanceMonitorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val flightDataViewModel by viewModels<FlightDataViewModel>()
    private var userAgreedTerms = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            window.statusBarColor = Cyan.toArgb()
            PC12PerformanceMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    if (!userAgreedTerms) {
                        WarningDialog({  // onProceed
                                userAgreedTerms = true
                                flightDataViewModel.startNetworkRequests()
                            },{  // onCancel
                                finish()
                            }
                        )
                    }
                    OverflowMenu()
                    PerformanceMonitorScreen(flightDataViewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        flightDataViewModel.stopNetworkRequests()
    }

    override fun onResume() {
        super.onResume()
        if (userAgreedTerms) {
            flightDataViewModel.startNetworkRequests()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun PerformanceMonitorScreen(flightDataViewModel: FlightDataViewModel) {
    val systemUiController = rememberSystemUiController()
    val backgroundColor = MaterialTheme.colors.background
    SideEffect {
        systemUiController.setNavigationBarColor(color = backgroundColor)
    }

    PerformanceDataDisplay(flightDataViewModel.uiState)
}

@Composable
fun PerformanceDataDisplay(uiState: UIState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column {
            val statusColor = if (uiState.isDataOld) {
                Color(200, 0, 0)
            } else {
                Color(30, 140, 100)
            }
            val textColor = (if (isSystemInDarkTheme()) Color.White else Color.Black)

            OutlinedTextField(
                value = "ALT: ${uiState.altitude} ft\nSAT: ${uiState.outsideTemp} \u2103 ${uiState.deltaIsaTemp}",
                onValueChange = { },
                label = { Text("Avionics Data ${uiState.avionicsLabel}") },
                enabled = false,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    disabledTextColor = textColor,
                    disabledBorderColor = statusColor,
                    disabledLabelColor = statusColor,
                ),
                textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp),
            )

            Spacer(modifier = Modifier.height(50.dp))

            OutlinedTextField(
                value = "TRQ: ${uiState.torqueStr} psi\nFF: ${uiState.fuelFlowStr} lb/h\nTAS: ${uiState.airspeed} kts",
                onValueChange = { },
                label = {
                    Text("Maximum Cruise Power")
                },
                enabled = false,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    disabledTextColor = textColor,
                    disabledBorderColor = statusColor,
                    disabledLabelColor = statusColor,
                ),
                textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp),
            )
        }
    }
}


@Composable
fun OverflowMenu() {
    val expanded = remember { mutableStateOf(false) }
    val scaffoldState = rememberScaffoldState()
    val showAircraftTypeDialog = remember { mutableStateOf(false) }
    val showAvionicsInterfaceDialog = remember { mutableStateOf(false) }
    val showAircraftWeightDialog = remember { mutableStateOf(false) }

    if (showAircraftTypeDialog.value) AircraftTypeSettings {
        showAircraftTypeDialog.value = false
    }
    if (showAvionicsInterfaceDialog.value) AvionicsInterfaceSettings {
        showAvionicsInterfaceDialog.value = false
    }
    if (showAircraftWeightDialog.value) AircraftWeightSettings {
        showAircraftWeightDialog.value = false
    }

    Scaffold(
        scaffoldState = scaffoldState,
        content = { },
        topBar = {
            TopAppBar(
                title = { Text("PC-12 Performance Monitor") },
                backgroundColor = Cyan,
                contentColor = Color.White,
                actions = {
                    IconButton(
                        onClick = {
                            expanded.value = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Aircraft Type",
                            tint = Color.White,
                        )
                    }
                    DropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false }) {
                        DropdownMenuItem(onClick = {
                            showAircraftTypeDialog.value = true
                            expanded.value = false
                        }) {
                            Text(text = "Aircraft Type")
                        }
                        DropdownMenuItem(onClick = {
                            showAvionicsInterfaceDialog.value = true
                            expanded.value = false
                        }) {
                            Text(text = "Avionics Interface")
                        }
                        DropdownMenuItem(onClick = {
                            showAircraftWeightDialog.value = true
                            expanded.value = false
                        }) {
                            Text(text = "Aircraft Weight")
                        }
                    }
                }
            )
        }
    )
}

@Composable
fun AvionicsInterfaceSettings(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val avionicsInterfaceFlow = settingsStore.avionicsInterfaceFlow.collectAsState(
        initial = SettingsStore.ASPEN_INTERFACE)

    val networkSsidFlow = settingsStore.networkSsidFlow.collectAsState(initial = "")
    val networkPasswordFlow = settingsStore.networkPasswordFlow.collectAsState(initial = "")

    var ssid by remember(networkSsidFlow.value) { mutableStateOf(networkSsidFlow.value) }
    var password by remember(networkPasswordFlow.value) { mutableStateOf(networkPasswordFlow.value) }

    val optionItems = listOf(
        SettingsStore.avionicsInterfaceToString(SettingsStore.ASPEN_INTERFACE),
        SettingsStore.avionicsInterfaceToString(SettingsStore.ECONNECT_INTERFACE),
        SettingsStore.avionicsInterfaceToString(SettingsStore.GOGO_INTERFACE))

    // A custom AlertDialog is now used to accommodate the new text fields
    AlertDialog(
        title = {
            Text(text = "Avionics Interface")
        },
        text = {
            Column {
                // Radio buttons for selecting the interface type
                optionItems.forEachIndexed { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { scope.launch { settingsStore.saveAvionicsInterface(index) } })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == avionicsInterfaceFlow.value,
                            onClick = { scope.launch { settingsStore.saveAvionicsInterface(index) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = item)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Auto connect app (optional)")
                Spacer(modifier = Modifier.height(8.dp))

                // Text field for Wi-Fi Name (SSID)
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Wi-Fi Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Text field for Wi-Fi Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Wi-Fi Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        settingsStore.saveNetworkSsid(ssid)
                        settingsStore.saveNetworkPassword(password)
                    }
                    onClose()
                }) {
                Text("OK")
            }
        },
        onDismissRequest = { onClose() }
    )
}

@Composable
fun AircraftTypeSettings(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context)  }
    val aircraftTypeFlow = settingsStore.aircraftTypeFlow.collectAsState(
        initial = SettingsStore.PC_12_47E_MSN_1576_1942_5_Blade)
    val optionItems = listOf(
        SettingsStore.aircraftTypeToString(SettingsStore.PC_12_47E_MSN_1001_1942_4_Blade),
        SettingsStore.aircraftTypeToString(SettingsStore.PC_12_47E_MSN_1576_1942_5_Blade),
        SettingsStore.aircraftTypeToString(SettingsStore.PC_12_47E_MSN_2001_5_Blade))

    SelectOptionsDialog("Aircraft Type", optionItems, aircraftTypeFlow.value,
        onSelected =
        {
            scope.launch {
                settingsStore.saveAircraftType(it)
            }
        },
        onClose = { onClose() }
    )
}

@Composable
fun AircraftWeightSettings(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context)  }
    val aircraftWeightFlow = settingsStore.aircraftWeightFlow.collectAsState(
        initial = SettingsStore.WEIGHT_8000)
    val optionItems = listOf(
        SettingsStore.aircraftWeightToString(SettingsStore.WEIGHT_7000),
        SettingsStore.aircraftWeightToString(SettingsStore.WEIGHT_8000),
        SettingsStore.aircraftWeightToString(SettingsStore.WEIGHT_9000),
        SettingsStore.aircraftWeightToString(SettingsStore.WEIGHT_10000),
        SettingsStore.aircraftWeightToString(SettingsStore.WEIGHT_10400))

    SelectOptionsDialog("Aircraft Weight", optionItems, aircraftWeightFlow.value,
        onSelected =
        {
            scope.launch {
                settingsStore.saveAircraftWeight(it)
            }
        },
        onClose = { onClose() }
    )
}

@Composable
fun SelectOptionsDialog(title: String, optionItems: List<String>, selectedIndex: Int, onSelected:(index: Int) -> Unit, onClose:() -> Unit) {
    AlertDialog(
        title = {
            Text(text = title)
        },
        text = {
            Spacer(modifier = Modifier.height(20.dp))
            Column {
                optionItems.forEachIndexed { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelected(index) }
                            )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item,
                            modifier = Modifier.clickable(onClick = { onSelected(index) })
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onClose()
                }) {
                Text("OK")
            }
        },
        onDismissRequest = { },
    )
}

@Composable
fun WarningDialog(onProceed: () -> Unit, onCancel: () -> Unit) {
    val firstRun = remember { mutableStateOf(true)  }

    if (firstRun.value) {
        val isTermsChecked = remember { mutableStateOf(false) }
        AlertDialog(
            title = {
                Text(text = "Warning", fontWeight = FontWeight.Bold)
            },
            backgroundColor = Color(200,0,0),
            contentColor = Color.White,
            text = {
                Column {
                    Text(
                        "THIS APP IS FOR DEMO/MONITORING PURPOSES ONLY. It must not be used to set engine " +
                             "torque. Always refer to the manufacturer's QRH or AFM for " +
                             "authoritative engine settings.\n\n" +
                             "NO WARRANTY: This app is provided as is. No guarantee is made " +
                             "that it is free from mistakes or errors. Use at your own risk.\n\n" +
                             "LIMITATION OF LIABILITY: In no event shall the author(s) of this app " +
                             "be held responsible for any engine or aircraft damage, " +
                             "consequential / indirect / special damages, or loss of profit or revenue " +
                             "resulting from the use of this app.\n",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "I agree to these terms & conditions ",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,)
                        Checkbox(
                            checked = isTermsChecked.value,
                            onCheckedChange = { isTermsChecked.value = it },
                            enabled = true,
                            colors = CheckboxDefaults.colors(uncheckedColor = Color.White,
                                                             checkedColor = Color.White,
                                                             checkmarkColor = Color.Black)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = isTermsChecked.value,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black),
                    onClick = {
                        onProceed()
                        firstRun.value = false
                    }) {
                    Text("PROCEED")
                }
            },
            dismissButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black),
                    onClick = {
                        onCancel()
                    }) {
                    Text("CANCEL")
                }
            },
            onDismissRequest = { },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light theme", heightDp = 600, widthDp = 400)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark theme", heightDp = 600, widthDp = 400)
@Composable
fun DefaultPreview() {
    PC12PerformanceMonitorTheme {
        OverflowMenu()
        PerformanceDataDisplay(UIState("24000", "-32", "30.1f", "300", "275", "5", "Gogo", false))
    }
}
