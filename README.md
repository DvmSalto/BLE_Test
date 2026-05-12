# BLE_Test Android App

This app connects to a BLE device, reads a characteristic, displays its content, and writes 128 random bytes to the same characteristic.

## Setup
1. Open the project in Android Studio.
2. Replace the placeholder BLE device address and characteristic UUID in `MainActivity.kt` with your actual values.
3. Build and run the app on a device with BLE support.

## Features
- Connects to a BLE device
- Reads and displays a characteristic value
- Writes 128 random bytes to the characteristic

## Permissions
The app requests Bluetooth and Location permissions as required for BLE operations.
