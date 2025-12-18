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
import com.example.iot.databinding.FragmentControlStbBinding
import com.example.iot.databinding.FragmentControlTvBinding
import com.example.iot.feature.control.dvd.DvdPage
import com.example.iot.databinding.FragmentLearnRemoteBinding
import com.example.iot.domain.model.DeviceType
import com.example.iot.ui.add.model.LearnedCommandArg
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList
import com.example.iot.databinding.FragmentControlDvdBinding
import com.example.iot.databinding.FragmentControlProjectorBinding

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
            DeviceType.TV -> {
                inflateTvLayout()
                true
            }
            DeviceType.FAN -> {
                inflateFanLayout()
                true
            }
            DeviceType.STB -> {
                inflateStbLayout()
                true
            }
            DeviceType.DVD -> {
                inflateDvdLayout()
                true
            }
            DeviceType.PROJECTOR -> {
                inflateProjectorLayout()
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

    private fun inflateTvLayout() {
        val binding = FragmentControlTvBinding.inflate(layoutInflater, b.remoteContainer, true)

        // Show all controls to make learning easier.
        binding.gridDigits.isVisible = true
        binding.dpad.isVisible = true
        binding.fabHome.isVisible = true
        binding.fabBack.isVisible = true
        binding.rowLinks.isVisible = true

        val map = linkedMapOf<View, String>()
        map[binding.btnPower] = "POWER"
        map[binding.btnMute] = "MUTE"
        map[binding.btnSource] = "TV_AV"
        map[binding.volUpArea] = "VOL_UP"
        map[binding.volDownArea] = "VOL_DOWN"
        map[binding.chUpArea] = "CH_UP"
        map[binding.chDownArea] = "CH_DOWN"
        map[binding.btnMenu] = "MENU"
        map[binding.btnExit] = "EXIT"
        map[binding.fabHome] = "HOME"
        map[binding.fabBack] = "BACK"

        map[binding.btnUp] = "UP"
        map[binding.btnDown] = "DOWN"
        map[binding.btnLeft] = "LEFT"
        map[binding.btnRight] = "RIGHT"
        map[binding.btnOk] = "OK"

        map[binding.txtMoreLink] = "MORE"

        val g = binding.gridDigits
        map[g.getChildAt(0)] = "DIGIT_1"
        map[g.getChildAt(1)] = "DIGIT_2"
        map[g.getChildAt(2)] = "DIGIT_3"
        map[g.getChildAt(3)] = "DIGIT_4"
        map[g.getChildAt(4)] = "DIGIT_5"
        map[g.getChildAt(5)] = "DIGIT_6"
        map[g.getChildAt(6)] = "DIGIT_7"
        map[g.getChildAt(7)] = "DIGIT_8"
        map[g.getChildAt(8)] = "DIGIT_9"
        map[g.getChildAt(9)] = "DASH"
        map[g.getChildAt(10)] = "DIGIT_0"

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

    private fun inflateDvdLayout() {
        val binding = FragmentControlDvdBinding.inflate(layoutInflater, b.remoteContainer, true)

        fun showPage(page: DvdPage) {
            val isBasic = page == DvdPage.BASIC
            val isDigits = page == DvdPage.DIGITS
            val isMore = page == DvdPage.MORE

            binding.dpad.isVisible = isBasic
            binding.btnMenuFloat.isVisible = isBasic
            binding.btnExitFloat.isVisible = isBasic
            binding.btnMenuBottom.isVisible = isBasic
            binding.btnMoreBottom.isVisible = isBasic

            binding.gridDigits.isVisible = isDigits
            binding.gridMore.isVisible = isMore

            binding.rowLinks.isVisible = true
        }

        showPage(DvdPage.BASIC)
        binding.link123.setOnClickListener {
            showPage(if (binding.gridDigits.isVisible) DvdPage.BASIC else DvdPage.DIGITS)
        }
        binding.linkMore.setOnClickListener {
            showPage(if (binding.gridMore.isVisible) DvdPage.BASIC else DvdPage.MORE)
        }
        binding.linkMenu.setOnClickListener { showPage(DvdPage.BASIC) }

        val map = linkedMapOf<View, String>()
        map[binding.btnPower] = "POWER"
        map[binding.btnMute] = "MUTE"
        map[binding.btnEject] = "EJECT"
        map[binding.btnVolUp] = "VOL_UP"
        map[binding.btnVolDown] = "VOL_DOWN"
        map[binding.btnRew] = "REW"
        map[binding.btnFf] = "FF"
        map[binding.btnPlayPause] = "PLAY_PAUSE"
        map[binding.btnStop] = "STOP"
        map[binding.btnPrev] = "PREV"
        map[binding.btnNext] = "NEXT"
        map[binding.btnMenuFloat] = "MENU"
        map[binding.btnExitFloat] = "EXIT"
        map[binding.btnMenuBottom] = "MENU"
        map[binding.btnMoreBottom] = "HOME"

        map[binding.btnOk] = "OK"
        map[binding.btnUp] = "UP"
        map[binding.btnDown] = "DOWN"
        map[binding.btnLeft] = "LEFT"
        map[binding.btnRight] = "RIGHT"

        map[binding.btnTitle] = "TITLE"
        map[binding.btnSubtitle] = "SUBTITLE"
        map[binding.btnRed] = "RED"
        map[binding.btnGreen] = "GREEN"
        map[binding.btnBlue] = "BLUE"
        map[binding.btnYellow] = "YELLOW"

        val g = binding.gridDigits
        map[g.getChildAt(0)] = "DIGIT_1"
        map[g.getChildAt(1)] = "DIGIT_2"
        map[g.getChildAt(2)] = "DIGIT_3"
        map[g.getChildAt(3)] = "DIGIT_4"
        map[g.getChildAt(4)] = "DIGIT_5"
        map[g.getChildAt(5)] = "DIGIT_6"
        map[g.getChildAt(6)] = "DIGIT_7"
        map[g.getChildAt(7)] = "DIGIT_8"
        map[g.getChildAt(8)] = "DIGIT_9"
        map[g.getChildAt(9)] = "DASH"
        map[g.getChildAt(10)] = "DIGIT_0"
        map[g.getChildAt(11)] = "BACK"

        registerKeys(map)
    }

    private fun inflateProjectorLayout() {
        val binding =
            FragmentControlProjectorBinding.inflate(layoutInflater, b.remoteContainer, true)
        val map: Map<View, String> = mapOf(
            binding.btnPower to "POWER",
            binding.btnFreeze to "FREEZE",
            binding.btnSource to "SOURCE",
            binding.btnVolUp to "VOL_UP",
            binding.btnVolDown to "VOL_DOWN",
            binding.btnPageUp to "PAGE_UP",
            binding.btnPageDown to "PAGE_DOWN",
            binding.btnZoomIn to "ZOOM_IN",
            binding.btnZoomOut to "ZOOM_OUT",
            binding.btnMenuFloat to "MENU",
            binding.btnExitFloat to "EXIT",
            binding.btnInfoBottom to "INFO",
            binding.btnBackBottom to "BACK",
            binding.btnOk to "OK",
            binding.btnUp to "UP",
            binding.btnDown to "DOWN",
            binding.btnLeft to "LEFT",
            binding.btnRight to "RIGHT"
        )
        registerKeys(map)
    }

    private fun inflateStbLayout() {
        val binding = FragmentControlStbBinding.inflate(layoutInflater, b.remoteContainer, true)

        // Show all controls to make learning easier.
        binding.gridDigits.isVisible = true
        binding.dpad.isVisible = true
        binding.btnMenuFloat.isVisible = true
        binding.btnExitFloat.isVisible = true
        binding.btnMenuBottom.isVisible = true
        binding.btnMoreBottom.isVisible = true
        binding.rowLinks.isVisible = true

        val map = linkedMapOf<View, String>()
        map[binding.btnPower] = "POWER"
        map[binding.btnMute] = "MUTE"
        map[binding.btnTvAv] = "TV_AV"

        map[binding.btnVolUp] = "VOL_UP"
        map[binding.btnVolDown] = "VOL_DOWN"
        map[binding.btnPageUp] = "PAGE_UP"
        map[binding.btnPageDown] = "PAGE_DOWN"
        map[binding.btnChUp] = "CH_UP"
        map[binding.btnChDown] = "CH_DOWN"

        map[binding.btnMenuFloat] = "BACK"
        map[binding.btnExitFloat] = "EXIT"
        map[binding.btnMenuBottom] = "MENU"
        map[binding.btnMoreBottom] = "MORE"

        map[binding.btnUp] = "UP"
        map[binding.btnDown] = "DOWN"
        map[binding.btnLeft] = "LEFT"
        map[binding.btnRight] = "RIGHT"
        map[binding.btnOk] = "OK"

        val g = binding.gridDigits
        map[g.getChildAt(0)] = "DIGIT_1"
        map[g.getChildAt(1)] = "DIGIT_2"
        map[g.getChildAt(2)] = "DIGIT_3"
        map[g.getChildAt(3)] = "DIGIT_4"
        map[g.getChildAt(4)] = "DIGIT_5"
        map[g.getChildAt(5)] = "DIGIT_6"
        map[g.getChildAt(6)] = "DIGIT_7"
        map[g.getChildAt(7)] = "DIGIT_8"
        map[g.getChildAt(8)] = "DIGIT_9"
        map[g.getChildAt(9)] = "DASH"
        map[g.getChildAt(10)] = "DIGIT_0"
        map[g.getChildAt(11)] = "BACK"

        registerKeys(map)
    }

    override fun onDestroyView() {
        keyViews.clear()
        _b = null
        super.onDestroyView()
    }
}
