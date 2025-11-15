package com.example.iot.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.iot.R
import com.example.iot.core.Defaults
import com.example.iot.domain.model.DeviceType
import com.example.iot.feature.control.test.CodeSetTestCatalog
import com.example.iot.feature.control.test.CodeSetTestModel
import com.example.iot.feature.control.test.CodeSetTestStrategies
import com.example.iot.feature.control.test.CodeSetTestStrategy
import com.example.iot.databinding.FragmentCodesetTestBinding
import com.example.iot.domain.usecase.PublishUseCase
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CodeSetTestFragment : Fragment() {
    private var _b: FragmentCodesetTestBinding? = null
    private val b get() = _b!!

    @Inject lateinit var publish: PublishUseCase

    private var position = 0
    private var total = 1
    private var models: List<CodeSetTestModel> = emptyList()
    private lateinit var nodeId: String
    private lateinit var deviceTypeLabel: String
    private lateinit var deviceType: DeviceType
    private lateinit var testStrategy: CodeSetTestStrategy
    private lateinit var displayTypeLabel: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentCodesetTestBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nodeId = arguments?.getString("nodeId")?.takeIf { it.isNotBlank() } ?: Defaults.NODE_ID
        val brand = arguments?.getString("brand").orEmpty()
        deviceTypeLabel = arguments?.getString("type").orEmpty()
        deviceType = DeviceType.from(deviceTypeLabel)
        testStrategy = CodeSetTestStrategies.strategyFor(deviceType)
        displayTypeLabel = deviceTypeLabel.ifBlank { deviceType.name }
        models = CodeSetTestCatalog.modelsFor(deviceType, brand, displayTypeLabel)
        total = models.size

        val toolbar: MaterialToolbar = b.topBar.root
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        updateUi(toolbar, brand)

        b.btnMatched.setOnClickListener {
            val model = currentModel()
            val payloadType = model.type
            val deviceType = deviceTypeLabel
            findNavController().navigate(R.id.action_codeSetTest_to_saveRemote, Bundle().apply {
                putString("type", deviceType)
                putString("brand", brand)
                putInt("index", model.index)
                putString("model", payloadType)
                putString("nodeId", nodeId)
            })
        }

        b.btnNext.setOnClickListener {
            if (position < total - 1) {
                position++
                updateUi(toolbar, brand)
            }
        }

        b.btnPower.setOnClickListener {
            val model = currentModel()
            val payload = testStrategy.payload(brand, model)
            publish(testStrategy.topic(nodeId), payload)
        }
    }

    private fun updateUi(toolbar: MaterialToolbar, brand: String) {
        val model = currentModel()
        val step = position + 1
        val typeLabel = displayTypeLabel.ifBlank { "Thiết bị" }
        toolbar.title = "Thêm điều khiển từ xa"
        toolbar.subtitle = "$brand · $typeLabel"
        b.txtTitle.text = "Khớp $brand $typeLabel ($step / $total)"
        b.txtModel.text = "Mẫu $typeLabel: ${model.label}"
    }

    private fun currentModel(): CodeSetTestModel = models[position]

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}