<h1 align="center">☀️Sunrise alarm (Arduino + Android)</h1>

As [studies demonstrate](https://link.springer.com/chapter/10.1007%2F978-3-662-06085-8_20#page-2),
you may find mornings more pleasant with a wake-up light. The concept of dawn simulation was
first patented in 1890. Also known as a “mechanical sunrise,” the technique involves timing lights
in the bedroom to come on gradually, over a period of 30 minutes to 2 hours, before awakening.

## Table of contents

- 🏗 [Structure](#-structure)
- ⭐️ [Features](#-features)
- 🛠 [Electronics](#-used-electronics)
- ✉️ [Feedback](https://t.me/p_val)

[<img src="https://github.com/ValeryP/sunrise-alarm/blob/dev/android/publish/demo.png" width="100%">]()

### 🏗️ Structure

- `android` - Android app to connect to Arduino via Bluetooth
- `arduino` - Arduino used to control the home lights
- `firebase-cloud-fun` - FCM function to transfer voice assistant commant to Android app via IFTTT

### ⭐️ Features

- Turns table lamp into a smart lamp using Arduino
- Remote control using Bluetooth and Android app
- Switches the lights on before the raise to wake up more smooth
- Voice control over smart home using [IFTTT](https://ifttt.com/) and [FCM](https://firebase.google.com/docs/cloud-messaging)
- The mobile app to set up waking time

### 🛠 Used electronics

- [Arduino Nano](https://store.arduino.cc/arduino-nano)
- [Breadboard mini](https://www.aliexpress.com/item/32523562738.html?spm=a2g0s.9042311.0.0.27424c4dW3Ildy)
- [HC06RF Bluetooth module](https://www.aliexpress.com/item/32498904923.html?spm=a2g0s.9042311.0.0.27424c4dryaNGp)
- [DS3231 RTC Module](https://www.aliexpress.com/item/32498904923.html?spm=a2g0s.9042311.0.0.27424c4dryaNGp)
- [AC 220v to DC 5v converter](https://www.aliexpress.com/item/32677330307.html?spm=a2g0s.9042311.0.0.27424c4dryaNGp)
- [Breadboard jumpers](https://www.aliexpress.com/item/32725034190.html?spm=a2g0s.9042311.0.0.27424c4dW3Ildy)
- [Resistors 100R](https://www.aliexpress.com/item/32847096736.html?spm=a2g0s.9042311.0.0.27424c4dW3Ildy)
- [Diode](https://www.aliexpress.com/item/33015523187.html?spm=a2g0o.productlist.0.0.57bed59cKNnk9r&algo_pvid=2fd5a5fd-0e40-4b99-95a4-31dc076569f1&algo_expid=2fd5a5fd-0e40-4b99-95a4-31dc076569f1-2&btsid=0be3743b15906050894075429e3070&ws_ab_test=searchweb0_0,searchweb201602_,searchweb201603_)
- [Button](https://www.aliexpress.com/item/32818219618.html?spm=a2g0o.productlist.0.0.2f1240a1SOloor&algo_pvid=f86968bd-3d03-4998-a3ae-ecb3cbfb842e&algo_expid=f86968bd-3d03-4998-a3ae-ecb3cbfb842e-17&btsid=0be3764515906051961707233ee832&ws_ab_test=searchweb0_0,searchweb201602_,searchweb201603_)

<img src="https://github.com/ValeryP/sunrise-alarm/blob/dev/android/publish/dfa2dcde-d879-4f08-8fe4-58053fbaeb82.jpg" width="100%">
