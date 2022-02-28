# PC-12 Performance Monitor

<b>Warning</b>: THIS APP IS FOR DEMO PURPOSES ONLY. It must not be used to set engine torque. Always refer to the manufacturer's QRH or AFM
for authoritative engine settings.

Android app that monitors altitude and outside air temp from avionics to automatically indicate max cruise engine torque for Pilatus PC-12NG / NGX aircraft. Data consistent with eQRH tables (with interpolation). Supports 4-blade and 5-blade aircraft (S/N 1451+; for NGX, assumes 1700 RPM mode). Requires Gogo Wi-Fi onboard (tested with L3 Avance). Future implementation may add support for eConnect Wi-Fi. 
<br/>
<br/>

<p align="center">
<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_1.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_2.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_3.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_4.png" alt="" width="175"/>
<br/>
<br/>
<em>Figure 1: Screenshots from app</em>
</p>

<br/>
The app has a simple/clean UI. Data refresh is attempted every 5 seconds when the app is open. The age of the avionics data is shown in seconds/minutes. The text outline boxes will turn red if the age of received avionics data is older than 5 mins to warn of stale data. The aircraft type can be selected from the overflow menu and is persisted across app restarts. Supports dark mode for night flying.
<br/>
<br/>

For Android developers: this simple app demonstrates "Modern Android Development" using the Kotlin programming language, Jetpack Compose for UI, with ViewModel, DataStore, and coroutines.


