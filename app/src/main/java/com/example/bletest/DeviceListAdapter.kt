package com.example.bletest

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceListAdapter(
    private val devices: List<BluetoothDevice>,
    private val onClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return DeviceViewHolder(view)
    }
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.textView.text = device.name ?: device.address
        holder.itemView.setOnClickListener { onClick(device) }
    }
    override fun getItemCount() = devices.size
    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }
}
