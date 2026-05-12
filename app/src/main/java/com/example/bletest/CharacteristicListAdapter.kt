package com.example.bletest

import android.bluetooth.BluetoothGattCharacteristic
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CharacteristicListAdapter(
    private val characteristics: List<BluetoothGattCharacteristic>,
    private val onClick: (BluetoothGattCharacteristic) -> Unit
) : RecyclerView.Adapter<CharacteristicListAdapter.CharacteristicViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacteristicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return CharacteristicViewHolder(view)
    }
    override fun onBindViewHolder(holder: CharacteristicViewHolder, position: Int) {
        val characteristic = characteristics[position]
        holder.textView.text = characteristic.uuid.toString()
        holder.itemView.setOnClickListener { onClick(characteristic) }
    }
    override fun getItemCount() = characteristics.size
    class CharacteristicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }
}
