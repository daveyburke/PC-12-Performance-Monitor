# PC-12-Performance-Monitor

Android app for pilots that monitors altitude and outside air temp from avionics to automatically suggest max cruise torque for Pilatus PC-12NG / NGX aircraft (S/N 1451+).
Requires Gogo Wi-Fi onboard (tested with L3 Avance). Future implementation may add support for eConnect Wi-Fi. Data consistent with eQRH tables.
<br/>
<br/>

<p align="center">
<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_1.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_2.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_3.png" alt="" width="175"/>
<br/>
<em>Figure 1: Screenshots from app</em>
</p>

<br/>
Avionics data outline indicates age of data in seconds/minutes. The outline boxes will turn red if the age of received avionics data is older than 5 mins to warn of stale data. Data refreshes every 5 seconds when app is open.
<br/>
<br/>
<b>Warning</b>: THIS APP IS FOR DEMO PURPOSES ONLY. It must not be used to set engine torque. Always refer to the manufacturer's QRH or AFM
for authoritative engine settings
<br/>
<br/>

For Android app developers: this simple app demonstrates "Modern Android Development" using Kotlin and Jetpack Compose with ViewModel, coroutines, and DataStore.


