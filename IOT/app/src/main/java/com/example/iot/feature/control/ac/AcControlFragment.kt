package com.example.iot.feature.control.ac

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.iot.databinding.FragmentControlAcBinding
import com.example.iot.feature.control.common.BaseControlFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AcControlFragment : BaseControlFragment<FragmentControlAcBinding>() {

    override val vm: AcControlViewModel by viewModels()

    override fun inflateContent(inflater: LayoutInflater, container: ViewGroup): FragmentControlAcBinding =
        FragmentControlAcBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)

        // Bind UI info
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.power.collect { b.txtCenterStatus.text = if (it) "Điều hòa bật" else "Điều hòa tắt" } }
                launch { vm.temp.collect { b.txtInfoTemp.text = "$it°C" } }
                launch { vm.mode.collect { b.chipMode.text = "Mode: $it" } }
                launch { vm.fan.collect  { b.chipFan.text  = "Quạt: $it" } }
            }
        }

        // Buttons
        b.btnPower.setOnClickListener { vm.togglePower() }
        b.btnTempUp.setOnClickListener { vm.setTemp(vm.temp.value + 1) }
        b.btnTempDown.setOnClickListener { vm.setTemp(vm.temp.value - 1) }
        b.btnMode.setOnClickListener { vm.cycleMode() }
        // Các nút khác (fan/swing/…): map sau theo nhu cầu
    }

    override fun onConfirmDelete() { vm.deleteRemote() }
}
