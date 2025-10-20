package com.example.iot.ui.add


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.iot.R
import com.example.iot.databinding.FragmentDeviceTypeBinding
import com.google.android.material.appbar.MaterialToolbar

class DeviceTypeFragment : Fragment() {
    private var _b: FragmentDeviceTypeBinding? = null
    private val b get() = _b!!


    private val types = listOf("TV", "STB/Sat", "Máy lạnh", "DVD", "Quạt điện", "Máy chiếu",
        "Máy nước nóng", "Máy lọc không khí", "Rèm cửa")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentDeviceTypeBinding.inflate(inflater, container, false)
        return b.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar: MaterialToolbar = b.topBar.root
        toolbar.title = "Thêm điều khiển từ xa"
        toolbar.subtitle = "Chọn loại thiết bị"
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        b.recyclerTypes.layoutManager = GridLayoutManager(requireContext(), 3)
        b.recyclerTypes.adapter = DeviceTypeAdapter(types) { selected ->
            findNavController().navigate(R.id.action_deviceType_to_brandList, Bundle().apply {
                putString("type", selected)
            })
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}