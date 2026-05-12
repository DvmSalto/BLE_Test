package com.example.bletest

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeviceDetailActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var buttonWrite: Button
    private lateinit var buttonRead: Button
    private lateinit var characteristicRecyclerView: RecyclerView
    private var bluetoothGatt: BluetoothGatt? = null
    private val foundCharacteristics = mutableListOf<BluetoothGattCharacteristic>()
    private var selectedCharacteristic: BluetoothGattCharacteristic? = null
    private val randomBytes = ByteArray(128) { (0..255).random().toByte() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)
        textView = findViewById(R.id.textView)
        buttonWrite = findViewById(R.id.buttonWrite)
        buttonRead = findViewById(R.id.buttonRead)
        characteristicRecyclerView = findViewById(R.id.characteristicRecyclerView)
        characteristicRecyclerView.layoutManager = LinearLayoutManager(this)

        val deviceAddress = intent.getStringExtra("device_address")
        val deviceName = intent.getStringExtra("device_name")
        textView.text = "Connecting to $deviceName\n$deviceAddress"
        val device = (applicationContext.getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)

        buttonWrite.setOnClickListener { writeCharacteristic() }
        buttonRead.setOnClickListener { readCharacteristic() }
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
