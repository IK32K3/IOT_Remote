package com.example.iot.ui.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.iot.R
import com.example.iot.core.Defaults
import com.example.iot.databinding.FragmentControlAcBinding
import com.example.iot.databinding.FragmentControlFanBinding
import com.example.iot.databinding.FragmentLearnRemoteBinding
import com.example.iot.domain.model.DeviceType
import com.example.iot.ui.add.model.LearnedCommandArg
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList

@AndroidEntryPoint
class LearnRemoteFragment : Fragment() {
    private var _b: FragmentLearnRemoteBinding? = null
    private val b get() = _b!!

    private val vm: LearnRemoteViewModel by viewModels()

    private val keyViews: MutableMap<String, View> = mutableMapOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentLearnRemoteBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val typeLabel = arguments?.getString("type").orEmpty()
        val deviceType = DeviceType.fromLabel(typeLabel)
        val nodeId = arguments?.getString("nodeId").orEmpty().ifBlank { Defaults.NODE_ID }

        val toolbar: MaterialToolbar = b.topBar.root
        toolbar.apply {
            title = getString(R.string.learn_remote_title)
            subtitle = "$typeLabel · Học lệnh"
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        val supported = when (deviceType) {
            DeviceType.AC -> {
                inflateAcLayout()
                true
            }
            DeviceType.FAN -> {
                inflateFanLayout()
                true
            }
            else -> {
                b.txtStatus.text = getString(R.string.learn_remote_not_supported)
                b.btnSave.isEnabled = false
                false
            }
        }

        if (supported) {
            val keys = keyViews.keys
            vm.configure(deviceType, nodeId, keys, setOf("POWER"))
            observeState(deviceType)
            b.btnSave.setOnClickListener {
                val learned = vm.learnedCommands().map {
                    LearnedCommandArg(
                        deviceType = it.deviceType,
                        key = it.key,
                        protocol = it.protocol,
                        code = it.code,
                        bits = it.bits
                    )
                }
                findNavController().navigate(
                    R.id.action_learnRemote_to_saveRemote,
                    Bundle().apply {
                        putString("type", typeLabel)
                        putString("brand", "Tự học")
                        putInt("index", 0)
                        putString("model", "Học lệnh")
                        putString("nodeId", nodeId)
                        putParcelableArrayList("learnedCommands", ArrayList(learned))
                    }
                )
            }
        } else {
            b.btnSave.isVisible = false
        }
    }

    private fun observeState(deviceType: DeviceType) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.statusMessage.collect { msg ->
                        b.txtStatus.text = msg
                    }
                }
                launch {
                    vm.canSave.collect { enabled ->
                        b.btnSave.isEnabled = enabled
                        b.btnSave.alpha = if (enabled) 1f else 0.5f
                    }
                }
                launch {
                    vm.states.collect { map ->
                        applyStates(map)
                    }
                }
            }
        }

        val instructionText = when (deviceType) {
            DeviceType.AC -> getString(R.string.learn_remote_instruction_ac)
            DeviceType.FAN -> getString(R.string.learn_remote_instruction_fan)
            else -> getString(R.string.learn_remote_instruction_generic)
        }
        b.txtInstruction.text = instructionText
    }

    private fun applyStates(states: Map<String, LearnRemoteViewModel.LearnKeyState>) {
        states.forEach { (key, state) ->
            keyViews[key]?.let { view ->
                when (state.status) {
                    LearnRemoteViewModel.LearnStatus.IDLE -> {
                        view.isEnabled = true
                        view.alpha = 0.7f
                    }
                    LearnRemoteViewModel.LearnStatus.LEARNING -> {
                        view.isEnabled = false
                        view.alpha = 0.4f
                    }
                    LearnRemoteViewModel.LearnStatus.SUCCESS -> {
                        view.isEnabled = true
                        view.alpha = 1f
                    }
                    LearnRemoteViewModel.LearnStatus.ERROR -> {
                        view.isEnabled = true
                        view.alpha = 0.8f
                    }
                }
            }
        }
    }

    private fun inflateAcLayout() {
        val binding = FragmentControlAcBinding.inflate(layoutInflater, b.remoteContainer, true)
        val map: Map<View, String> = mapOf(
            binding.btnPower to "POWER",
            binding.btnTempUp to "TEMP_UP",
            binding.btnTempDown to "TEMP_DOWN",
            binding.btnMode to "MODE",
            binding.btnFan to "FAN",
            binding.btnSwing to "SWING",
            binding.btnCool to "COOL",
            binding.btnHeat to "HEAT",
            binding.btnTurbo to "TURBO"
        )
        registerKeys(map)
    }

    private fun inflateFanLayout() {
        val binding = FragmentControlFanBinding.inflate(layoutInflater, b.remoteContainer, true)
        val map: Map<View, String> = mapOf(
            binding.btnPower to "POWER",
            binding.btnTimer to "TIMER",
            binding.btnSpeedUp to "SPEED_UP",
            binding.btnSpeedDown to "SPEED_DOWN",
            binding.btnSwing to "SWING",
            binding.btnType to "TYPE"
        )
        registerKeys(map)
    }

    private fun registerKeys(map: Map<View, String>) {
        keyViews.clear()
        map.forEach { (view, key) ->
            val normalized = key.uppercase()
            keyViews[normalized] = view
            view.setOnClickListener { vm.startLearning(normalized) }
        }
    }

    override fun onDestroyView() {
        keyViews.clear()
        _b = null
        super.onDestroyView()
    }
}