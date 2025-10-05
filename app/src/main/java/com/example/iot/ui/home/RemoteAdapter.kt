package com.example.iot.ui.home

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.iot.databinding.ItemRemoteBinding

class RemoteAdapter(
    private var items: List<RemoteCardUi> = emptyList(),
    private val onPower: (RemoteCardUi) -> Unit,
    private val onClick: (RemoteCardUi) -> Unit
) : RecyclerView.Adapter<RemoteAdapter.VH>() {

    class VH(val b: ItemRemoteBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemRemoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.txtRemoteName.text = item.name

        holder.b.txtStatus.text = if (item.online) "Online" else "Offline"
        val bg = holder.b.txtStatus.background.mutate() as GradientDrawable
        bg.setColor(if (item.online) 0xFF2E7D32.toInt() else 0xFF9E9E9E.toInt()) // xanh lá/ghi

        holder.b.btnPower.isEnabled = item.online
        holder.b.btnPower.alpha = if (item.online) 1f else 0.4f
        holder.b.root.setOnClickListener { onClick(item) }
        holder.b.btnPower.setOnClickListener { onPower(item) }
    }

    fun submit(newItems: List<RemoteCardUi>) {
        items = newItems
        notifyDataSetChanged()
    }
}
