package com.example.iot.ui.add


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.iot.R
import com.example.iot.databinding.FragmentBrandListBinding
import com.google.android.material.appbar.MaterialToolbar

class BrandListFragment : Fragment() {
    private var _b: FragmentBrandListBinding? = null
    private val b get() = _b!!


    private val brands = listOf("LG", "Mitsubishi", "Panasonic", "Samsung", "Sharp", "Sony", "TCL", "Daikin", "Aqua")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentBrandListBinding.inflate(inflater, container, false)
        return b.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type = arguments?.getString("type") ?: "Thiết bị"

        val toolbar: MaterialToolbar = b.topBar.root
        toolbar.title = "Thêm điều khiển từ xa"
        toolbar.subtitle = "$type · Chọn thương hiệu"
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        b.recyclerBrands.adapter = BrandListAdapter(brands) { brand ->
            findNavController().navigate(
                R.id.action_brandList_to_codeSetTest,
                Bundle().apply {
                    putString("type", type)
                    putString("brand", brand)
                }
            )
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}