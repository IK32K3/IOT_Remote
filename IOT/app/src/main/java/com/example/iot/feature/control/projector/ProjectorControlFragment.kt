package com.example.iot.feature.control.projector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.iot.databinding.FragmentControlProjectorBinding
import com.example.iot.feature.control.common.BaseControlFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProjectorControlFragment : BaseControlFragment<FragmentControlProjectorBinding>() {

    override val vm: ProjectorControlViewModel by viewModels()

    override fun inflateContent(
        inflater: LayoutInflater,
        container: ViewGroup
    ) = FragmentControlProjectorBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)
        vm.showBasic()

        // Top row
        b.btnPower.setOnClickListener { vm.togglePower() }
        b.btnFreeze.setOnClickListener { vm.freeze() }
        b.btnSource.setOnClickListener { vm.source() }

        // Pills
        b.btnVolUp.setOnClickListener { vm.volUp() }
        b.btnVolDown.setOnClickListener { vm.volDown() }
        b.btnPageUp.setOnClickListener { vm.pageUp() }
        b.btnPageDown.setOnClickListener { vm.pageDown() }
        b.btnZoomIn.setOnClickListener { vm.zoomIn() }
        b.btnZoomOut.setOnClickListener { vm.zoomOut() }

        // Links
        b.link123.setOnClickListener {
            if (b.gridDigits.isVisible) vm.showBasic() else vm.showDigits()
        }
        b.linkMenu.setOnClickListener { vm.showBasic(); vm.menu() }
        b.linkMore.setOnClickListener {
            if (b.gridMore.isVisible) vm.showBasic() else vm.showMore()
        }

        // D-pad + surrounding
        b.btnMenuFloat.setOnClickListener { vm.menu() }
        b.btnExitFloat.setOnClickListener { vm.exit() }
        b.btnInfoBottom.setOnClickListener { vm.info() }
        b.btnBackBottom.setOnClickListener { vm.back() }
        b.btnOk.setOnClickListener { vm.ok() }
        b.btnUp.setOnClickListener { vm.up() }
        b.btnDown.setOnClickListener { vm.down() }
        b.btnLeft.setOnClickListener { vm.left() }
        b.btnRight.setOnClickListener { vm.right() }

        // Digits grid
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
                vm.showBasic()
            }
        }

        // More grid
        b.btnMoreMute.setOnClickListener { vm.mute() }
        b.btnMoreVideo.setOnClickListener { vm.video() }
        b.btnMorePagePlus.setOnClickListener { vm.pageUp() }
        b.btnMorePageMinus.setOnClickListener { vm.pageDown() }
        b.btnMoreTrapPlus.setOnClickListener { vm.trapUp() }
        b.btnMoreTrapMinus.setOnClickListener { vm.trapDown() }
        b.btnMoreUsb.setOnClickListener { vm.usb() }

        // Page observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.page.collect { p ->
                    val isBasic = p == ProjectorPage.BASIC
                    val isDigits = p == ProjectorPage.DIGITS
                    val isMore = p == ProjectorPage.MORE

                    b.rowTop.isVisible = true
                    b.pillVol.isVisible = true
                    b.pillPage.isVisible = true
                    b.pillZoom.isVisible = true
                    b.rowLinks.isVisible = true

                    b.groupBasic.isVisible = isBasic
                    b.groupDigits.isVisible = isDigits
                    b.groupMore.isVisible = isMore
                }
            }
        }
    }

    override fun onConfirmDelete(remoteId: String) = vm.deleteRemote(remoteId)
}
