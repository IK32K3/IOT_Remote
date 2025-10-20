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
        b.volUpArea.setOnClickListener { vm.volUp() }
        b.volDownArea.setOnClickListener { vm.volDown() }
        b.chUpArea.setOnClickListener { vm.chUp() }
        b.chDownArea.setOnClickListener { vm.chDown() }
    }

    private fun setupLinks() {
        updateLinkSelection(null)

        b.txt123.setOnClickListener {
            val showDigits = !b.gridDigits.isVisible
            showDigits(showDigits)
            updateLinkSelection(if (showDigits) LinkRow.DIGITS else null)
        }
        b.txtMenuLink.setOnClickListener {
            showDigits(false)
            updateLinkSelection(LinkRow.MENU)
            vm.menu()
        }
        b.txtMoreLink.setOnClickListener {
            showDigits(false)
            updateLinkSelection(LinkRow.MORE)
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
                updateLinkSelection(null)
            }
        }
    }

    private fun showDigits(show: Boolean) {
        b.gridDigits.isVisible = show
        b.groupMainControls.isVisible = !show
        b.rowLinks.isVisible = true
    }

    private fun updateLinkSelection(active: LinkRow?) {
        b.txt123.isSelected = active == LinkRow.DIGITS
        b.txtMenuLink.isSelected = active == LinkRow.MENU
        b.txtMoreLink.isSelected = active == LinkRow.MORE
    }

    private enum class LinkRow { DIGITS, MENU, MORE }
}
