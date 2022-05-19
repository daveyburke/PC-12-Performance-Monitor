package com.pc12

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.pc12.ui.theme.Cyan
import com.pc12.ui.theme.PC12PerformanceMonitorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val flightDataViewModel by viewModels<FlightDataViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            window.statusBarColor = Cyan.toArgb()
            PC12PerformanceMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    if (!flightDataViewModel.getUserAgreedTerms()) {
                        WarningDialog({  // onProceed
                                flightDataViewModel.setUserAgreedTerms()
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
        flightDataViewModel.startNetworkRequests()
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

    PerformanceDataDisplay(flightDataViewModel.uiState.avionicsData.altitude,
                           flightDataViewModel.uiState.avionicsData.outsideTemp,
                           flightDataViewModel.uiState.perfData.torque,
                           flightDataViewModel.uiState.perfData.fuelFlow,
                           flightDataViewModel.uiState.perfData.airspeed,
                           flightDataViewModel.uiState.avionicsInterface,
                           flightDataViewModel.uiState.age)
}

@Composable
fun PerformanceDataDisplay(altitude: Int, outsideTemp: Int, torque: Float, fuelFlow: Int,
                           airspeed: Int, avionicsInterface: String, age: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column {
            val DATA_MAXAGE = 60  // 1 min
            val deltaIsaTemp = outsideTemp + (altitude + 500) / 1000 * 2 - 15

            val altitudeStr = if (avionicsInterface == "") "---" else altitude
            val outsideTempStr = if (avionicsInterface == "") "---" else outsideTemp
            val deltaIsaTempStr = when {
                avionicsInterface == "" -> ""
                deltaIsaTemp > 0 -> "(ISA +$deltaIsaTemp)"
                deltaIsaTemp < 0 -> "(ISA $deltaIsaTemp)"
                else -> ""
            }
            val torqueStr = if (torque.isNaN() || age > DATA_MAXAGE) "---" else torque
            val fuelFlowStr = if (torque.isNaN() || age > DATA_MAXAGE || fuelFlow == 0) "---" else fuelFlow
            val airspeedStr = if (torque.isNaN() || age > DATA_MAXAGE || airspeed == 0) "---" else airspeed

            val ageStr = if (age > 60) (age / 60).toString() + "m" else "$age" + "s"
            var avionicsLabel = "Avionics Data"
            if (avionicsInterface != "") {
                avionicsLabel += " - $avionicsInterface"
                if (age > 0) avionicsLabel += " ($ageStr old)"
            } else {
                avionicsLabel += " - Searching..."
            }

            val statusColor = if (age > DATA_MAXAGE || avionicsInterface == "") {
                Color(200, 0, 0)
            } else {
                Color(30, 140, 100)
            }
            val textColor = (if (isSystemInDarkTheme()) Color.White else Color.Black)

            OutlinedTextField(
                value = "ALT: $altitudeStr ft\nSAT: $outsideTempStr \u2103 $deltaIsaTempStr",
                onValueChange = { },
                label = { Text(avionicsLabel) },
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
                value = "TRQ: $torqueStr psi\nFF: $fuelFlowStr lb/h\nTAS: $airspeedStr kts",
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
fun AvionicsInterfaceSettings(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context)  }
    val avionicsInterfaceFlow = settingsStore.avionicsInterfaceFlow.collectAsState(
        initial = SettingsStore.AUTO_DETECT_INTERFACE)
    val optionItems = listOf(
        SettingsStore.avionicsInterfaceToString(SettingsStore.ASPEN_INTERFACE),
        SettingsStore.avionicsInterfaceToString(SettingsStore.ECONNECT_INTERFACE),
        SettingsStore.avionicsInterfaceToString(SettingsStore.GOGO_INTERFACE),
        SettingsStore.avionicsInterfaceToString(SettingsStore.AUTO_DETECT_INTERFACE))

    SelectOptionsDialog("Avionics Interface", optionItems, avionicsInterfaceFlow.value,
        onSelected =
        {
            scope.launch {
                settingsStore.saveAvionicsInterface(it)
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

@Preview(showBackground = true,  heightDp = 600, widthDp = 400)
@Composable
fun DefaultPreview() {
    PC12PerformanceMonitorTheme {
        OverflowMenu()
        PerformanceDataDisplay(24000, -32, 30.1f, 300, 275,"Gogo", 5)
    }
}
