package com.example.iot.ui.add


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.databinding.FragmentCodesetTestBinding
import com.example.iot.domain.usecase.PublishUseCase
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.iot.R

@AndroidEntryPoint
class CodeSetTestFragment : Fragment() {
    private var _b: FragmentCodesetTestBinding? = null
    private val b get() = _b!!

    @Inject lateinit var publish: PublishUseCase

    private var index = 1
    private val total = 31
    private lateinit var nodeId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentCodesetTestBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nodeId = arguments?.getString("nodeId") ?: ""

        val toolbar: MaterialToolbar = b.topBar.root
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        updateTitle(toolbar)


        b.btnMatched.setOnClickListener {
            val type = arguments?.getString("type").orEmpty()
            val brand = arguments?.getString("brand").orEmpty()
            findNavController().navigate(R.id.action_codeSetTest_to_saveRemote, Bundle().apply {
                putString("type", type)
                putString("brand", brand)
                putInt("index", index)
                putString("nodeId", "esp-bedroom") // TODO: chọn node động ở bước tới
            })
        }


        b.btnNext.setOnClickListener {
            if (index < total) {
                index++
                updateTitle(toolbar)
            }
        }

        b.btnPower.setOnClickListener {
            val brand = arguments?.getString("brand") ?: ""
            val type = arguments?.getString("type") ?: ""
            val payload = """{"key":"POWER","brand":"$brand","type":"$type","index":$index}"""
            publish(MqttTopics.testIrTopic(nodeId), payload)
        }
    }

    private fun updateTitle(toolbar: MaterialToolbar) {
        val type = arguments?.getString("type") ?: "Thiết bị"
        val brand = arguments?.getString("brand") ?: "Thương hiệu"
        toolbar.title = "Thêm điều khiển từ xa"
        toolbar.subtitle = "$brand · $type"
        b.txtTitle.text = "Khớp $brand $type ($index / $total)"
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}