package com.example.iot.feature.control.fan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.iot.R
import com.example.iot.databinding.FragmentControlFanBinding
import com.example.iot.feature.control.common.BaseControlFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FanControlFragment : BaseControlFragment<FragmentControlFanBinding>() {
    override val vm: FanControlViewModel by viewModels()

    override fun inflateContent(
        inflater: LayoutInflater, container: ViewGroup
    ) = FragmentControlFanBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)

        b.btnPower.setOnClickListener { vm.togglePower() }
        b.btnTimer.setOnClickListener { vm.timer() }
        b.btnSpeedUp.setOnClickListener { vm.speedUp() }
        b.btnSpeedDown.setOnClickListener { vm.speedDown() }
        b.btnSwing.setOnClickListener { vm.toggleSwing() }
        b.btnType.setOnClickListener { vm.type() }
    }

    override fun onConfirmDelete(remoteId: String) = vm.deleteRemote(remoteId)
}
