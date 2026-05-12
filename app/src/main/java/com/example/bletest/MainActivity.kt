package com.example.bletest

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var buttonWrite: Button
    private lateinit var buttonRead: Button
    private lateinit var buttonScan: Button
    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var characteristicRecyclerView: RecyclerView

    private val randomBytes = ByteArray(128) { (0..255).random().toByte() }
    private val foundDevices = mutableListOf<BluetoothDevice>()
    private var selectedDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val foundCharacteristics = mutableListOf<BluetoothGattCharacteristic>()
    private var selectedCharacteristic: BluetoothGattCharacteristic? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)
        buttonWrite = findViewById(R.id.buttonWrite)
        buttonRead = findViewById(R.id.buttonRead)
        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)
        characteristicRecyclerView = findViewById(R.id.characteristicRecyclerView)
        buttonScan = findViewById(R.id.buttonScan)

        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        characteristicRecyclerView.layoutManager = LinearLayoutManager(this)

        buttonWrite.setOnClickListener { writeCharacteristic() }
        buttonRead.setOnClickListener { readCharacteristic() }
        buttonScan.setOnClickListener { startScan() }

        requestAllPermissions()
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val toRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 100)
        } else {
            checkLocationAndScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkLocationAndScan()
            } else {
                Toast.makeText(this, "Permissions required for BLE scan", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkLocationAndScan() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isLocationEnabled) {
            Toast.makeText(this, "Location must be enabled for BLE scan", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            startScan()
        }
    }

    private var scanCallback: android.bluetooth.le.ScanCallback? = null

    private fun startScan() {
        foundDevices.clear()
        val adapter = DeviceListAdapter(foundDevices) { device ->
            stopScan()
            val intent = android.content.Intent(this, DeviceDetailActivity::class.java)
            intent.putExtra("device_address", device.address)
            intent.putExtra("device_name", device.name ?: "Unknown")
            startActivity(intent)
        }
        deviceRecyclerView.adapter = adapter

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth scan permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
        scanCallback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                result?.device?.let { device ->
                    if (!foundDevices.any { it.address == device.address }) {
                        foundDevices.add(device)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
        textView.text = "Scanning for BLE devices... Tap to connect."
    }

    private fun stopScan() {
        if (scanCallback != null) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            scanCallback = null
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            return
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        textView.text = "Connecting to ${device.name ?: device.address}..."
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
                runOnUiThread { textView.text = "Connected. Discovering services..." }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread { textView.text = "Disconnected." }
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            foundCharacteristics.clear()
            gatt.services.forEach { service ->
                foundCharacteristics.addAll(service.characteristics)
            }
            runOnUiThread {
                val charAdapter = CharacteristicListAdapter(foundCharacteristics) { characteristic ->
                    selectedCharacteristic = characteristic
                    textView.text = "Selected characteristic: ${characteristic.uuid}"
                }
                characteristicRecyclerView.adapter = charAdapter
                textView.text = "Select a characteristic to read/write."
            }
        }
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value
                runOnUiThread {
                    textView.text = "Read: " + value.joinToString(", ") { String.format("%02X", it) }
                }
            }
        }
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            runOnUiThread {
                textView.text = if (status == BluetoothGatt.GATT_SUCCESS) "Write successful" else "Write failed"
            }
        }
    }

    private fun readCharacteristic() {
        val gatt = bluetoothGatt ?: return
        val characteristic = selectedCharacteristic ?: return
        gatt.readCharacteristic(characteristic)
    }

    private fun writeCharacteristic() {
        val gatt = bluetoothGatt ?: return
        val characteristic = selectedCharacteristic ?: return
        characteristic.value = randomBytes
        gatt.writeCharacteristic(characteristic)
    }
}
