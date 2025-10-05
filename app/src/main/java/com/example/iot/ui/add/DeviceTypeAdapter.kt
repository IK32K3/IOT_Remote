package com.example.iot.ui.add


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.iot.databinding.ItemDeviceTypeBinding


class DeviceTypeAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<DeviceTypeAdapter.VH>() {


    class VH(val b: ItemDeviceTypeBinding) : RecyclerView.ViewHolder(b.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemDeviceTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }


    override fun getItemCount() = items.size


    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.txtType.text = item
        holder.b.root.setOnClickListener { onClick(item) }
    }
}