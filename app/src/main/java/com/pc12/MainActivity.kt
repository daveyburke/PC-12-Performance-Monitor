package com.pc12

import android.os.Bundle
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
                        WarningDialog({
                                flightDataViewModel.setUserAgreedTerms()
                                flightDataViewModel.startNetworkRequests()
                            },{
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
    }
}

@Composable
fun PerformanceMonitorScreen(flightDataViewModel: FlightDataViewModel) {
    PerformanceDataDisplay(flightDataViewModel.uiState.avionicsData.altitude,
                           flightDataViewModel.uiState.avionicsData.outsideTemp,
                           flightDataViewModel.uiState.perfData.torque,
                           flightDataViewModel.uiState.age)
}

@Composable
fun PerformanceDataDisplay(altitude: Int, outsideTemp: Int, torque: Float, age: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column {
            val MAXAGE = 60  // 1 min
            val textColor = (if (isSystemInDarkTheme()) Color.White else Color.Black)
            val statusColor = if (age > MAXAGE || torque.isNaN()) Color(200, 0, 0) else Color(30, 140, 100)
            val altitudeStr = (if (torque.isNaN()) "---" else altitude)
            val outsideTempStr = (if (torque.isNaN()) "---" else outsideTemp)
            val torqueStr = (if (torque.isNaN() || age > MAXAGE) "---" else torque)
            val ageStr = if (age > 60) (age / 60).toString() + " min" else "$age sec"

            OutlinedTextField(
                value = "Altitude: $altitudeStr ft\nSAT: $outsideTempStr \u2103",
                onValueChange = { },
                label = { Text("Avionics Data" + if (!torque.isNaN() && age > 0) " ($ageStr)" else "") },
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
                value = "TRQ: $torqueStr psi",
                onValueChange = { },
                label = {
                    Text("Max Cruise")
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
    val showWifiTypeDialog = remember { mutableStateOf(false) }

    if (showAircraftTypeDialog.value) AircraftTypeSettings {
        showAircraftTypeDialog.value = false
    }

    if (showWifiTypeDialog.value) WifiTypeSettings {
        showWifiTypeDialog.value = false
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
                            showWifiTypeDialog.value = true
                            expanded.value = false
                        }) {
                            Text(text = "Wi-Fi Type")
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
        SettingsStore.aircraftTypeToString(SettingsStore.PC_12_47E_MSN_1451_1942_4_Blade),
        SettingsStore.aircraftTypeToString(SettingsStore.PC_12_47E_MSN_1576_1942_5_Blade),
        SettingsStore.aircraftTypeToString(SettingsStore.PC_12_47E_MSN_2001_5_Blade)
    )

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
fun WifiTypeSettings(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context)  }
    val wifiTypeFlow = settingsStore.wifiTypeFlow.collectAsState(
        initial = SettingsStore.GOGO_WIFI)
    val optionItems = listOf(
        SettingsStore.wifiTypeToString(SettingsStore.GOGO_WIFI),
        SettingsStore.wifiTypeToString(SettingsStore.ECONNECT_WIFI)
    )

    SelectOptionsDialog("Wi-Fi Type", optionItems, wifiTypeFlow.value,
        onSelected =
        {
            scope.launch {
                settingsStore.saveWifiType(it)
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
                        "THIS APP IS FOR DEMO PURPOSES ONLY. It must not be used to set engine " +
                                "torque. Always refer to the manufacturer's QRH or AFM for " +
                                "authoritative engine settings.\n\n" +
                                "LIMITATION OF LIABILITY: In no event shall the author(s) of this app " +
                                "be held responsible for any engine or aircraft damage, " +
                                "consequential / indirect / special damages, or loss of profit or revenue " +
                                "resulting from the use of this app.\n",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Row {
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
        PerformanceDataDisplay(24000, -32, 30.1f, 5)
    }
}
