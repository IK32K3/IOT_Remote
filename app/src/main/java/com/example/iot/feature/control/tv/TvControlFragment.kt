package com.example.iot.feature.control.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.iot.databinding.FragmentControlTvBinding
import com.example.iot.feature.control.common.BaseControlFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TvControlFragment :
    BaseControlFragment<FragmentControlTvBinding>() {

    override val vm: TvControlViewModel by viewModels()

    override fun inflateContent(
        inflater: LayoutInflater,
        container: ViewGroup
    ): FragmentControlTvBinding =
        FragmentControlTvBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)

        setupTopRow()
        setupPills()
        setupLinks()
        setupFloatingControls()
        setupDpad()
        setupDigitsGrid()
    }

    /** BaseControlFragment sẽ gọi khi bấm thùng rác trên toolbar */
    override fun onConfirmDelete() = vm.deleteRemote()

    private fun setupTopRow() {
        b.btnSource.setOnClickListener { vm.tvAv() }
        b.btnMute.setOnClickListener { vm.mute() }
        b.btnPower.setOnClickListener { vm.power() }
        b.btnMenu.setOnClickListener { vm.menu() }
        b.btnExit.setOnClickListener { vm.exit() }
        // ensure tapping inner content triggers the same action
        b.icPower.setOnClickListener { vm.power() }
        b.icMute.setOnClickListener { vm.mute() }
        b.tvSource.setOnClickListener { vm.tvAv() }
    }

    private fun setupPills() {
        val volContainer = b.pillVol.getChildAt(0) as? ViewGroup
        volContainer?.let {
            if (it.childCount >= 3) {
                it.getChildAt(0).setOnClickListener { vm.volUp() }
                it.getChildAt(2).setOnClickListener { vm.volDown() }
            }
        }

        val chContainer = b.pillCh.getChildAt(0) as? ViewGroup
        chContainer?.let {
            if (it.childCount >= 3) {
                it.getChildAt(0).setOnClickListener { vm.chUp() }
                it.getChildAt(2).setOnClickListener { vm.chDown() }
            }
        }
    }

    private fun setupLinks() {
        b.txt123.setOnClickListener { showDigits(!b.gridDigits.isVisible) }
        b.txtMenuLink.setOnClickListener {
            showDigits(false)
            vm.menu()
        }
        b.txtMoreLink.setOnClickListener {
            showDigits(false)
            vm.more()
        }
    }

    private fun setupFloatingControls() {
        b.fabHome.setOnClickListener { vm.home() }
        b.fabBack.setOnClickListener { vm.back() }
    }

    private fun setupDpad() {
        b.dpad.btnUp.setOnClickListener { vm.navUp() }
        b.dpad.btnDown.setOnClickListener { vm.navDown() }
        b.dpad.btnLeft.setOnClickListener { vm.navLeft() }
        b.dpad.btnRight.setOnClickListener { vm.navRight() }
        b.dpad.btnOk.setOnClickListener { vm.ok() }
    }

    private fun setupDigitsGrid() {
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
            getChildAt(11).setOnClickListener {
                vm.back()
                showDigits(false)
            }
        }
    }

    private fun showDigits(show: Boolean) {
        b.gridDigits.isVisible = show
        b.rowLinks.isVisible = !show
    }
}
