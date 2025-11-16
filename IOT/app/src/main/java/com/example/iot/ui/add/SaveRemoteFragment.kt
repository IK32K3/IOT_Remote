package com.example.iot.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.iot.R
import com.example.iot.core.Defaults
import com.example.iot.data.local.RemoteProfile
import com.example.iot.databinding.FragmentSaveRemoteBinding
import com.example.iot.domain.model.DeviceType
import com.example.iot.domain.model.LearnedCommandDraft
import com.example.iot.domain.usecase.SaveLearnedCommandsUseCase
import com.example.iot.domain.usecase.SaveRemoteUseCase
import com.example.iot.ui.add.model.LearnedCommandArg
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SaveRemoteFragment : Fragment() {
    private var _b: FragmentSaveRemoteBinding? = null
    private val b get() = _b!!

    @Inject lateinit var save: SaveRemoteUseCase
    @Inject lateinit var saveLearned: SaveLearnedCommandsUseCase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentSaveRemoteBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val deviceTypeArg = arguments?.getString("type").orEmpty()
        val brand = arguments?.getString("brand").orEmpty()
        val index = arguments?.getInt("index") ?: 1
        val nodeId = arguments?.getString("nodeId").orEmpty().ifBlank { Defaults.NODE_ID }
        val model = arguments?.getString("model").orEmpty()
        val learned = arguments?.let {
            BundleCompat.getParcelableArrayList(
                it,
                "learnedCommands",
                LearnedCommandArg::class.java
            )
        } ?: arrayListOf()

        val defaultLabel = model.ifBlank { deviceTypeArg }
        b.inputName.setText("$brand $defaultLabel".trim())
        b.btnSave.setOnClickListener {
            val name = b.inputName.text?.toString().orEmpty()
            val room = b.inputRoom.text?.toString().orEmpty()
            val deviceType = DeviceType.fromLabel(deviceTypeArg)
            viewLifecycleOwner.lifecycleScope.launch {
                val id = save(
                    RemoteProfile(
                        name = name.ifBlank { "$brand $defaultLabel".trim() },
                        room = room.ifBlank { "Mặc định" },
                        brand = brand.ifBlank { "Tự học" },
                        type = model.ifBlank { deviceTypeArg },
                        nodeId = nodeId,
                        codeSetIndex = index,
                        deviceType = deviceType.name
                    )
                )
                if (learned.isNotEmpty()) {
                    val drafts = learned.map {
                        LearnedCommandDraft(
                            deviceType = it.deviceType,
                            key = it.key,
                            protocol = it.protocol,
                            code = it.code,
                            bits = it.bits
                        )
                    }
                    saveLearned(id, drafts)
                }
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}