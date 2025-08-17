# PC-12 Performance Monitor

Android app for pilots that monitors altitude and outside air temp from avionics to automatically calculate max cruise engine torque, fuel flow, and true airspeed for Pilatus PC-12NG / NGX aircraft. Works with the following Wi-Fi access points / gateways:
* Aspen CG-100 ("Connected Flight Deck")
* Emteq eConnect
* Gogo

Supports 4-blade and 5-blade aircraft S/N 1001+ (for NGX, assumes 1700 RPM mode). Data consistent with eQRH tables and POH tables using interpolation. Reported torque and fuel flow assumes a 8000lb weight. Airspeed is a function of weight.
<br/>
<br/>
<p align="center">
<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_1.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_2.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_2b.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<br/>
<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_3.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_4.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="https://raw.githubusercontent.com/daveyburke/PC-12-Performance-Monitor/main/Screenshot_5.png" alt="" width="175"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<br/>
<br/>
<em>Figure 1: Screenshots from app</em>
</p>

<br/>
The app has a simple/clean UI and supports dark mode for night flying. 
<br/>
<br/>
Data refresh is attempted every 5 seconds when the app is open. If the data is stale (older than 1 min), the text outline boxes will turn red and calculated values will be displayed as ---. The age of the received avionics data is shown in seconds/minutes.
<br/>
<br/>The aircraft type, avionics interface, and weight can be configured from the overflow menu and is persisted across app restarts. You can optionally specify the Wi-Fi access point for the app to automatically connect to. This is useful if the phone is connected to another access point for Internet (e.g. Starlink) and you need the app to temporarily connect to an avionics Wi-Fi (say the Aspen CG-100, e.g. "PC12N123AB") when open.
<br/>
<br/>
<b>Warning</b>: THIS APP IS FOR DEMO/MONITORING PURPOSES ONLY. It must not be used to set engine torque. Always refer to the manufacturer's QRH or AFM
for authoritative engine settings.
<br/>
<br/>
Please file aircraft specific issues <a href="https://github.com/daveyburke/PC-12-Performance-Monitor/issues">here</a>.
<br/>
<br/>
Privacy policy: This app has minimum permissions and does not collect any user or aircraft data nor include ads.

---

For Android developers: this app demonstrates "Modern Android Development" using Kotlin, Jetpack Compose, with ViewModel, DataStore, and coroutines.


