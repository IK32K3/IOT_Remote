package com.example.iot.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.iot.databinding.ItemSimpleTextBinding

class SimpleStringAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SimpleStringAdapter.VH>() {
    private var items: List<String> = emptyList()
    class VH(val b: ItemSimpleTextBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        return VH(ItemSimpleTextBinding.inflate(LayoutInflater.from(p.context), p, false))
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = items[pos]
        h.b.text1.text = s
        h.b.root.setOnClickListener { onClick(s) }
    }
    fun submit(list: List<String>) { items = list; notifyDataSetChanged() }
}
