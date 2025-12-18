package com.example.iot.feature.control.stb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.iot.databinding.FragmentControlStbBinding
import com.example.iot.feature.control.common.BaseControlFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StbControlFragment : BaseControlFragment<FragmentControlStbBinding>() {

    override val vm: StbControlViewModel by viewModels()

    override fun inflateContent(
        inflater: LayoutInflater,
        container: ViewGroup
    ) = FragmentControlStbBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)
        vm.showBasic()

        // === BASIC BUTTONS ===
        b.btnPower.setOnClickListener { vm.power() }
        b.btnMute.setOnClickListener { vm.mute() }
        b.btnTvAv.setOnClickListener { vm.tvAv() }
        b.btnVolUp.setOnClickListener { vm.volUp() }
        b.btnVolDown.setOnClickListener { vm.volDown() }
        b.btnPageUp.setOnClickListener { vm.pageUp() }
        b.btnPageDown.setOnClickListener { vm.pageDown() }
        b.btnChUp.setOnClickListener { vm.chUp() }
        b.btnChDown.setOnClickListener { vm.chDown() }

        // === PAGE LINKS ===
        b.link123.setOnClickListener {
            if (b.gridDigits.isVisible) vm.showBasic() else vm.showDigits()
        }
        b.linkMenu.setOnClickListener { vm.showBasic(); vm.menu() }

        // === FLOATING ===
        b.btnMenuFloat.setOnClickListener { vm.back() }
        b.btnExitFloat.setOnClickListener { vm.exit() }
        b.btnMenuBottom.setOnClickListener { vm.menu() }
        b.btnMoreBottom.setOnClickListener { vm.more() }

        // === D-PAD ===
        b.btnOk.setOnClickListener { vm.ok() }
        b.btnUp.setOnClickListener { vm.up() }
        b.btnDown.setOnClickListener { vm.down() }
        b.btnLeft.setOnClickListener { vm.left() }
        b.btnRight.setOnClickListener { vm.right() }

        // === DIGITS GRID ===
        b.gridDigits.apply {
            getChildAt(0).setOnClickListener { vm.digit(1) }
            getChildAt(1).setOnClickListener { vm.digit(2) }
            getChildAt(2).setOnClickListener { vm.digit(3) }
            getChildAt(3).setOnClickListener { vm.digit(4) }
            getChildAt(4).setOnClickListener { vm.digit(5) }
            getChildAt(5).setOnClickListener { vm.digit(6) }
            getChildAt(6).setOnClickListener { vm.digit(7) }
            getChildAt(7).setOnClickListener { vm.digit(8) }
            getChildAt(8).setOnClickListener { vm.digit(9) }
            getChildAt(9).setOnClickListener { vm.dash() }
            getChildAt(10).setOnClickListener { vm.digit(0) }
            getChildAt(11).setOnClickListener { vm.back(); vm.showBasic() }
        }

        // === PAGE OBSERVER ===
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.page.collect { p ->
                    val isBasic = p == StbPage.BASIC
                    val isDigits = p == StbPage.DIGITS

                    // Always keep top row + pills visible; only swap lower half like TV
                    b.rowTop.isVisible = true
                    b.pillVol.isVisible = true
                    b.pillPage.isVisible = true
                    b.pillCh.isVisible = true

                    b.dpad.isVisible = isBasic
                    b.btnMenuFloat.isVisible = isBasic
                    b.btnExitFloat.isVisible = isBasic
                    b.btnMenuBottom.isVisible = isBasic
                    b.btnMoreBottom.isVisible = isBasic

                    b.gridDigits.isVisible = isDigits

                    b.rowLinks.isVisible = true
                }
            }
        }
    }

    override fun onConfirmDelete(remoteId: String) = vm.deleteRemote(remoteId)
}
