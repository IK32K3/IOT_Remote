package com.example.iot.feature.control.fan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.iot.R
import com.example.iot.databinding.FragmentControlFanBinding
import com.example.iot.feature.control.common.BaseControlFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FanControlFragment : BaseControlFragment<FragmentControlFanBinding>() {
    private var rotateAnim: Animation? = null
    override val vm: FanControlViewModel by viewModels()

    override fun inflateContent(
        inflater: LayoutInflater, container: ViewGroup
    ) = FragmentControlFanBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)

        rotateAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.fan_rotate)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.power.collect { on ->
                    if (on) b.imgFanBlades.startAnimation(rotateAnim)
                    else b.imgFanBlades.clearAnimation()
                }
            }
        }

        b.btnPower.setOnClickListener { vm.togglePower() }
        b.btnTimer.setOnClickListener { vm.timer() }
        b.btnSpeedUp.setOnClickListener { vm.speedUp() }
        b.btnSpeedDown.setOnClickListener { vm.speedDown() }
        b.btnSwing.setOnClickListener { vm.toggleSwing() }
        b.btnType.setOnClickListener { vm.type() }
    }

    override fun onConfirmDelete() = vm.deleteRemote()
}
